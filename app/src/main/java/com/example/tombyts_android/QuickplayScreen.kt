package com.example.tombyts_android

import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.SubtitleConfiguration
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import java.io.File
import java.io.FileOutputStream

@OptIn(UnstableApi::class)
@Composable
fun QuickPlayScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    val mediaSession = remember(player) { MediaSession.Builder(context, player).build() }
    val movieUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    val subtitleContent = """WEBVTT
        1
        00:00:00.000 --> 00:00:05.000
        Hello, this is a test subtitle.
        
        2
        00:00:05.500 --> 00:00:10.000
        Enjoy the movie!
        """

    LaunchedEffect(player) {
        val tempFile = File.createTempFile("subtitle", ".vtt", context.cacheDir)
        FileOutputStream(tempFile).use { fileOutputStream ->
            fileOutputStream.write(subtitleContent.toByteArray())
        }
        val subtitleUri = Uri.fromFile(tempFile)
        val subtitleConfiguration = SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType("text/vtt")
            .setLanguage("en")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(movieUrl)
            .setSubtitleConfigurations(listOf(subtitleConfiguration))
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player, mediaSession) {
        onDispose {
            player.release()
            mediaSession.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                val customLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.BLACK)
                    addView(TextView(context).apply {
                        text = "Media Controller"
                        textSize = 30f
                        setTextColor(android.graphics.Color.WHITE)
                        gravity = Gravity.CENTER
                    })
                }

                PlayerView(context).apply {
                    useController = true
                    this.player = player
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()

                    // Handle remote key presses
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_DPAD_UP,
                                KeyEvent.KEYCODE_DPAD_DOWN,
                                KeyEvent.KEYCODE_DPAD_LEFT,
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    // Show controller if hidden
                                    showController()
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}