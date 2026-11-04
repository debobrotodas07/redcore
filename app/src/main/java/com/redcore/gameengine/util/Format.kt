package com.redcore.gameengine.util

import kotlin.math.abs

object Format {

    fun duration(seconds: Long): String {
        if (seconds <= 0) return "0m"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    fun clock(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun bytesPerSecond(bytes: Long): String {
        val b = bytes.coerceAtLeast(0)
        return when {
            b < 1024 -> "$b B/s"
            b < 1024 * 1024 -> "%.1f KB/s".format(b / 1024.0)
            else -> "%.1f MB/s".format(b / (1024.0 * 1024.0))
        }
    }

    fun bytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

    fun ago(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "Never played"
        val diff = abs(System.currentTimeMillis() - timestamp) / 1000
        return when {
            diff < 60 -> "Just now"
            diff < 3600 -> "${diff / 60}m ago"
            diff < 86_400 -> "${diff / 3600}h ago"
            diff < 86_400 * 7 -> "${diff / 86_400}d ago"
            else -> {
                val days = diff / 86_400
                "${days / 7}w ago"
            }
        }
    }

    fun percent(value: Float?): String = value?.let { "${it.toInt()}%" } ?: "—"

    fun temp(value: Float?): String = value?.let { "%.1f°C".format(it) } ?: "—"
}
