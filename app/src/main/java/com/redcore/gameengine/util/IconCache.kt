package com.redcore.gameengine.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small in-memory cache for launcher icons and user-picked images.
 *
 * Icons used to be decoded with PackageManager / BitmapFactory directly inside
 * composition, so every scroll frame paid for disk + drawable inflation on the
 * main thread. They are now decoded once, off Main, and reused while scrolling.
 */
object IconCache {

    private const val MAX_ENTRIES = 64

    private val cache = object : LruCache<String, ImageBitmap>(MAX_ENTRIES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = 1
    }

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun keyOf(packageName: String?, customUri: String?): String? = when {
        !customUri.isNullOrBlank() -> "uri:$customUri"
        !packageName.isNullOrBlank() -> "pkg:$packageName"
        else -> null
    }

    suspend fun load(context: Context, packageName: String?, customUri: String?): ImageBitmap? {
        val key = keyOf(packageName, customUri) ?: return null
        cache.get(key)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) { decode(context, packageName, customUri) } ?: return null
        val image = bitmap.asImageBitmap()
        cache.put(key, image)
        return image
    }

    /** Drops cached entries (used after the library is wiped or an icon is replaced). */
    fun clear() = cache.evictAll()

    private fun decode(context: Context, packageName: String?, customUri: String?): Bitmap? = try {
        when {
            !customUri.isNullOrBlank() -> context.contentResolver
                .openInputStream(Uri.parse(customUri))
                ?.use { BitmapFactory.decodeStream(it) }

            !packageName.isNullOrBlank() ->
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap(width = 144, height = 144)

            else -> null
        }
    } catch (_: Throwable) {
        null
    }
}
