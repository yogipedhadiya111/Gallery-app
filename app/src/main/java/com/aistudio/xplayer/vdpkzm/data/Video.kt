package com.aistudio.xplayer.vdpkzm.data

import java.io.File
import java.util.Locale

data class Video(
    val uriString: String,
    val path: String,
    val title: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val folderName: String,
    val id: Long = 0L,
    val resolution: String = "Unknown",
    val dateModified: Long = 0L,
    val mimeType: String = "video/mp4"
) {
    val durationFormatted: String
        get() {
            if (duration <= 0) return "00:00"
            val totalSeconds = duration / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }

    val sizeFormatted: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) return "$size B"
            return String.format(
                Locale.getDefault(),
                "%.1f %s",
                size / Math.pow(1024.0, digitGroups.toDouble()),
                units[digitGroups]
            )
        }

    val dateFormatted: String
        get() {
            val date = java.util.Date(dateAdded * 1000)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(date)
        }
}
