package dev.aaa1115910.bv.player

import android.view.SurfaceView

data class VideoPlayerOptions(
    val surfaceView: SurfaceView? = null,
    val userAgent: String? = null,
    val referer: String? = null,
    val enableFfmpegAudioRenderer: Boolean = true,
    val enableCache: Boolean = true
)