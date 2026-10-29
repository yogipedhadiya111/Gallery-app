package com.aistudio.xplayer.vdpkzm.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

@Suppress("DEPRECATION")
class VideoRepository(
    context: Context,
    private val videoDao: VideoDao
) {
    private val context: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.createAttributionContext("google")
    } else {
        context
    }
    val recentVideos: Flow<List<RecentVideo>> = videoDao.getRecentVideosFlow()
    val favoriteVideos: Flow<List<FavoriteVideo>> = videoDao.getFavoriteVideosFlow()
    val playlists: Flow<List<Playlist>> = videoDao.getPlaylistsFlow()
    val vaultVideos: Flow<List<VaultVideo>> = videoDao.getVaultVideosFlow()

    fun isFavoriteFlow(path: String): Flow<Boolean> = videoDao.isFavoriteFlow(path)

    suspend fun toggleFavorite(video: Video) = withContext(Dispatchers.IO) {
        if (videoDao.isFavorite(video.path)) {
            videoDao.deleteFavoriteVideo(video.path)
        } else {
            videoDao.insertFavoriteVideo(
                FavoriteVideo(
                    path = video.path,
                    title = video.title,
                    duration = video.duration
                )
            )
        }
    }

    suspend fun addToRecent(video: Video, position: Long) = withContext(Dispatchers.IO) {
        videoDao.insertRecentVideo(
            RecentVideo(
                path = video.path,
                title = video.title,
                duration = video.duration,
                lastPlayedPosition = position,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        videoDao.clearHistory()
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        videoDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        videoDao.deletePlaylistWithVideos(playlistId)
    }

    fun getVideosInPlaylist(playlistId: Long): Flow<List<PlaylistVideo>> {
        return videoDao.getVideosInPlaylistFlow(playlistId)
    }

    suspend fun addVideoToPlaylist(playlistId: Long, video: Video) = withContext(Dispatchers.IO) {
        videoDao.insertPlaylistVideo(
            PlaylistVideo(
                playlistId = playlistId,
                videoPath = video.path,
                title = video.title,
                duration = video.duration
            )
        )
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoPath: String) = withContext(Dispatchers.IO) {
        videoDao.deleteVideoFromPlaylist(playlistId, videoPath)
    }

    // --- SECURE VAULT OPERATIONS ---
    suspend fun importVideoToVault(video: Video): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "vault_videos")
            if (!vaultDir.exists()) {
                vaultDir.mkdirs()
            }

            val uniqueName = "vault_${UUID.randomUUID()}.dat"
            val targetFile = File(vaultDir, uniqueName)

            // Copy file to internal sandbox storage
            if (video.path.startsWith("http")) {
                // For demo/sample stream videos, just mock importing them
                val dummyOut = FileOutputStream(targetFile)
                dummyOut.write("vault_dummy_stream_data:${video.path}".toByteArray())
                dummyOut.close()
            } else {
                val uri = Uri.parse(video.uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext false
                val outputStream = FileOutputStream(targetFile)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
            }

            videoDao.insertVaultVideo(
                VaultVideo(
                    originalPath = video.path,
                    secureFileName = uniqueName,
                    title = video.title,
                    duration = video.duration,
                    size = video.size,
                    importTimestamp = System.currentTimeMillis()
                )
            )

            // Try to delete original if it's a physical file
            if (!video.path.startsWith("http")) {
                try {
                    val originalFile = File(video.path)
                    if (originalFile.exists()) {
                        originalFile.delete()
                    }
                    context.contentResolver.delete(
                        Uri.parse(video.uriString),
                        null,
                        null
                    )
                } catch (e: Exception) {
                    Log.e("VideoRepository", "Failed to delete original file: ${e.message}")
                }
            }
            true
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error importing video to vault", e)
            false
        }
    }

    suspend fun exportVideoFromVault(vaultVideo: VaultVideo): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultDir = File(context.filesDir, "vault_videos")
            val sourceFile = File(vaultDir, vaultVideo.secureFileName)
            if (!sourceFile.exists()) return@withContext false

            // If it was originally a streamed dummy video
            if (vaultVideo.originalPath.startsWith("http")) {
                sourceFile.delete()
                videoDao.deleteVaultVideo(vaultVideo)
                return@withContext true
            }

            // Expose export back to system media store (depending on scoped storage)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, vaultVideo.title)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/XPlayerPro")
                }
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            } else {
                val targetFile = File(vaultVideo.originalPath)
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { output ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(output)
                    }
                }
            }

            sourceFile.delete()
            videoDao.deleteVaultVideo(vaultVideo)
            true
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error exporting movie from vault", e)
            false
        }
    }

    suspend fun deleteVideoFromVault(vaultVideo: VaultVideo) = withContext(Dispatchers.IO) {
        val vaultDir = File(context.filesDir, "vault_videos")
        val sourceFile = File(vaultDir, vaultVideo.secureFileName)
        if (sourceFile.exists()) {
            sourceFile.delete()
        }
        videoDao.deleteVaultVideo(vaultVideo)
    }

    // --- FILE MANAGER OPERATIONS ---
    suspend fun deleteLocalFile(video: Video): Boolean = withContext(Dispatchers.IO) {
        if (video.path.startsWith("http")) return@withContext true // Demomode is virtual
        try {
            val file = File(video.path)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    Log.d("VideoRepository", "Standard File.delete() failed, trying ContentResolver")
                }
            }
            val numRows = context.contentResolver.delete(
                Uri.parse(video.uriString),
                null,
                null
            )
            numRows > 0
        } catch (e: Exception) {
            Log.e("VideoRepository", "Failed to delete file", e)
            false
        }
    }

    suspend fun renameLocalFile(video: Video, newNameWithoutExtension: String): Boolean = withContext(Dispatchers.IO) {
        if (video.path.startsWith("http")) return@withContext false
        try {
            val file = File(video.path)
            val ext = file.extension
            val newFileName = if (ext.isNotEmpty()) "$newNameWithoutExtension.$ext" else newNameWithoutExtension
            val targetFile = File(file.parentFile, newFileName)

            if (file.exists()) {
                val renamed = file.renameTo(targetFile)
                if (renamed) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, newFileName)
                        put(MediaStore.Video.Media.TITLE, newNameWithoutExtension)
                        put(MediaStore.Video.Media.DATA, targetFile.absolutePath)
                    }
                    context.contentResolver.update(
                        Uri.parse(video.uriString),
                        values,
                        null,
                        null
                    )
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error renaming file", e)
            false
        }
    }

    // --- MASTER SCANNER ---
    suspend fun scanLocalVideos(): List<Video> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Video>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.RESOLUTION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            } else {
                MediaStore.Video.Media.DATA
            }
        )

        val uris = listOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.INTERNAL_CONTENT_URI
        )

        for (contentUri in uris) {
            try {
                val cursor = context.contentResolver.query(
                    contentUri,
                    projection,
                    null,
                    null,
                    "${MediaStore.Video.Media.DATE_ADDED} DESC"
                )

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val titleCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                    val durationCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dateAddedCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val dateModifiedCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                    val mimeTypeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                    val resolutionCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION)
                    val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    } else {
                        dataCol
                    }

                    val supportedExtensions = setOf(
                        "mp4", "mkv", "avi", "mov", "flv", "webm", "3gp", "m4v", "ts", "mpeg", "mpg", "wmv", "vob"
                    )

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val path = c.getString(dataCol) ?: ""
                        if (path.isEmpty()) continue

                        // Filter for supported formats
                        val file = File(path)
                        val ext = file.extension.lowercase(java.util.Locale.ROOT)
                        if (!supportedExtensions.contains(ext)) {
                            continue
                        }

                        val title = c.getString(titleCol) ?: "Unknown"
                        val duration = c.getLong(durationCol)
                        val size = c.getLong(sizeCol)
                        val dateAdded = c.getLong(dateAddedCol)
                        val dateModified = c.getLong(dateModifiedCol)
                        val mimeType = c.getString(mimeTypeCol) ?: "video/mp4"
                        val resolution = c.getString(resolutionCol) ?: "Unknown"

                        val bucketName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            c.getString(bucketCol) ?: "Camera"
                        } else {
                            val folderPath = File(c.getString(bucketCol) ?: "").parentFile?.name ?: "Video"
                            folderPath
                        }

                        val uri = Uri.withAppendedPath(contentUri, id.toString())
                        list.add(
                            Video(
                                uriString = uri.toString(),
                                path = path,
                                title = title,
                                duration = duration,
                                size = size,
                                dateAdded = dateAdded,
                                folderName = bucketName,
                                id = id,
                                resolution = resolution,
                                dateModified = dateModified,
                                mimeType = mimeType
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoRepository", "Error scanning media store uri $contentUri", e)
            }
        }

        list.distinctBy { it.path }
    }
}
