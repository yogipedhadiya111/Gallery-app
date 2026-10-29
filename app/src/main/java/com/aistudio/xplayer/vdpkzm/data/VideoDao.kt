package com.aistudio.xplayer.vdpkzm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    // --- RECENT VIDEOS (HISTORY) ---
    @Query("SELECT * FROM recent_videos ORDER BY lastPlayedTimestamp DESC")
    fun getRecentVideosFlow(): Flow<List<RecentVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentVideo(video: RecentVideo)

    @Query("DELETE FROM recent_videos WHERE path = :path")
    suspend fun deleteRecentVideo(path: String)

    @Query("DELETE FROM recent_videos")
    suspend fun clearHistory()

    // --- FAVORITES ---
    @Query("SELECT * FROM favorite_videos ORDER BY addedTimestamp DESC")
    fun getFavoriteVideosFlow(): Flow<List<FavoriteVideo>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_videos WHERE path = :path)")
    fun isFavoriteFlow(path: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_videos WHERE path = :path)")
    suspend fun isFavorite(path: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteVideo(video: FavoriteVideo)

    @Query("DELETE FROM favorite_videos WHERE path = :path")
    suspend fun deleteFavoriteVideo(path: String)

    // --- PLAYLISTS ---
    @Query("SELECT * FROM playlists ORDER BY createdTimestamp DESC")
    fun getPlaylistsFlow(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId")
    suspend fun deletePlaylistVideosForPlaylist(playlistId: Long)

    @Transaction
    suspend fun deletePlaylistWithVideos(playlistId: Long) {
        deletePlaylistVideosForPlaylist(playlistId)
        deletePlaylist(playlistId)
    }

    // --- PLAYLIST VIDEOS ---
    @Query("SELECT * FROM playlist_videos WHERE playlistId = :playlistId ORDER BY addedTimestamp DESC")
    fun getVideosInPlaylistFlow(playlistId: Long): Flow<List<PlaylistVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideo(video: PlaylistVideo)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoPath = :videoPath")
    suspend fun deleteVideoFromPlaylist(playlistId: Long, videoPath: String)

    // --- SECURE VAULT ---
    @Query("SELECT * FROM vault_videos ORDER BY importTimestamp DESC")
    fun getVaultVideosFlow(): Flow<List<VaultVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultVideo(video: VaultVideo)

    @Delete
    suspend fun deleteVaultVideo(video: VaultVideo)
}
