package com.redcore.gameengine.util

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.redcore.gameengine.data.model.VoiceEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.coroutines.coroutineContext

/**
 * Microphone recording, DSP effects and playback. Everything here runs on the
 * app's own audio pipeline — Android gives no API to insert a voice changer into
 * other apps' calls, so REDCORE never pretends to do that.
 */
object AudioEngine {

    const val SAMPLE_RATE = 44_100

    /** Records mono PCM for [seconds] and returns the samples, or null if mic access fails. */
    suspend fun record(context: Context, seconds: Int = 6): ShortArray? = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return@withContext null
        val bufferSize = max(minBuffer * 2, SAMPLE_RATE)
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (t: Throwable) {
            return@withContext null
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return@withContext null
        }
        val total = SAMPLE_RATE * seconds
        val samples = ShortArray(total)
        var read = 0
        try {
            recorder.startRecording()
            val chunk = ShortArray(2048)
            while (read < total && coroutineContext.isActive) {
                val n = recorder.read(chunk, 0, min(chunk.size, total - read))
                if (n <= 0) break
                System.arraycopy(chunk, 0, samples, read, n)
                read += n
            }
        } catch (t: Throwable) {
            return@withContext null
        } finally {
            try {
                recorder.stop()
            } catch (_: Throwable) {
            }
            recorder.release()
        }
        if (read <= 0) null else samples.copyOf(read)
    }

    fun applyEffect(input: ShortArray, effect: VoiceEffect): ShortArray = when (effect) {
        VoiceEffect.NONE -> input
        VoiceEffect.DEEP -> pitchShift(input, 0.72f)
        VoiceEffect.CHIPMUNK -> pitchShift(input, 1.55f)
        VoiceEffect.ROBOT -> robot(input)
        VoiceEffect.ECHO -> echo(input)
        VoiceEffect.RADIO -> radio(input)
        VoiceEffect.ALIEN -> alien(input)
    }

    /** Linear-interpolation resample: changes pitch and speed (classic walkie effect). */
    private fun pitchShift(input: ShortArray, factor: Float): ShortArray {
        val outLength = (input.size / factor).toInt().coerceAtLeast(1)
        val out = ShortArray(outLength)
        for (i in 0 until outLength) {
            val src = i * factor
            val i0 = src.toInt().coerceIn(0, input.size - 1)
            val i1 = (i0 + 1).coerceAtMost(input.size - 1)
            val frac = src - i0
            val value = input[i0] * (1 - frac) + input[i1] * frac
            out[i] = value.roundToInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    private fun robot(input: ShortArray): ShortArray {
        val out = ShortArray(input.size)
        val carrier = 38.0
        for (i in input.indices) {
            val mod = sin(2 * PI * carrier * i / SAMPLE_RATE)
            out[i] = (input[i] * mod).roundToInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    private fun echo(input: ShortArray): ShortArray {
        val delay = (SAMPLE_RATE * 0.22f).toInt()
        val out = ShortArray(input.size + delay)
        System.arraycopy(input, 0, out, 0, input.size)
        for (i in input.indices) {
            val target = i + delay
            if (target < out.size) {
                val mixed = out[target] + (input[i] * 0.42f).roundToInt()
                out[target] = mixed.coerceIn(-32768, 32767).toShort()
            }
        }
        return out
    }

    /** Cheap band-limiting: one-pole low pass then high pass, plus soft clipping. */
    private fun radio(input: ShortArray): ShortArray {
        val out = ShortArray(input.size)
        var lowState = 0f
        var highPrev = 0f
        val lowCoef = 0.22f
        val highCoef = 0.92f
        for (i in input.indices) {
            val x = input[i] / 32768f
            lowState += lowCoef * (x - lowState)
            val high = highCoef * (highPrev + lowState - (if (i > 0) input[i - 1] / 32768f else 0f))
            highPrev = high
            val boosted = (high * 3.2f).coerceIn(-1f, 1f)
            out[i] = (boosted * 32767).roundToInt().toShort()
        }
        return out
    }

    private fun alien(input: ShortArray): ShortArray {
        val out = ShortArray(input.size)
        var phase = 0.0
        var readPos = 0.0
        for (i in input.indices) {
            val lfo = 0.85 + 0.35 * sin(2 * PI * 3.2 * i / SAMPLE_RATE)
            phase += lfo
            readPos += lfo
            val idx = readPos.toInt().coerceIn(0, input.size - 1)
            val tremolo = 0.6 + 0.4 * sin(2 * PI * 6.5 * i / SAMPLE_RATE)
            out[i] = (input[idx] * tremolo).roundToInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /** Plays samples on the media stream and blocks until playback finishes. */
    suspend fun play(samples: ShortArray, gain: Float = 1.1f) = withContext(Dispatchers.Default) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(4096, samples.size * 2))
            .build()
        try {
            track.play()
            var offset = 0
            val chunk = 4096
            while (offset < samples.size) {
                val count = min(chunk, samples.size - offset)
                val buffer = ShortArray(count)
                for (i in 0 until count) {
                    val v = samples[offset + i] * gain
                    buffer[i] = v.roundToInt().coerceIn(-32768, 32767).toShort()
                }
                track.write(buffer, 0, count)
                offset += count
            }
            Thread.sleep(250)
        } catch (_: Throwable) {
        } finally {
            try {
                track.stop()
            } catch (_: Throwable) {
            }
            track.release()
        }
    }

    fun saveWav(context: Context, samples: ShortArray): String? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "REDCORE_voice_${System.currentTimeMillis()}.wav")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MUSIC + "/REDCORE"
                )
            }
            val uri = context.contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                writeWav(out, samples)
            }
            "Saved to Music/REDCORE"
        } catch (t: Throwable) {
            null
        }
    }

    private fun writeWav(out: java.io.OutputStream, samples: ShortArray) {
        val dataSize = samples.size * 2
        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(SAMPLE_RATE)
        header.putInt(SAMPLE_RATE * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(dataSize)
        out.write(header.array())
        val pcm = java.nio.ByteBuffer.allocate(dataSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        for (s in samples) pcm.putShort(s)
        out.write(pcm.array())
        out.flush()
    }

    /** Live mic level meter, 0..1. Cancelling the coroutine stops the recorder. */
    suspend fun meter(onLevel: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return@withContext
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )
        } catch (t: Throwable) {
            return@withContext
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return@withContext
        }
        val buffer = ShortArray(1024)
        try {
            recorder.startRecording()
            while (coroutineContext.isActive) {
                val n = recorder.read(buffer, 0, buffer.size)
                if (n > 0) {
                    var sum = 0.0
                    var peak = 0
                    for (i in 0 until n) {
                        val v = abs(buffer[i].toInt())
                        if (v > peak) peak = v
                        sum += (buffer[i].toDouble() / 32768.0).pow(2.0)
                    }
                    val rms = kotlin.math.sqrt(sum / n).toFloat()
                    val level = max(rms * 4.2f, peak / 32768f * 0.55f).coerceIn(0f, 1f)
                    onLevel(level)
                }
            }
        } catch (_: Throwable) {
        } finally {
            try {
                recorder.stop()
            } catch (_: Throwable) {
            }
            recorder.release()
        }
    }

    fun noiseFloorLabel(level: Float): String = when {
        level < 0.05f -> "Silent"
        level < 0.2f -> "Quiet"
        level < 0.45f -> "Normal"
        level < 0.75f -> "Loud"
        else -> "Clipping risk"
    }
}
