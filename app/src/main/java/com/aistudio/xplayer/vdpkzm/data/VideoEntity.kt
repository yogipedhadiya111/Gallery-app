package com.aistudio.xplayer.vdpkzm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_videos")
data class RecentVideo(
    @PrimaryKey val path: String,
    val title: String,
    val duration: Long,
    val lastPlayedPosition: Long,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_videos")
data class FavoriteVideo(
    @PrimaryKey val path: String,
    val title: String,
    val duration: Long,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_videos", primaryKeys = ["playlistId", "videoPath"])
data class PlaylistVideo(
    val playlistId: Long,
    val videoPath: String,
    val title: String,
    val duration: Long,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_videos")
data class VaultVideo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val secureFileName: String, // Copied local code files in filesDir/vault/
    val title: String,
    val duration: Long,
    val size: Long,
    val importTimestamp: Long = System.currentTimeMillis()
)
