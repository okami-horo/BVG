package dev.aaa1115910.bv.player.impl.exo

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.video.VideoRendererEventListener
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.OkHttpUtil
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.formatMinSec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.Handler

@OptIn(UnstableApi::class)
class ExoMediaPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer(), Player.Listener {
    var mPlayer: ExoPlayer? = null
    protected var mMediaSource: MediaSource? = null
    
    // 保存当前播放的URL以便重试
    private var currentVideoUrl: String? = null
    private var currentAudioUrl: String? = null
    
    // 重试相关属性
    private var retryCount = 0
    private val maxRetryCount = 3
    private val retryDelayMs = 500L // 重试间隔时间，单位ms

    @OptIn(UnstableApi::class)
    private val dataSourceFactory =
        OkHttpDataSource.Factory(OkHttpUtil.generateCustomSslOkHttpClient(context)).apply {
            options.userAgent?.let { setUserAgent(it) }
            options.referer?.let { setDefaultRequestProperties(mapOf("referer" to it)) }
        }

    init {
        initPlayer()
    }

    @OptIn(UnstableApi::class)
    override fun initPlayer() {
        val renderersFactory =
            object : DefaultRenderersFactory(context) {
                override fun buildVideoRenderers(
                    context: Context,
                    extensionRendererMode: Int,
                    mediaCodecSelector: MediaCodecSelector,
                    enableDecoderFallback: Boolean,
                    handler: Handler,
                    eventListener: VideoRendererEventListener,
                    maxVideoFrameDurationMs: Long,
                    out: ArrayList<Renderer>
                ) {
                    super.buildVideoRenderers(
                        context,
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF,
                        mediaCodecSelector,
                        enableDecoderFallback,
                        handler,
                        eventListener,
                        maxVideoFrameDurationMs,
                        out
                    )
                }
            }.apply {
                setExtensionRendererMode(
                    when (options.enableFfmpegAudioRenderer) {
                        true -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                        false -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    }
                )
            }
        mPlayer = ExoPlayer
            .Builder(context)
            .setRenderersFactory(renderersFactory)
            .setSeekForwardIncrementMs(1000 * 10)
            .setSeekBackIncrementMs(1000 * 5)
            // 设置音视频同步参数
            // .setAudioOffloadEnabled(false) // 禁用音频卸载，避免可能的同步问题 - 已移除，不兼容当前版本
            .setHandleAudioBecomingNoisy(true) // 处理音频中断
            .build()

        // 设置音视频同步参数
        mPlayer?.setSkipSilenceEnabled(false) // 禁用跳过静音，以保持音频连续性
        mPlayer?.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT) // 设置视频缩放模式

