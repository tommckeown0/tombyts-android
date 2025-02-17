package com.example.tombyts_android

import android.net.Uri
import android.util.Log
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MoviePlayerScreen(movieTitle: String, token: String, navController: NavController) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var moviePath: String? by remember { mutableStateOf(null) }
    var playbackPosition by rememberSaveable { mutableLongStateOf(0L) }
    var playWhenReady by rememberSaveable { mutableStateOf(true) }
    var progress by rememberSaveable { mutableDoubleStateOf(0.0) }
    val hasSeeked = rememberSaveable { mutableStateOf(false) }

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

            val mediaItem = MediaItem.fromUri(Uri.parse("https://${BuildConfig
                .HOME_PC_IP}:3001/media/$moviePath"))
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
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
