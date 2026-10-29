package com.aistudio.xplayer.vdpkzm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.xplayer.vdpkzm.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val application: Application,
    private val repository: VideoRepository
) : AndroidViewModel(application) {

    // --- PREFERENCES / THEME ---
    private val sharedPrefs = application.getSharedPreferences("xplayer_settings", Application.MODE_PRIVATE)
    
    private val _themeState = MutableStateFlow(sharedPrefs.getInt("theme_mode", 0)) // Default: 0=Light, 1=Dark, 2=AMOLED
    val themeState: StateFlow<Int> = _themeState.asStateFlow()

    fun updateTheme(mode: Int) {
        _themeState.value = mode
        sharedPrefs.edit().putInt("theme_mode", mode).apply()
    }

    // --- SECURITY SETTINGS ---
    private val _protectScreenshots = MutableStateFlow(sharedPrefs.getBoolean("protect_screenshots", true))
    val protectScreenshots: StateFlow<Boolean> = _protectScreenshots.asStateFlow()

    private val _preventScreenRecording = MutableStateFlow(sharedPrefs.getBoolean("prevent_screen_recording", true))
    val preventScreenRecording: StateFlow<Boolean> = _preventScreenRecording.asStateFlow()

    private val _hideRecentApps = MutableStateFlow(sharedPrefs.getBoolean("hide_recent_apps", true))
    val hideRecentApps: StateFlow<Boolean> = _hideRecentApps.asStateFlow()

    fun setProtectScreenshots(enabled: Boolean) {
        _protectScreenshots.value = enabled
        sharedPrefs.edit().putBoolean("protect_screenshots", enabled).apply()
    }

    fun setPreventScreenRecording(enabled: Boolean) {
        _preventScreenRecording.value = enabled
        sharedPrefs.edit().putBoolean("prevent_screen_recording", enabled).apply()
    }

    fun setHideRecentApps(enabled: Boolean) {
        _hideRecentApps.value = enabled
        sharedPrefs.edit().putBoolean("hide_recent_apps", enabled).apply()
    }

    // --- PREMIUM STATUS ---
    private val _isPremium = MutableStateFlow(sharedPrefs.getBoolean("is_premium", false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun startPremiumTrial() {
        _isPremium.value = true
        sharedPrefs.edit().putBoolean("is_premium", true).apply()
    }

    fun setPremium(value: Boolean) {
        _isPremium.value = value
        sharedPrefs.edit().putBoolean("is_premium", value).apply()
    }

    fun hasShownPaywall(): Boolean {
        return sharedPrefs.getBoolean("has_shown_paywall", false)
    }

    fun setHasShownPaywall(value: Boolean) {
        sharedPrefs.edit().putBoolean("has_shown_paywall", value).apply()
    }

    fun hasCompletedOnboarding(): Boolean {
        return sharedPrefs.getBoolean("has_completed_onboarding", false)
    }

    fun setCompletedOnboarding(value: Boolean) {
        sharedPrefs.edit().putBoolean("has_completed_onboarding", value).apply()
    }

    // --- LANGUAGE ---
    private val _selectedLanguage = MutableStateFlow(
        sharedPrefs.getString("selected_language", null) ?: getSystemDefaultLanguage()
    )
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun selectLanguage(lang: String) {
        _selectedLanguage.value = lang
        sharedPrefs.edit()
            .putString("selected_language", lang)
            .putBoolean("has_selected_language", true)
            .apply()
    }

    fun hasSelectedLanguage(): Boolean {
        return sharedPrefs.getBoolean("has_selected_language", false)
    }

    // --- SEARCH / SORTING ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFolderName = MutableStateFlow<String?>(null)
    val selectedFolderName = _selectedFolderName.asStateFlow()

    private val _sortType = MutableStateFlow(SortType.NEWEST)
    val sortType = _sortType.asStateFlow()

    enum class SortType {
        NEWEST,
        OLDEST,
        NAME_A_Z,
        NAME_Z_A,
        LARGEST_SIZE,
        SMALLEST_SIZE,
        LONGEST_DURATION,
        SHORTEST_DURATION
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolder(folder: String?) {
        _selectedFolderName.value = folder
    }

    fun updateSortType(type: SortType) {
        _sortType.value = type
    }

    // --- VIDEO STATES ---
    private val _scannedVideos = MutableStateFlow<List<Video>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    // Filtered / Sorted Videos
    val videos: StateFlow<List<Video>> = combine(
        _scannedVideos,
        _searchQuery,
        _selectedFolderName,
        _sortType
    ) { rawVideos, query, folder, sort ->
        var filtered = rawVideos

        // Folder filter
        if (folder != null) {
            filtered = filtered.filter { it.folderName == folder }
        }

        // Search query filter (instant search across title, folderName, or filename)
        if (query.isNotEmpty()) {
            filtered = filtered.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.folderName.contains(query, ignoreCase = true) ||
                File(it.path).name.contains(query, ignoreCase = true)
            }
        }

        // Apply sort
        when (sort) {
            SortType.NEWEST -> filtered.sortedByDescending { it.dateAdded }
            SortType.OLDEST -> filtered.sortedBy { it.dateAdded }
            SortType.NAME_A_Z -> filtered.sortedBy { it.title.lowercase() }
            SortType.NAME_Z_A -> filtered.sortedByDescending { it.title.lowercase() }
            SortType.LARGEST_SIZE -> filtered.sortedByDescending { it.size }
            SortType.SMALLEST_SIZE -> filtered.sortedBy { it.size }
            SortType.LONGEST_DURATION -> filtered.sortedByDescending { it.duration }
            SortType.SHORTEST_DURATION -> filtered.sortedBy { it.duration }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group scanned videos into Folders
    val folders: StateFlow<List<FolderInfo>> = _scannedVideos.map { list ->
        list.groupBy { it.folderName }.map { (name, videosInFolder) ->
            FolderInfo(
                name = name,
                videoCount = videosInFolder.size,
                totalSize = videosInFolder.sumOf { it.size },
                recentVideo = videosInFolder.maxByOrNull { it.dateAdded }
            )
        }.sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- RECENT / HISTORY & FAVORITES FLOWS ---
    val recentVideos: StateFlow<List<RecentVideo>> = repository.recentVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteVideos: StateFlow<List<FavoriteVideo>> = repository.favoriteVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun isFavorite(video: Video): Flow<Boolean> = repository.isFavoriteFlow(video.path)

    // --- PRIVATE VAULT SECURED STATES ---
    val vaultVideos: StateFlow<List<VaultVideo>> = repository.vaultVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _vaultUnlocked = MutableStateFlow(false)
    val vaultUnlocked = _vaultUnlocked.asStateFlow()

    fun lockVault() {
        _vaultUnlocked.value = false
    }

    fun isVaultPinSet(): Boolean {
        return sharedPrefs.contains("vault_pin")
    }

    fun setVaultPin(pin: String) {
        sharedPrefs.edit().putString("vault_pin", pin).apply()
    }

    fun verifyVaultPin(pin: String): Boolean {
        val savedPin = sharedPrefs.getString("vault_pin", null)
        val matches = savedPin == pin
        if (matches) {
            _vaultUnlocked.value = true
        }
        return matches
    }

    // --- INIT SCAN ---
    init {
        scanVideos()
    }

    fun scanVideos() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val list = repository.scanLocalVideos()
                _scannedVideos.value = list
            } catch (e: Exception) {
                _scannedVideos.value = emptyList()
            } finally {
                _isScanning.value = false
            }
        }
    }

    // --- ACTIONS ---
    fun toggleFavorite(video: Video) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
        }
    }

    fun addToRecent(video: Video, position: Long) {
        viewModelScope.launch {
            repository.addToRecent(video, position)
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun getVideosInPlaylist(playlistId: Long): Flow<List<PlaylistVideo>> {
        return repository.getVideosInPlaylist(playlistId)
    }

    fun addVideoToPlaylist(playlistId: Long, video: Video) {
        viewModelScope.launch {
            repository.addVideoToPlaylist(playlistId, video)
        }
    }

    fun removeVideoFromPlaylist(playlistId: Long, videoPath: String) {
        viewModelScope.launch {
            repository.removeVideoFromPlaylist(playlistId, videoPath)
        }
    }

    fun moveVideoToVault(video: Video) {
        viewModelScope.launch {
            val success = repository.importVideoToVault(video)
            if (success) {
                // Refresh list
                scanVideos()
            }
        }
    }

    fun restoreVideoFromVault(vaultVideo: VaultVideo) {
        viewModelScope.launch {
            val success = repository.exportVideoFromVault(vaultVideo)
            if (success) {
                scanVideos()
            }
        }
    }

    fun deleteVideoFromVault(vaultVideo: VaultVideo) {
        viewModelScope.launch {
            repository.deleteVideoFromVault(vaultVideo)
        }
    }

    fun renameVideo(video: Video, newName: String) {
        viewModelScope.launch {
            val success = repository.renameLocalFile(video, newName)
            if (success) {
                scanVideos()
            }
        }
    }

    fun deleteVideo(video: Video) {
        viewModelScope.launch {
            val success = repository.deleteLocalFile(video)
            if (success) {
                scanVideos()
            }
        }
    }
}

data class FolderInfo(
    val name: String,
    val videoCount: Int,
    val totalSize: Long,
    val recentVideo: Video?
) {
    val totalSizeFormatted: String
        get() {
            if (totalSize <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(totalSize.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) return "$totalSize B"
            return String.format(
                java.util.Locale.getDefault(),
                "%.1f %s",
                totalSize / Math.pow(1024.0, digitGroups.toDouble()),
                units[digitGroups]
            )
        }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: VideoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