        initListener()
    }

    private fun initListener() {
        mPlayer?.addListener(this)
    }

    @OptIn(UnstableApi::class)
    override fun setHeader(headers: Map<String, String>) {

    }

    @OptIn(UnstableApi::class)
    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        // 保存当前URL以便重试
        currentVideoUrl = videoUrl
        currentAudioUrl = audioUrl
        
        // 重置重试计数
        retryCount = 0
        
        val videoMediaSource = videoUrl?.let {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(it))
        }
        val audioMediaSource = audioUrl?.let {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(it))
        }

        val mediaSources = listOfNotNull(videoMediaSource, audioMediaSource)
        // MergingMediaSource 的 isAtomic 参数可以确保音视频流同步进行各种操作，有助于保持同步
        mMediaSource = MergingMediaSource(
            /* isAtomic= */ true,
            *mediaSources.toTypedArray()
        )
    }

    @OptIn(UnstableApi::class)
    override fun prepare() {
        mPlayer?.setMediaSource(mMediaSource!!)
        mPlayer?.prepare()
    }

    override fun start() {
        mPlayer?.play()
    }

    override fun pause() {
        mPlayer?.pause()
    }

    override fun stop() {
        mPlayer?.stop()
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override val isPlaying: Boolean
        get() = mPlayer?.isPlaying == true

    override fun seekTo(time: Long) {
        mPlayer?.seekTo(time)
    }

    override fun release() {
        mPlayer?.release()
    }

    override val currentPosition: Long
        get() = mPlayer?.currentPosition ?: 0
    override val duration: Long
        get() = mPlayer?.duration ?: 0
    override val bufferedPercentage: Int
        get() = mPlayer?.bufferedPercentage ?: 0

    override fun setOptions() {
        mPlayer?.playWhenReady = true
    }

    override var speed: Float
        get() = mPlayer?.playbackParameters?.speed ?: 1f
        set(value) {
            mPlayer?.setPlaybackSpeed(value)
        }
    override val tcpSpeed: Long
        get() = 0L

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> {}
            Player.STATE_BUFFERING -> mPlayerEventListener?.onBuffering()
            Player.STATE_READY -> mPlayerEventListener?.onReady()
            Player.STATE_ENDED -> mPlayerEventListener?.onEnd()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            mPlayerEventListener?.onPlay()
        } else {
            mPlayerEventListener?.onPause()
        }
    }

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        mPlayerEventListener?.onSeekBack(seekBackIncrementMs)
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        mPlayerEventListener?.onSeekForward(seekForwardIncrementMs)
    }

    override val debugInfo: String
        get() {
            return """
                player: ${androidx.media3.common.MediaLibraryInfo.VERSION_SLASHY}
                time: ${currentPosition.formatMinSec()} / ${duration.formatMinSec()}
                buffered: $bufferedPercentage%
                resolution: ${mPlayer?.videoSize?.width} x ${mPlayer?.videoSize?.height}
                audio: ${mPlayer?.audioFormat?.bitrate ?: 0} kbps
                video codec: ${mPlayer?.videoFormat?.sampleMimeType ?: "null"}
                audio codec: ${mPlayer?.audioFormat?.sampleMimeType ?: "null"} (${getAudioRendererName()})
            """.trimIndent().also {
                println(mPlayer?.audioFormat)
            }
        }

    private fun getAudioRendererName(): String {
        val rendererCount = mPlayer?.rendererCount ?: return "UnknownRenderer"
        for (i in 0 until rendererCount) {
            val renderer = mPlayer!!.getRenderer(i)
            if (renderer.trackType == C.TRACK_TYPE_AUDIO && renderer.state == Renderer.STATE_STARTED) {
                return renderer.name
            }
        }
        return "UnknownRenderer"
    }

    override val videoWidth: Int
        get() = mPlayer?.videoSize?.width ?: 0
    override val videoHeight: Int
        get() = mPlayer?.videoSize?.height ?: 0

    private fun findCause(throwable: Throwable): Throwable {
        var cause: Throwable? = throwable
        while (cause?.cause != null) {
            cause = cause.cause
        }
        return cause ?: throwable
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        // 获取错误发生时的播放位置
        val lastPosition = currentPosition

        // 检查是否是 403 错误
        val cause = findCause(error)
        if (cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 403) {
            if (retryCount < maxRetryCount) {
                retryCount++
                CoroutineScope(Dispatchers.Main).launch {
                    delay(retryDelayMs)
                    // 对于 403 错误，尝试 seek
                    mPlayer?.seekTo(lastPosition)
                    mPlayer?.prepare()
                    mPlayer?.play()
                    mPlayerEventListener?.onBuffering()
                }
            } else {
                // 达到最大重试次数，通知错误
                mPlayerEventListener?.onError(error)
            }
            return
        }

        // 对于其他错误，使用通用重试逻辑
        if (retryCount < maxRetryCount) {
            retryCount++
            CoroutineScope(Dispatchers.Main).launch {
                delay(retryDelayMs)
                // 重新创建媒体源
                val videoMediaSource = currentVideoUrl?.let {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(it))
                }
                val audioMediaSource = currentAudioUrl?.let {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(it))
                }

                val mediaSources = listOfNotNull(videoMediaSource, audioMediaSource)
                mMediaSource = MergingMediaSource(
                    /* isAtomic= */ true,
                    *mediaSources.toTypedArray()
                )

                // 重新设置媒体源，并直接跳转到记录的位置
                mPlayer?.setMediaSource(mMediaSource!!, lastPosition)
                mPlayer?.prepare()
                mPlayer?.play()

                // 通知UI正在缓冲，UI上会显示“缓冲中”
                mPlayerEventListener?.onBuffering()
            }
        } else {
            // 达到最大重试次数，通知错误
            mPlayerEventListener?.onError(error)
        }
    }
}
