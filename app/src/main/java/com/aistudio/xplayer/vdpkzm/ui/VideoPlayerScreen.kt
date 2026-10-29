package com.aistudio.xplayer.vdpkzm.ui

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aistudio.xplayer.vdpkzm.XPlayerApplication
import com.aistudio.xplayer.vdpkzm.data.AudioEffectsManager
import com.aistudio.xplayer.vdpkzm.data.Video
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    video: Video,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as XPlayerApplication
    val audioEffectsManager = app.audioEffectsManager
    val viewModelStore = app.repository // Repos handles addToRecent

    // Coroutine & Player Controller State
    val scope = rememberCoroutineScope()
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0L) }
    var currentPosition by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }

    // Screen Overlays
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var aspectVal by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) } // fit, fill, zoom etc.
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    // Swipe Gestures Tracking
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    
    // Brightness gesture state
    val activity = context as? Activity
    var currentBrightness by remember {
        mutableStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 }
                ?: Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
        )
    }

    // Display temporary gesture progress overlays
    var gestureOverlayType by remember { mutableStateOf<String?>(null) } // "volume" or "brightness"
    var gestureValue by remember { mutableStateOf(0f) } // 0f to 1f

    // Double tap bubble overlay animation
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) } // "rewind" or "forward"

    // Dialog overlays
    var showEquilizerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    // Sleep Timer countdown values
    var sleepTimerMinutesLeft by remember { mutableStateOf<Int?>(null) }

    // Auto-Hide controls job
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(5000)
            controlsVisible = false
        }
    }

    // Progress updates countdown ticker
    LaunchedEffect(player, isPlaying) {
        while (player != null && isPlaying) {
            currentPosition = player?.currentPosition ?: 0
            duration = player?.duration ?: 0
            delay(500)
        }
    }

    // Sleep Timer active tick down
    LaunchedEffect(sleepTimerMinutesLeft) {
        sleepTimerMinutesLeft?.let { min ->
            if (min > 0) {
                delay(60000)
                val next = min - 1
                sleepTimerMinutesLeft = if (next == 0) {
                    player?.pause()
                    null
                } else {
                    next
                }
            }
        }
    }

    // Initialize ExoPlayer
    LaunchedEffect(video) {
        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(if (video.path.startsWith("http")) video.path else video.uriString)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
            
            // Connect native audio effects to the initialized Player session ID!
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        duration = this@apply.duration
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (playing && audioSessionId != 0) {
                        audioEffectsManager.initEffects(audioSessionId)
                    }
                }
            })
        }
        player = exoPlayer

        // Track History insertion
        scope.launch {
            viewModelStore.addToRecent(video, 0)
        }
    }

    // Dispose Player
    DisposableEffect(Unit) {
        onDispose {
            player?.let { p ->
                val lastPos = p.currentPosition
                scope.launch {
                    viewModelStore.addToRecent(video, lastPos)
                }
                p.release()
            }
            audioEffectsManager.release()
        }
    }

    // Back button override
    BackHandler {
        if (isLocked) {
            isLocked = false
            controlsVisible = true
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- 1. EXO PLAYER RENDER CANVAS VIEW ---
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (!isLocked) {
                                val isRight = offset.x > size.width / 2
                                if (isRight) {
                                    doubleTapFeedback = "forward"
                                    player?.let { p ->
                                        p.seekTo((p.currentPosition + 10000).coerceAtMost(p.duration))
                                    }
                                } else {
                                    doubleTapFeedback = "rewind"
                                    player?.let { p ->
                                        p.seekTo((p.currentPosition - 10000).coerceAtLeast(0))
                                    }
                                }
                                scope.launch {
                                    delay(650)
                                    doubleTapFeedback = null
                                }
                            }
                        },
                        onTap = {
                            controlsVisible = !controlsVisible
                        }
                    )
                }
                .pointerInput(isLocked) {
                    if (!isLocked) {
                        detectDragGestures(
                            onDragStart = { gestureOverlayType = null },
                            onDragEnd = { gestureOverlayType = null },
                            onDragCancel = { gestureOverlayType = null }
                        ) { change, dragAmount ->
                            change.consume()
                            val width = size.width
                            val height = size.height
                            
                            val isRightSide = change.position.x > width / 2
                            if (isRightSide) {
                                // Volume Gesture
                                val volDelta = -(dragAmount.y / height) * maxVolume * 1.5f
                                val newVol = (currentVolume + volDelta).coerceIn(0f, maxVolume.toFloat())
                                currentVolume = newVol.toInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
                                gestureOverlayType = "Volume"
                                gestureValue = currentVolume / maxVolume.toFloat()
                            } else {
                                // Brightness Gesture
                                val brightDelta = -(dragAmount.y / height) * 1.5f
                                currentBrightness = (currentBrightness + brightDelta).coerceIn(0.01f, 1f)
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    lp.screenBrightness = currentBrightness
                                    act.window.attributes = lp
                                }
                                gestureOverlayType = "Brightness"
                                gestureValue = currentBrightness
                            }
                        }
                    }
                },
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false // Custom overlay is used
                    keepScreenOn = true
                    resizeMode = aspectVal
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = player
                view.keepScreenOn = (player?.isPlaying == true || player?.playWhenReady == true)
                view.resizeMode = aspectVal
            }
        )

        // --- 2. PIP TRIGGER ASSIST PANEL ---
        if (isPlaying && controlsVisible && !isLocked) {
            IconButton(
                onClick = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val params = android.app.PictureInPictureParams.Builder().build()
                        activity?.enterPictureInPictureMode(params)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureInPicture,
                    contentDescription = "Picture-In-Picture",
                    tint = Color.White
                )
            }
        }

        // --- 3. DOUBLE-TAP POPUPS ANIMATIONS ---
        AnimatedVisibility(
            visible = doubleTapFeedback != null,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (doubleTapFeedback == "forward") Icons.Filled.FastForward else Icons.Filled.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (doubleTapFeedback == "forward") "+10 Sec" else "-10 Sec",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // --- 4. GESTURE TRACKING HUD OVERLAYS ---
        AnimatedVisibility(
            visible = gestureOverlayType != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            gestureOverlayType?.let { label ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                    modifier = Modifier.width(180.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (label == "Volume") {
                                if (gestureValue == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.Default.BrightnessHigh
                            },
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$label: ${(gestureValue * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { gestureValue },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // --- 5. LOADING / BUFFERING SPINNER ---
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
        }

        // --- 6. CONTROLLER OVERLAYS (MAIN HUD) ---
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { -30 }),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { -30 })
        ) {
            // Locked UI View state handles lock overlay exclusively
            if (isLocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = { isLocked = false },
                        modifier = Modifier
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Unlock Controls",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                // Fully featured control HUD
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    // --- TOP BAR OPTIONS ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (sleepTimerMinutesLeft != null) {
                                Text(
                                    text = "Timer: ${sleepTimerMinutesLeft}m remaining",
                                    color = Color.Yellow,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        // Sleep, Subtitles, Audio, EQ
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerMinutesLeft != null) Color.Yellow else Color.White
                            )
                        }
                        IconButton(onClick = { showSubtitleDialog = true }) {
                            Icon(Icons.Default.Subtitles, "Subtitles", tint = Color.White)
                        }
                        IconButton(onClick = { showEquilizerDialog = true }) {
                            Icon(Icons.Default.Equalizer, "Equalizer", tint = Color.White)
                        }
                    }

                    // --- CENTER: SEEK FLING & PLAY/PAUSE BACKWARD/FORWARD ---
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                player?.let { p ->
                                    p.seekTo((p.currentPosition - 10000).coerceAtLeast(0))
                                }
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                player?.let { p ->
                                    if (p.isPlaying) p.pause() else p.play()
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                player?.let { p ->
                                    p.seekTo((p.currentPosition + 10000).coerceAtMost(p.duration))
                                }
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Forward10, "Fast Forward 10s", tint = Color.White)
                        }
                    }

                    // --- BOTTOM CONTROLS ---
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        // PROGRESS SLIDER WITH DURATION TEXTS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatPosition(currentPosition),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() else 0f,
                                onValueChange = { pos ->
                                    player?.seekTo(pos.toLong())
                                    currentPosition = pos.toLong()
                                },
                                valueRange = 0f..(if (duration > 0) duration.toFloat() else 1f),
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Text(
                                text = formatPosition(duration),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ROW OF UTILITIES: Aspect Ratio, Speed, Lock, PiP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Lock
                            IconButton(onClick = { isLocked = true }) {
                                Icon(Icons.Default.LockOpen, "Lock Controls", tint = Color.White)
                            }

                            // Aspect Ratio Controller
                            Button(
                                onClick = {
                                    aspectVal = when (aspectVal) {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                val ratioLabel = when (aspectVal) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                                    else -> "Custom"
                                }
                                Icon(Icons.Default.AspectRatio, "Aspect", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(ratioLabel, color = Color.White, fontSize = 12.sp)
                            }

                            // Speed Toggler
                            Button(
                                onClick = { showSpeedDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Speed, "Speed", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${playbackSpeed}x", color = Color.White, fontSize = 12.sp)
                            }

                            // Brightness display label shortcut
                            IconButton(onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    val params = android.app.PictureInPictureParams.Builder().build()
                                    context.findActivity()?.enterPictureInPictureMode(params)
                                }
                            }) {
                                Icon(Icons.Default.PictureInPicture, "PiP", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS REGISTRY ---
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackSpeed = speed
                                    player?.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playbackSpeed == speed,
                                onClick = {
                                    playbackSpeed = speed
                                    player?.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("${speed}x", fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Dismiss") }
            }
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                val periods = listOf(
                    Pair("Off", null),
                    Pair("5 Minutes", 5),
                    Pair("15 Minutes", 15),
                    Pair("30 Minutes", 30),
                    Pair("45 Minutes", 45),
                    Pair("60 Minutes", 60)
                )
                Column {
                    periods.forEach { (label, min) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sleepTimerMinutesLeft = min
                                    showSleepTimerDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sleepTimerMinutesLeft == min,
                                onClick = {
                                    sleepTimerMinutesLeft = min
                                    showSleepTimerDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(label, fontWeight = if (sleepTimerMinutesLeft == min) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) { Text("Dismiss") }
            }
        )
    }

    if (showSubtitleDialog) {
        var subFontSize by remember { mutableStateOf(16f) }
        var subColorStr by remember { mutableStateOf("White") }

        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Local Subtitle Options") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Sync & Styling Parameters", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Font Size: ${subFontSize.toInt()}sp")
                    Slider(
                        value = subFontSize,
                        onValueChange = { subFontSize = it },
                        valueRange = 12f..32f,
                        steps = 4
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Text Accent Color:")
                    val colors = listOf("White", "Yellow", "Cyan", "Green")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { color ->
                            FilterChip(
                                selected = subColorStr == color,
                                onClick = { subColorStr = color },
                                label = { Text(color) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSubtitleDialog = false
                }) {
                    Text("Apply Style")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubtitleDialog = false }) { Text("Close") }
            }
        )
    }

    if (showEquilizerDialog) {
        Dialog(
            onDismissRequest = { showEquilizerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface),
                tonalElevation = 6.dp
            ) {
                EqualizerHUD(
                    manager = audioEffectsManager,
                    onDismiss = { showEquilizerDialog = false }
                )
            }
        }
    }
}

// Custom 5-Band Equalizer Overlay HUD UI representing native equalizers nicely
@Composable
fun EqualizerHUD(
    manager: AudioEffectsManager,
    onDismiss: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(manager.isEqEnabled) }
    var selectedPreset by remember { mutableStateOf(manager.selectedPreset) }
    var bassVal by remember { mutableStateOf(manager.bassLevel.toFloat()) }
    var virtVal by remember { mutableStateOf(manager.virtualizerStrength.toFloat()) }
    
    // Band controllers state
    val bandCount = manager.getBandCount()
    val bandRange = manager.getBandLevelRange()
    val bandLevels = remember {
        mutableStateListOf<Short>().apply {
            for (i in 0 until bandCount) {
                add(manager.getBandLevel(i.toShort()))
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "FX 10-Band Equalizer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = {
                    isEnabled = it
                    manager.isEqEnabled = it
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset selector drop list
        Text("Audio Environment Presets", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf("Custom", "Rock", "Pop", "Jazz", "Metal", "Flat")
            presets.forEachIndexed { idx, pLabel ->
                val targetIdx = (idx - 1).toShort()
                val isSelected = selectedPreset == targetIdx
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isEnabled) {
                            selectedPreset = targetIdx
                            manager.selectedPreset = targetIdx
                            // reload manual band arrays
                            for (i in 0 until bandCount) {
                                bandLevels[i] = manager.getBandLevel(i.toShort())
                            }
                        }
                    },
                    label = { Text(pLabel, fontSize = 11.sp) },
                    enabled = isEnabled
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // sliders for bands
        Text("Fine Tune Frequency Bands", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceSpacey()
        ) {
            for (i in 0 until bandCount) {
                val bandIdx = i.toShort()
                val freq = manager.getBandCenterFreq(bandIdx) / 1000 // In KHz
                val level = bandLevels[i]
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("${level / 100}dB", fontSize = 10.sp, color = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { newVal ->
                            if (isEnabled) {
                                bandLevels[i] = newVal.toInt().toShort()
                                manager.setBandLevel(bandIdx, newVal.toInt().toShort())
                                selectedPreset = -1 // changes preset to manual
                            }
                        },
                        valueRange = bandRange.first.toFloat()..bandRange.second.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = isEnabled
                    )
                    Text("${freq}Hz", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bass and Virtualizer bottom sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Bass Boost: ${bassVal.toInt() / 10}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = bassVal,
                    onValueChange = {
                        if (isEnabled) {
                            bassVal = it
                            manager.bassLevel = it.toInt().toShort()
                        }
                    },
                    valueRange = 0f..1000f,
                    enabled = isEnabled
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Virtualizer: ${virtVal.toInt() / 10}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = virtVal,
                    onValueChange = {
                        if (isEnabled) {
                            virtVal = it
                            manager.virtualizerStrength = it.toInt().toShort()
                        }
                    },
                    valueRange = 0f..1000f,
                    enabled = isEnabled
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply and Save settings")
        }
    }
}

// Helper arranging extension for equal spacing on vertical bands
@Composable
fun Arrangement.SpaceSpacey() = Arrangement.spacedBy(4.dp)

private fun formatPosition(positionMs: Long): String {
    if (positionMs <= 0) return "00:00"
    val totalSeconds = positionMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

// Context Activity retriever
fun Context.findActivity(): Activity? {
    var context = this
    while (context is androidx.lifecycle.LifecycleOwner) {
        if (context is Activity) return context
        // Try getting parent, etc
        break
    }
    return context as? Activity
}
