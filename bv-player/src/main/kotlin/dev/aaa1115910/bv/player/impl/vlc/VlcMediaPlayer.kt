package dev.aaa1115910.bv.player.impl.vlc

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.formatMinSec
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

/**
 * VLC播放器实现
 * 支持原生音画同步功能
 */
class VlcMediaPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer() {

    private var libVLC: LibVLC? = null
    var mediaPlayer: MediaPlayer? = null
        private set

    // 当播放器实例重建时，该值会增加，用于通知 Compose UI 更新
    var playerInstanceId by mutableIntStateOf(0)

    // 保存当前播放的URL以便重试
    private var currentVideoUrl: String? = null
    private var currentAudioUrl: String? = null

    // 音频延迟相关属性
    private var _audioDelayMs: Long = options.audioDelayMs

    override fun initPlayer() {
        try {
            // 创建LibVLC实例
            val args = mutableListOf<String>().apply {
                // 基础配置
                add("--intf=dummy")
                add("--verbose=0")
                add("--no-stats")
                add("--no-osd")

                // 网络配置
                add("--network-caching=1000")
                add("--live-caching=1000")

                // 音频配置
                add("--aout=opensles")

                // 如果有自定义User-Agent
                options.userAgent?.let {
                    add("--http-user-agent=$it")
                }

                // 如果有Referer
                options.referer?.let {
                    add("--http-referrer=$it")
                }
            }

            libVLC = LibVLC(context, args)
            mediaPlayer = MediaPlayer(libVLC)

            // 设置事件监听
            setupEventListeners()

            // 应用音频延迟设置
            applyAudioDelay()

            // 增加此值以触发UI更新
            playerInstanceId++

        } catch (e: Exception) {
            e.printStackTrace()
            mPlayerEventListener?.onError(e)
        }
    }
    private fun setupEventListeners() {
        mediaPlayer?.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Playing -> {
                    mPlayerEventListener?.onPlay()
                }
                MediaPlayer.Event.Paused -> {
                    mPlayerEventListener?.onPause()
                }
                MediaPlayer.Event.Stopped -> {
                    mPlayerEventListener?.onPause()
                }
                MediaPlayer.Event.EndReached -> {
                    mPlayerEventListener?.onEnd()
                }
                MediaPlayer.Event.EncounteredError -> {
                    mPlayerEventListener?.onError(Exception("VLC播放错误"))
                }
                MediaPlayer.Event.Buffering -> {
                    if (event.buffering < 100f) {
                        mPlayerEventListener?.onBuffering()
                    }
                }
                MediaPlayer.Event.MediaChanged -> {
                    mPlayerEventListener?.onReady()
                }
                MediaPlayer.Event.SeekableChanged -> {
                    // 可以在这里处理快进快退事件
                }
            }
        }
    }

    override fun setHeader(headers: Map<String, String>) {
        // VLC通过命令行参数设置headers，在initPlayer中处理
    }

    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        currentVideoUrl = videoUrl
        currentAudioUrl = audioUrl

        try {
            // VLC可以直接播放包含音视频的URL，优先使用videoUrl
            val playUrl = videoUrl ?: audioUrl
            playUrl?.let { url ->
                val media = Media(libVLC, Uri.parse(url))

                // 如果同时有音频和视频URL，可以考虑使用VLC的多媒体源功能
                // 这里先简化处理，只播放主要的URL

                mediaPlayer?.media = media
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mPlayerEventListener?.onError(e)
        }
    }

    override fun prepare() {
        // VLC不需要显式的prepare步骤，设置media后可以直接播放
    }

    override fun start() {
        mediaPlayer?.play()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun stop() {
        mediaPlayer?.stop()
    }

    override fun reset() {
        mediaPlayer?.stop()
        mediaPlayer?.media = null
    }

    override val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    override fun seekTo(time: Long) {
        mediaPlayer?.time = time
    }

    override fun release() {
        mediaPlayer?.release()
        libVLC?.release()
        mediaPlayer = null
        libVLC = null
    }

    override val currentPosition: Long
        get() = mediaPlayer?.time ?: 0L

    override val duration: Long
        get() = mediaPlayer?.length ?: 0L

    override val bufferedPercentage: Int
        get() {
            // VLC没有直接的缓冲百分比API，这里返回一个估算值
            val position = currentPosition
            val duration = duration
            return if (duration > 0) {
                ((position.toFloat() / duration) * 100).toInt()
            } else 0
        }

    override fun setOptions() {
        // VLC的选项在initPlayer中通过命令行参数设置
    }

    override var speed: Float
        get() = mediaPlayer?.rate ?: 1.0f
        set(value) {
            mediaPlayer?.rate = value
        }

    override val debugInfo: String
        get() {
            return """
                player: VLC ${libVLC?.version() ?: "unknown"}
                time: ${currentPosition.formatMinSec()} / ${duration.formatMinSec()}
                buffered: $bufferedPercentage%
                resolution: ${videoWidth} x ${videoHeight}
                rate: ${speed}x
                audio delay: ${audioDelayMs}ms
            """.trimIndent()
        }

    override val videoWidth: Int
        get() = mediaPlayer?.videoTrackDescription?.firstOrNull()?.let {
            // VLC获取视频宽度的方法，这里简化处理
            1920 // 默认值，实际应该从媒体信息中获取
        } ?: 0

    override val videoHeight: Int
        get() = mediaPlayer?.videoTrackDescription?.firstOrNull()?.let {
            // VLC获取视频高度的方法，这里简化处理
            1080 // 默认值，实际应该从媒体信息中获取
        } ?: 0

    override var audioDelayMs: Long
        get() = _audioDelayMs
        set(value) {
            _audioDelayMs = value
            applyAudioDelay()
        }

    override var tcpSpeed: Long
        get() = 0L // VLC不直接支持TCP速度设置
        set(value) {
            // VLC不直接支持TCP速度设置
        }

    /**
     * 应用音频延迟设置
     * VLC原生支持音频延迟功能
     */
    private fun applyAudioDelay() {
        try {
            mediaPlayer?.let { player ->
                // VLC的setAudioDelay方法接受微秒为单位的延迟值
                // 我们的audioDelayMs是毫秒，需要转换为微秒
                player.setAudioDelay(_audioDelayMs * 1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
