package com.example.tombyts_android

import android.net.Uri
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(UnstableApi::class)
@Composable
fun MoviePlayerScreen(movieTitle: String, token: String, navController: NavController) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var mediaSession by remember { mutableStateOf<MediaSession?>(null) }
    var moviePath: String? by remember { mutableStateOf(null) }
    var playbackPosition by rememberSaveable { mutableLongStateOf(0L) }
    var playWhenReady by rememberSaveable { mutableStateOf(true) }
    var progress by rememberSaveable { mutableDoubleStateOf(0.0) }
    val hasSeeked = rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // App is coming to foreground or screen is initialized/resumed
                    // Ensure playWhenReady is considered here
                    if (playWhenReady) {
                        player.play()
                    } else {
                        player.pause() // If it was paused when backgrounded
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // App is going to background or screen is paused
                    // Save the current playWhenReady state before pausing
                    playWhenReady = player.playWhenReady
                    player.pause()
                }
                // Lifecycle.Event.ON_STOP -> {
                // Optional: If you want to pause even on partial visibility loss, use ON_STOP
                // player.pause()
                // }
                // Lifecycle.Event.ON_RESUME -> {
                // ON_START is often sufficient, but you could use ON_RESUME too
                // player.play() // Only if playWhenReady is true - ON_START handles this better
                // }
                else -> { /* Do Nothing */ }
            }
        }

        // Add the observer to the lifecycle owner
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val playerListener = remember {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !hasSeeked.value) {
                    if (player.duration != -9223372036854775807L) {
                        playbackPosition = (player.duration * (progress / 100)).toLong()
                        player.seekTo(playbackPosition)
                        hasSeeked.value = true
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                // Log available subtitle tracks
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            Log.d("Subtitles", "Available subtitle track: ${format.language} - ${format.label ?: "Unknown"}")
                        }
                    }
                }

                // Automatically select first English subtitle track if available
                val trackSelector = (player as ExoPlayer).trackSelector
                if (trackSelector is DefaultTrackSelector) {
                    val parametersBuilder = trackSelector.buildUponParameters()

                    // Prefer English subtitles
                    parametersBuilder.setPreferredTextLanguage("en")

                    // You can also force subtitles to be selected
                    // parametersBuilder.setRendererDisabled(C.TRACK_TYPE_TEXT, false)

                    trackSelector.setParameters(parametersBuilder)
                }
            }

//            override fun onTracksChanged(tracks: Tracks) {
//                for (group in tracks.groups) {
//                    for (i in 0 until group.length) {
//                        val format = group.getTrackFormat(i)
//                        Log.d("Tracks", "Track: ${format.sampleMimeType} - ${format.language} - ${format.label}")
//                    }
//                }
//            }
        }
    }

    LaunchedEffect(player){
        player.addListener(playerListener)
    }

    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = Classes.ApiProvider.apiService.getMovieDetails(movieTitle, "Bearer $token")
                if (response.isSuccessful) {
                    val movieDetails = response.body()
                    moviePath = movieDetails?.path?.let { Uri.encode(it) }
                } else {
                    Log.d("API Error", "Failed to fetch movie details: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.d("API Error", "Error: ${e.message}")
            }
        }
    }

    LaunchedEffect(moviePath) {
        mediaSession?.release()
        mediaSession = MediaSession.Builder(context, player).build()
        hasSeeked.value = false
        if (moviePath != null) {
            try {
                val progressResponse = Classes.ApiProvider.apiService.getProgress(movieTitle, "Bearer $token")
                if (progressResponse.isSuccessful) {
                    progress = progressResponse.body()?.progress ?: 0.0
                } else {
                    Log.d("API Error", "Failed to fetch progress: ${progressResponse.code()}")
                }
            } catch (e: Exception) {
                Log.d("API Error", "Error fetching progress: ${e.message}")
            }

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse("https://${BuildConfig.SERVER_IP}:3001/media/$moviePath"))

            var subtitleContent: String? = null
            try {
                val subtitleResponse = Classes.ApiProvider.getSubtitleApiService().getSubtitles(movieTitle, "en", "Bearer $token")
                if (subtitleResponse.isSuccessful) {
                    subtitleContent = subtitleResponse.body()
                } else {
                    Log.d("API Error", "Failed to fetch subtitles: ${subtitleResponse.code()}")
                }
            } catch (e: Exception) {
                Log.d("API Error", "Error fetching subtitles: ${e.message}")
            }

//            val mediaItemBuilder = MediaItem.Builder()
//                .setUri(Uri.parse("https://${BuildConfig.SERVER_IP}:3001/media/$moviePath"))

            if (!subtitleContent.isNullOrBlank()) {
                val tempFile = File.createTempFile("subtitle", ".vtt", context.cacheDir)
                FileOutputStream(tempFile).use { fileOutputStream ->
                    fileOutputStream.write(subtitleContent.toByteArray())
                }

                val subtitleUri = Uri.fromFile(tempFile)
                val subtitle = SubtitleConfiguration.Builder(subtitleUri)
                    .setMimeType("text/vtt")
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()

                mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
            }

            val mediaItem = mediaItemBuilder.build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = playWhenReady
        }
    }

    LaunchedEffect(player.isPlaying){
        while (true){
            delay(5000)
            if (player.isPlaying){
                val currentProgress = if (player.duration != 0L) {
                    (player.currentPosition.toDouble() / player.duration.toDouble()) * 100
                } else {
                    0.0
                }
                try {
                val progressUpdate = ProgressUpdate(currentProgress.toInt())
                    val response = Classes.ApiProvider.apiService.updateProgress(movieTitle, "Bearer $token", progressUpdate)
                    if (response.isSuccessful) {
                    } else {
                        Log.d("API Error", "Failed to update progress: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.d("API Error", "Error updating progress: ${e.message}")
                }
            }
        }
    }

    DisposableEffect(Unit){
        onDispose {
            playbackPosition = player.currentPosition
            playWhenReady = player.playWhenReady
            player.removeListener(playerListener)
            player.release()
            mediaSession?.release()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = true
                    this.player = player
                    isFocusable = true
                    isFocusableInTouchMode = true

                    post {
                        if (!hasFocus()) {
                            requestFocus()
                            Log.d("Focus", "PlayerView requested focus via post")
                        }
                    }

                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        if (visibility == View.GONE) {
                            postDelayed({ requestFocus() }, 100)
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
