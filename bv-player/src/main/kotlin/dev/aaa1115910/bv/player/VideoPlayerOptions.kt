package dev.aaa1115910.bv.player

data class VideoPlayerOptions(
    val userAgent: String? = null,
    val referer: String? = null,
    val enableFfmpegAudioRenderer: Boolean = false,
    val audioDelayMs: Long = 0L // 音频延迟时间，单位毫秒，正值表示音频延迟，负值表示音频提前
)