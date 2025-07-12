package dev.aaa1115910.bv.player

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.aaa1115910.bv.player.impl.exo.ExoMediaPlayer
import dev.aaa1115910.bv.player.impl.vlc.VlcMediaPlayer

@OptIn(UnstableApi::class)
@Composable
fun BvVideoPlayer(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,
    playerListener: VideoPlayerListener,
    // LibVLC 要用，误删
    isVerticalVideo: Boolean = false
) {
    DisposableEffect(Unit) {
        videoPlayer.setPlayerEventListener(playerListener)

        onDispose {
            videoPlayer.release()
        }
    }

    when (videoPlayer) {
        is ExoMediaPlayer -> {
            // 订阅播放器实例ID，当播放器重建时，这里会得到通知并触发重组
            val playerInstanceId = videoPlayer.playerInstanceId

            AndroidView(
                modifier = modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                },
                update = { view ->
                    // 每次重组时（包括播放器重建后），都将最新的播放器实例设置给PlayerView
                    if (playerInstanceId > 0) {
                        view.player = videoPlayer.mPlayer
                    }
                }
            )
        }

        is VlcMediaPlayer -> {
            // 订阅播放器实例ID，当播放器重建时，这里会得到通知并触发重组
            val playerInstanceId = videoPlayer.playerInstanceId

            AndroidView(
                modifier = modifier.fillMaxSize(),
                factory = { ctx ->
                    org.videolan.libvlc.util.VLCVideoLayout(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { vlcVideoLayout ->
                    if (playerInstanceId > 0) {
                        videoPlayer.vlcVideoLayout = vlcVideoLayout
                        // 使用VLC 4.0的正确API
                        videoPlayer.mPlayer?.vlcVout?.setVideoView(vlcVideoLayout)
                    }
                }
            )
        }
    }
}
