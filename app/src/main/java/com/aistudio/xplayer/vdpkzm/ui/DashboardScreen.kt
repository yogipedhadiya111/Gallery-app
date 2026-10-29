package com.aistudio.xplayer.vdpkzm.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.xplayer.vdpkzm.R
import com.aistudio.xplayer.vdpkzm.data.FavoriteVideo
import com.aistudio.xplayer.vdpkzm.data.Playlist
import com.aistudio.xplayer.vdpkzm.data.PlaylistVideo
import com.aistudio.xplayer.vdpkzm.data.RecentVideo
import com.aistudio.xplayer.vdpkzm.data.VaultVideo
import com.aistudio.xplayer.vdpkzm.data.Video
import com.aistudio.xplayer.vdpkzm.monetization.AdManager
import com.aistudio.xplayer.vdpkzm.monetization.BannerAdView
import kotlinx.coroutines.delay
import java.io.File
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onPlayVideo: (Video) -> Unit,
    onLaunchPaywall: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Screen State
    var currentTab by remember { mutableStateOf(0) } // 0=Videos, 1=Folders, 2=Playlists, 3=Vault, 4=Settings
    val videos by viewModel.videos.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentVideos by viewModel.recentVideos.collectAsState()
    val favoriteVideos by viewModel.favoriteVideos.collectAsState()
    val vaultVideos by viewModel.vaultVideos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    // Navigation & Folder Drilldown state
    val selectedFolder by viewModel.selectedFolderName.collectAsState()

    // Dialogue State Management
    var showPlaylistCreationDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    
    var showPlaylistChooserDialog by remember { mutableStateOf<Video?>(null) }
    var showRenameDialog by remember { mutableStateOf<Video?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinUnlockDialog by remember { mutableStateOf(false) }
    var pinFirstInput by remember { mutableStateOf("") }
    var pinSecondInput by remember { mutableStateOf("") }
    var pinUnlockInput by remember { mutableStateOf("") }

    // Interstitial Ad simulator Trigger
    var showPostPlayAd by remember { mutableStateOf(false) }

    // Shared Menu state for properties
    var infoDetailDialog by remember { mutableStateOf<Video?>(null) }

    // Synchronize private vault and unlock states with InterstitialAdManager
    LaunchedEffect(currentTab, showPinSetupDialog, showPinUnlockDialog) {
        com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager.isVaultUnlockActive = 
            (currentTab == 3 || showPinSetupDialog || showPinUnlockDialog)
    }

    // Handle screen transitions on tab change
    var isFirstTabLoad by remember { mutableStateOf(true) }
    LaunchedEffect(currentTab) {
        if (isFirstTabLoad) {
            isFirstTabLoad = false
        } else {
            val activity = context as? Activity
            if (activity != null) {
                com.aistudio.xplayer.vdpkzm.monetization.InterstitialAdManager.onScreenTransition(activity)
            }
        }
    }

    // Sensitive screens security override inside Dashboard (Vault tab or passcode dialogs)
    if (currentTab == 3 || showPinSetupDialog || showPinUnlockDialog) {
        SecureScreen(isSensitive = true, viewModel = viewModel)
    }

    Scaffold(
        bottomBar = {
            Column {
                if (!isPremium) {
                    when (currentTab) {
                        0 -> BannerAdView(
                            adUnitId = "ca-app-pub-2878814018092038/6658849637",
                            modifier = Modifier.fillMaxWidth()
                        )
                        1 -> BannerAdView(
                            adUnitId = "ca-app-pub-2878814018092038/6243952051",
                            modifier = Modifier.fillMaxWidth()
                        )
                        2 -> BannerAdView(
                            adUnitId = "ca-app-pub-2878814018092038/7017977676",
                            modifier = Modifier.fillMaxWidth()
                        )
                        4 -> BannerAdView(
                            adUnitId = "ca-app-pub-2878814018092038/6658849637",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                val navBarDividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .drawBehind {
                            drawLine(
                                color = navBarDividerColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    val items = listOf(
                        Triple("Videos", Icons.Filled.Movie, Icons.Outlined.Movie),
                        Triple("Folders", Icons.Filled.Folder, Icons.Outlined.Folder),
                        Triple("Playlists", Icons.AutoMirrored.Filled.PlaylistPlay, Icons.AutoMirrored.Outlined.PlaylistPlay),
                        Triple("Private Vault", Icons.Filled.EnhancedEncryption, Icons.Outlined.EnhancedEncryption),
                        Triple("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
                    )
                    items.forEachIndexed { index, (label, filledIcon, outlinedIcon) ->
                        NavigationBarItem(
                            selected = currentTab == index,
                            onClick = {
                                if (index == 3 && !viewModel.vaultUnlocked.value) {
                                    if (viewModel.isVaultPinSet()) {
                                        showPinUnlockDialog = true
                                        pinUnlockInput = ""
                                    } else {
                                        showPinSetupDialog = true
                                        pinFirstInput = ""
                                        pinSecondInput = ""
                                    }
                                } else {
                                    currentTab = index
                                }
                            },
                            label = { 
                                Text(
                                    text = label, 
                                    fontSize = 10.sp, 
                                    fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Medium
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onTertiary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.tertiary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == index) filledIcon else outlinedIcon,
                                    contentDescription = label
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentTab == 2) {
                ExtendedFloatingActionButton(
                    text = { Text("Create Playlist") },
                    icon = { Icon(Icons.Default.Add, "Create") },
                    onClick = {
                        playlistNameInput = ""
                        showPlaylistCreationDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    0 -> VideosTabContent(
                        videos = videos,
                        recentVideos = recentVideos,
                        favoriteVideos = favoriteVideos,
                        searchQuery = searchQuery,
                        isScanning = isScanning,
                        sortType = sortType,
                        selectedFolder = selectedFolder,
                        isPremium = isPremium,
                        foldersCount = folders.size,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onSortChange = { viewModel.updateSortType(it) },
                        onClearFolderFilter = { viewModel.selectFolder(null) },
                        onVideoClick = { video ->
                            onPlayVideo(video)
                        },
                        onAddToPlaylist = { video ->
                            showPlaylistChooserDialog = video
                        },
                        onToggleFav = { viewModel.toggleFavorite(it) },
                        onRename = { video ->
                            renameInput = video.title
                            showRenameDialog = video
                        },
                        onHide = { viewModel.moveVideoToVault(it) },
                        onDelete = { viewModel.deleteVideo(it) },
                        onShowInfo = { infoDetailDialog = it },
                        onVaultPromoClick = {
                            if (viewModel.isVaultPinSet()) {
                                showPinUnlockDialog = true
                                pinUnlockInput = ""
                            } else {
                                showPinSetupDialog = true
                                pinFirstInput = ""
                                pinSecondInput = ""
                            }
                        },
                        onRefresh = { viewModel.scanVideos() },
                        onLaunchPaywall = onLaunchPaywall
                    )
                    1 -> FoldersTabContent(
                        folders = folders,
                        isPremium = isPremium,
                        onFolderClick = { folder ->
                            viewModel.selectFolder(folder.name)
                            currentTab = 0 // jump to Videos with filter
                        }
                    )
                    2 -> PlaylistsTabContent(
                        playlists = playlists,
                        viewModel = viewModel,
                        onPlayPlaylistVideo = { path ->
                            val matched = videos.find { it.path == path } ?: Video(
                                uriString = path, path = path, title = File(path).nameWithoutExtension, duration = 0, size = 0, dateAdded = System.currentTimeMillis() / 1000, folderName = "Playlist"
                            )
                            onPlayVideo(matched)
                        },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) }
                    )
                    3 -> VaultTabContent(
                        vaultVideos = vaultVideos,
                        onRestore = { viewModel.restoreVideoFromVault(it) },
                        onDeleteSecurely = { viewModel.deleteVideoFromVault(it) },
                        onLock = {
                            viewModel.lockVault()
                            currentTab = 0
                        },
                        onPlayVaultVideo = { v ->
                            // Render play
                            val mockVideo = Video(
                                uriString = v.originalPath,
                                path = v.originalPath,
                                title = v.title,
                                duration = v.duration,
                                size = v.size,
                                dateAdded = v.importTimestamp / 1000,
                                folderName = "Private Vault"
                            )
                            onPlayVideo(mockVideo)
                        }
                    )
                    4 -> SettingsTabContent(
                        viewModel = viewModel,
                        onLaunchPaywall = onLaunchPaywall
                    )
                }
            }
        }
    }

    // --- POPUP DIALOGS INTERACTIVE IMPLEMENTATION ---

    // 1. Playlist creation
    if (showPlaylistCreationDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistCreationDialog = false },
            title = { Text("Create Custom Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createPlaylist(playlistNameInput)
                            showPlaylistCreationDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaylistCreationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Playlist chooser overlay
    showPlaylistChooserDialog?.let { targetVideo ->
        AlertDialog(
            onDismissRequest = { showPlaylistChooserDialog = null },
            title = { Text("Add Video to Playlist") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (playlists.isEmpty()) {
                        Text(
                            "You don't have any custom playlists yet. Create one in the Playlists tab!",
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn {
                            items(playlists) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addVideoToPlaylist(p.id, targetVideo)
                                            Toast.makeText(context, "Added to playlist!", Toast.LENGTH_SHORT).show()
                                            showPlaylistChooserDialog = null
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(p.name, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 3. Rename dialog
    showRenameDialog?.let { targetVideo ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Value parameters") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank() && renameInput != targetVideo.title) {
                            viewModel.renameVideo(targetVideo, renameInput)
                            showRenameDialog = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
            }
        )
    }

    // 4. Secure PIN Setup (Private files)
    if (showPinSetupDialog) {
        Dialog(onDismissRequest = { showPinSetupDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EnhancedEncryption, "Lock", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Secure Vault PIN Code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Set up a unique 4-digit security PIN to restrict access to your private local videos.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = pinFirstInput,
                        onValueChange = { if (it.length <= 4) pinFirstInput = it },
                        label = { Text("Enter 4-Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinSecondInput,
                        onValueChange = { if (it.length <= 4) pinSecondInput = it },
                        label = { Text("Confirm PIN Code") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (pinFirstInput.length == 4 && pinFirstInput == pinSecondInput) {
                                viewModel.setVaultPin(pinFirstInput)
                                showPinSetupDialog = false
                                showPinUnlockDialog = true // Trigger unlock flow
                            } else {
                                Toast.makeText(context, "PIN code fields must match 4 digits!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save passcode")
                    }
                }
            }
        }
    }

    // 5. Secure PIN entry to Unlock Vault
    if (showPinUnlockDialog) {
        Dialog(onDismissRequest = { showPinUnlockDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, "Lock", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Unlock Vault Room", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = pinUnlockInput,
                        onValueChange = {
                            if (it.length <= 4) pinUnlockInput = it
                            if (it.length == 4) {
                                val success = viewModel.verifyVaultPin(it)
                                if (success) {
                                    showPinUnlockDialog = false
                                    currentTab = 3 // Jump to Vault view tab!
                                    Toast.makeText(context, "Access Granted!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
                                    pinUnlockInput = ""
                                }
                            }
                        },
                        label = { Text("Enter 4-Digit Passcode") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = { showPinUnlockDialog = false }) { Text("Cancel") }
                }
            }
        }
    }

    // 6. Infopanel properties drawer
    infoDetailDialog?.let { info ->
        AlertDialog(
            onDismissRequest = { infoDetailDialog = null },
            title = { Text("File Properties") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Title: ${info.title}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Duration: ${info.durationFormatted}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Size: ${info.sizeFormatted}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Location: ${info.path}")
                }
            },
            confirmButton = {
                Button(onClick = { infoDetailDialog = null }) { Text("Close") }
            }
        )
    }
}

// ==========================================
// --- TABS DETAILED LAYOUTS ---
// ==========================================

@Composable
fun VideosTabContent(
    videos: List<Video>,
    recentVideos: List<RecentVideo>,
    favoriteVideos: List<FavoriteVideo>,
    searchQuery: String,
    isScanning: Boolean,
    sortType: MainViewModel.SortType,
    selectedFolder: String?,
    isPremium: Boolean,
    foldersCount: Int,
    onSearchChange: (String) -> Unit,
    onSortChange: (MainViewModel.SortType) -> Unit,
    onClearFolderFilter: () -> Unit,
    onVideoClick: (Video) -> Unit,
    onAddToPlaylist: (Video) -> Unit,
    onToggleFav: (Video) -> Unit,
    onRename: (Video) -> Unit,
    onHide: (Video) -> Unit,
    onDelete: (Video) -> Unit,
    onShowInfo: (Video) -> Unit,
    onVaultPromoClick: () -> Unit,
    onRefresh: () -> Unit,
    onLaunchPaywall: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        
        // Premium Cinematic Header Image (Using our beautifully generated banner)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_dashboard_banner_1781706545173),
                contentDescription = "Cinematic Header",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = if (selectedFolder != null) "📁 $selectedFolder" else "XPlayer Pro Cinema",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Experience 4K hardware-accelerated playback with Dolby Equalization",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Search Bar & Filter Options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search title, path or tag", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Short Option menu dropdown button
            var sortMenuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.SortByAlpha,
                        contentDescription = "Sort Menu",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    val sorts = listOf(
                        Pair("Newest First", MainViewModel.SortType.NEWEST),
                        Pair("Oldest First", MainViewModel.SortType.OLDEST),
                        Pair("Name A-Z", MainViewModel.SortType.NAME_A_Z),
                        Pair("Name Z-A", MainViewModel.SortType.NAME_Z_A),
                        Pair("Largest Size", MainViewModel.SortType.LARGEST_SIZE),
                        Pair("Smallest Size", MainViewModel.SortType.SMALLEST_SIZE),
                        Pair("Longest Duration", MainViewModel.SortType.LONGEST_DURATION),
                        Pair("Shortest Duration", MainViewModel.SortType.SHORTEST_DURATION)
                    )
                    sorts.forEach { (label, type) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onSortChange(type)
                                sortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortType == type) {
                                    Icon(Icons.Filled.Check, contentDescription = "Active")
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Library",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (selectedFolder != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Showing videos in folder '$selectedFolder'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Clear filter",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow,
                    modifier = Modifier.clickable { onClearFolderFilter() }
                )
            }
        }

        // Secure Permission & Empty State
        val context = LocalContext.current
        val permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val isPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            permissionName
        ) == PackageManager.PERMISSION_GRANTED

        if (!isPermissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Storage Access Required",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Storage Permission Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "XPlayer Pro requires read access to find, catalog, and play your offline videos from internal storage, SD cards, and download directories.\n\nAll scanning is processed securely and privately on-device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val activity = context as? Activity
                                activity?.let {
                                    androidx.core.app.ActivityCompat.requestPermissions(
                                        it,
                                        arrayOf(permissionName),
                                        101
                                    )
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Storage Access", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open settings.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Open App Settings", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else if (videos.isEmpty() && !isScanning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = "No Videos Found",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No Videos Found",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "No supported videos (MP4, MKV, AVI, MOV, FLV, WebM, etc.) were found in your local directories.\nVerify they are on your device and rescan below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan / Rescan Storage", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                // STATISTICS CARD & STORAGE CARD ROW
                if (searchQuery.isEmpty() && selectedFolder == null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Calculate library size
                            val totalSize = videos.sumOf { it.size }
                            val formattedSize = if (totalSize > 1024 * 1024 * 1024) {
                                String.format("%.2f GB", totalSize.toFloat() / (1024 * 1024 * 1024))
                            } else {
                                String.format("%.1f MB", totalSize.toFloat() / (1024 * 1024))
                            }

                            // Statistics Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Analytics,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Quick Stats",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${videos.size} Videos",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$foldersCount Folders",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }

                            // Storage Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Storage,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "App Storage",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = formattedSize,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Video Cache Size",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // PREMIUM UPGRADE BANNER
                if (!isPremium && searchQuery.isEmpty() && selectedFolder == null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable { onLaunchPaywall() },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E153D))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFFFD700).copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = "Premium",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Upgrade to Premium Cinema 👑",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFFFFD700)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Ad-free playback & Atmos Equalization",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFD700), RoundedCornerShape(18.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "UPGRADE",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // QUICK ACTION ROW CHIPS
                if (searchQuery.isEmpty() && selectedFolder == null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = onRefresh,
                                label = { Text("🔄 Rescan Library", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = false,
                                onClick = onVaultPromoClick,
                                label = { Text("🔒 Open Private Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(12.dp)
                            )
                            if (!isPremium) {
                                FilterChip(
                                    selected = false,
                                    onClick = onLaunchPaywall,
                                    label = { Text("💎 Buy Premium", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // RECENT WATCH HISTORY HORIZONTAL BAND (Only if not searching or filtered)
                if (recentVideos.isNotEmpty() && searchQuery.isEmpty() && selectedFolder == null) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                "🕒 Recents & Watch History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                recentVideos.take(6).forEach { rec ->
                                    RecentVideoCard(
                                        rec = rec,
                                        onCardClick = {
                                            // construct Video entity
                                            val convertedVideo = Video(
                                                uriString = rec.path,
                                                path = rec.path,
                                                title = rec.title,
                                                duration = rec.duration,
                                                size = 0,
                                                dateAdded = rec.lastPlayedTimestamp / 1000,
                                                folderName = rec.path.substringBeforeLast("/", "Local")
                                            )
                                            onVideoClick(convertedVideo)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // PRIVATE VAULT PROMOTIONAL BANNER CARD (From high-fidelity HTML specs)
                if (searchQuery.isEmpty() && selectedFolder == null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .clickable { onVaultPromoClick() },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Inner icon container: w-10 h-10, bg-white, text/icon color #001552, rounded-full
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EnhancedEncryption,
                                            contentDescription = "Vault",
                                            tint = Color(0xFF001552),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column {
                                        Text(
                                            text = "Private Secure Vault",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF001552)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Password-protect personal video records",
                                            fontSize = 11.sp,
                                            color = Color(0xFF001552).copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // UNLOCK button (bg-[#001552], text-white, px-3, py-1.5, rounded-full, font-bold)
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF001552), RoundedCornerShape(18.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "UNLOCK",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ALL VIDEOS HEADER
                item {
                    Text(
                        "🎥 Dynamic Videos List (${videos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                }

                // MASTER VIDEOS SCROLL IT
                itemsIndexed(videos) { index, item ->
                    VideoItemRow(
                        video = item,
                        onVideoClick = { onVideoClick(item) },
                        onToggleFav = { onToggleFav(item) },
                        onAddToPlaylist = { onAddToPlaylist(item) },
                        onRename = { onRename(item) },
                        onHide = { onHide(item) },
                        onDelete = { onDelete(item) },
                        onShowInfo = { onShowInfo(item) }
                    )

                    // Insert Native Ad Card every 9 video items, never at first position (index > 0)
                    if (!isPremium && index > 0 && index % 9 == 0) {
                        com.aistudio.xplayer.vdpkzm.monetization.NativeAdCard(
                            adUnitId = "ca-app-pub-2878814018092038/1001152670",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun VideoThumbnail(
    videoUriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(videoUriString) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(videoUriString) {
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(videoUriString)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bitmap = try {
                        context.contentResolver.loadThumbnail(uri, android.util.Size(180, 120), null)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    val id = uri.lastPathSegment?.toLongOrNull()
                    if (id != null) {
                        bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                            context.contentResolver,
                            id,
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                // Keep default play indicator
            }
        }
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun RecentVideoCard(
    rec: RecentVideo,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(190.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                contentAlignment = Alignment.Center
            ) {
                VideoThumbnail(
                    videoUriString = rec.path,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = rec.title,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Resume playback",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun VideoItemRow(
    video: Video,
    onVideoClick: () -> Unit,
    onToggleFav: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onVideoClick() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                VideoThumbnail(
                    videoUriString = video.uriString,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        video.durationFormatted,
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.sizeFormatted} • ${video.folderName}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Expand Menu trigger
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Filled.MoreVert, "Actions")
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.video_favorite)) },
                        onClick = { onToggleFav(); expandedMenu = false },
                        leadingIcon = { Icon(Icons.Default.Favorite, null, tint = Color.Red) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.video_add_to_playlist)) },
                        onClick = { onAddToPlaylist(); expandedMenu = false },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.video_secure_hide)) },
                        onClick = { onHide(); expandedMenu = false },
                        leadingIcon = { Icon(Icons.Default.EnhancedEncryption, null, tint = Color.Yellow) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.video_rename)) },
                        onClick = { onRename(); expandedMenu = false },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.video_delete)) },
                        onClick = { onDelete(); expandedMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.video_details)) },
                        onClick = { onShowInfo(); expandedMenu = false },
                        leadingIcon = { Icon(Icons.Default.Info, null) }
                    )
                }
            }
        }
    }
}

// FOLDERS CONTENT LISTS
sealed class FolderOrAd {
    data class Folder(val folderInfo: FolderInfo) : FolderOrAd()
    object Ad : FolderOrAd()
}

@Composable
fun FoldersTabContent(
    folders: List<FolderInfo>,
    isPremium: Boolean,
    onFolderClick: (FolderInfo) -> Unit
) {
    val gridItems = remember(folders, isPremium) {
        val list = mutableListOf<FolderOrAd>()
        folders.forEachIndexed { index, folder ->
            list.add(FolderOrAd.Folder(folder))
            if (!isPremium && index > 0 && (index + 1) % 10 == 0) {
                list.add(FolderOrAd.Ad)
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📁 SD Card & Storage Folders",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (folders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(id = R.string.no_folders_located), color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = gridItems,
                    span = { _, item ->
                        when (item) {
                            is FolderOrAd.Folder -> GridItemSpan(1)
                            is FolderOrAd.Ad -> GridItemSpan(2)
                        }
                    }
                ) { _, item ->
                    when (item) {
                        is FolderOrAd.Folder -> {
                            val folderInfo = item.folderInfo
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFolderClick(folderInfo) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Folder icon container: w-12 h-12, rounded-xl (12.dp) bg `#dbe1ff` with text/icon color `#001552`
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.tertiary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = folderInfo.name,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${folderInfo.videoCount} Videos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Text(
                                        text = folderInfo.totalSizeFormatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        is FolderOrAd.Ad -> {
                            com.aistudio.xplayer.vdpkzm.monetization.NativeAdCard(
                                adUnitId = "ca-app-pub-2878814018092038/4391814337",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// PLAYLISTS CONTENT MANAGEMENT
@Composable
fun PlaylistsTabContent(
    playlists: List<Playlist>,
    viewModel: MainViewModel,
    onPlayPlaylistVideo: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📋 Customized Video Playlists",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(id = R.string.no_playlists_built), textAlign = TextAlign.Center, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(playlists) { item ->
                    var showVideosListDropdown by remember { mutableStateOf(false) }
                    val currentPlayVideos by viewModel.getVideosInPlaylist(item.id).collectAsState(initial = emptyList())

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.clickable { showVideosListDropdown = !showVideosListDropdown }) {
                                    Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    Text(stringResource(id = R.string.videos_inside_count, currentPlayVideos.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Row {
                                    IconButton(onClick = { onDeletePlaylist(item.id) }) {
                                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            if (showVideosListDropdown) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (currentPlayVideos.isEmpty()) {
                                    Text(stringResource(id = R.string.playlist_empty_msg), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        currentPlayVideos.forEach { pVid ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onPlayPlaylistVideo(pVid.videoPath) }
                                                    .background(Color.Black.copy(0.12f), RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                                                Text(pVid.title, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                                IconButton(onClick = { viewModel.removeVideoFromPlaylist(item.id, pVid.videoPath) }) {
                                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// PRIVATE VAULT CONTENT TAB
@Composable
fun VaultTabContent(
    vaultVideos: List<VaultVideo>,
    onRestore: (VaultVideo) -> Unit,
    onDeleteSecurely: (VaultVideo) -> Unit,
    onLock: () -> Unit,
    onPlayVaultVideo: (VaultVideo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛡️ Private Secure Vault",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onLock) {
                Icon(Icons.Default.Lock, "Lock Vault", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (vaultVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.EnhancedEncryption, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your private safe-room is completely empty.\nTo secure videos, choose 'Securely Hide' in the general Videos tab context menus.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vaultVideos) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayVaultVideo(item) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(stringResource(id = R.string.imported_securely), fontSize = 11.sp, color = Color.Green)
                            }
                            Row {
                                IconButton(onClick = { onRestore(item) }) {
                                    Icon(Icons.Default.Restore, "Restore Publicly", tint = Color.Yellow)
                                }
                                IconButton(onClick = { onDeleteSecurely(item) }) {
                                    Icon(Icons.Default.Delete, "Delete Securely", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// SETTINGS PREFERENCES TABS
@Composable
fun SettingsTabContent(
    viewModel: MainViewModel,
    onLaunchPaywall: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeState.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        Dialog(
            onDismissRequest = { showLanguageDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                LanguageSelectionScreen(
                    currentSelectedLanguage = selectedLanguage,
                    onLanguageSelected = { lang ->
                        viewModel.selectLanguage(lang)
                        showLanguageDialog = false
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "⚙️ Playback Settings & Themes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Premium Membership Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { onLaunchPaywall() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPremium) Color(0xFF1E153D) else MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (isPremium) Color(0xFFFFD700) else Color.Transparent
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isPremium) Color(0xFFFFD700).copy(0.15f) else Color.White.copy(0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isPremium) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isPremium) stringResource(id = R.string.premium_active) else stringResource(id = R.string.unlock_premium) + " 👑",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isPremium) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPremium) stringResource(id = R.string.premium_slogan) else stringResource(id = R.string.premium_trial_desc),
                            fontSize = 11.sp,
                            color = if (isPremium) Color.White.copy(0.7f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            if (isPremium) Color.White.copy(0.12f) else MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isPremium) stringResource(id = R.string.active_btn) else stringResource(id = R.string.upgrade_btn),
                        color = if (isPremium) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Language Option Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable { showLanguageDialog = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_language_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.current_language_lbl, selectedLanguage),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.change_btn),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Theme Toggle Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Application Theme Vibe", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                
                val modes = listOf("Light Day Vibe", "Dark Theater", "AMOLED Blackout")
                modes.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateTheme(index) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label)
                        RadioButton(
                            selected = currentTheme == index,
                            onClick = { viewModel.updateTheme(index) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Policy Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yogipedhadiya-111.github.io/privacy-policy/")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to open Privacy Policy.", Toast.LENGTH_SHORT).show()
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = "Privacy Policy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy Policy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Read how we collect, use, and protect your information.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
