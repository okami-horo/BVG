package dev.aaa1115910.bv.player.impl.exo

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.OkHttpUtil
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.formatMinSec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.net.URI
import java.util.concurrent.Executors

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
    private var currentUseDashMode: Boolean = false
    
    // 重试相关属性
    private var retryCount = 0
    private val maxRetryCount = 3
    private val retryDelayMs = 500L // 重试间隔时间，单位ms
    
    // 缓存相关
    private val cacheSize = 500 * 1024 * 1024L // 500MB
    private val cacheDirectory = File(context.cacheDir, "media_cache")
    private var cache: Cache? = null
    private val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
    private val databaseProvider = StandaloneDatabaseProvider(context)
    private val cacheExecutor = Executors.newSingleThreadExecutor()

    @OptIn(UnstableApi::class)
    private val okHttpDataSourceFactory =
        OkHttpDataSource.Factory(OkHttpUtil.generateCustomSslOkHttpClient(context)).apply {
            options.userAgent?.let { setUserAgent(it) }
            options.referer?.let { setDefaultRequestProperties(mapOf("referer" to it)) }
        }
    
    @OptIn(UnstableApi::class)
    private val dataSourceFactory: DataSource.Factory by lazy {
        // 初始化缓存
        if (cache == null && options.enableCache) {
            cache = SimpleCache(cacheDirectory, cacheEvictor, databaseProvider)
        }
        
        // 创建带缓存的数据源工厂
        if (options.enableCache && cache != null) {
            CacheDataSource.Factory()
                .setCache(cache!!)
                .setUpstreamDataSourceFactory(
                    DefaultDataSource.Factory(context, okHttpDataSourceFactory)
                )
                .setCacheWriteDataSinkFactory(null) // 暂时禁用写入缓存，只读取缓存
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .setEventListener(object : CacheDataSource.EventListener {
                    override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {
                        // 可以在这里监控缓存读取情况
                    }
                    
                    override fun onCacheIgnored(reason: Int) {
                        // 缓存被忽略的原因
                    }
                })
        } else {
            // 不使用缓存时，直接使用OkHttp数据源
            DefaultDataSource.Factory(context, okHttpDataSourceFactory)
        }
    }

    init {
        initPlayer()
    }

    @OptIn(UnstableApi::class)
    override fun initPlayer() {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(
                when (options.enableFfmpegAudioRenderer) {
                    true -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                    false -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                }
            )
        }
        
        // 创建自定义LoadControl以优化缓冲策略
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS * 2, // 增加最小缓冲区大小
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * 2, // 增加最大缓冲区大小
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true) // 优先考虑时间而不是大小阈值
            .build()
            
        val builder = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setSeekForwardIncrementMs(1000 * 10)
            .setSeekBackIncrementMs(1000 * 5)
            
        // 创建播放器
        mPlayer = builder.build()

        // 设置音视频同步参数
        mPlayer?.setSkipSilenceEnabled(false) // 禁用跳过静音，以保持音频连续性
        mPlayer?.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT) // 设置视频缩放模式
        
        // 设置最大缓冲区大小
        mPlayer?.setVideoSurfaceView(null)

        initListener()
    }

    private fun initListener() {
        mPlayer?.addListener(this)
    }

    @OptIn(UnstableApi::class)
    override fun setHeader(headers: Map<String, String>) {
        // 设置请求头
        okHttpDataSourceFactory.setDefaultRequestProperties(headers)
    }

    @OptIn(UnstableApi::class)
    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        // 保存当前URL以便重试
        currentVideoUrl = videoUrl
        currentAudioUrl = audioUrl
        currentUseDashMode = false
        
        // 重置重试计数
        retryCount = 0
        
        // 尝试检测是否为DASH格式
        if (videoUrl != null && videoUrl.endsWith(".mpd")) {
            // 使用DASH格式处理
            currentUseDashMode = true
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .build()
            
            mMediaSource = DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        } else {
            // 使用传统的分离音视频流方式
            val videoMediaSource = videoUrl?.let {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(it))
            }
            val audioMediaSource = audioUrl?.let {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(it))
            }

            val mediaSources = listOfNotNull(videoMediaSource, audioMediaSource)
            // 使用标准的MergingMediaSource构造方法
            if (mediaSources.size == 2) {
                mMediaSource = MergingMediaSource(mediaSources[0], mediaSources[1])
            } else if (mediaSources.isNotEmpty()) {
                mMediaSource = mediaSources[0]
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun prepare() {
        mMediaSource?.let {
            mPlayer?.setMediaSource(it)
            mPlayer?.prepare()
        }
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
        mPlayer?.stop()
        mPlayer?.clearMediaItems()
    }

    override val isPlaying: Boolean
        get() = mPlayer?.isPlaying == true

    override fun seekTo(time: Long) {
        mPlayer?.seekTo(time)
    }

    override fun release() {
        mPlayer?.release()
        mPlayer = null
        
        // 释放缓存资源
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cache?.release()
                cache = null
            } catch (e: Exception) {
                // 忽略释放缓存时的错误
            }
        }
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
                mode: ${if (currentUseDashMode) "DASH" else "MergingMediaSource"}
                A/V sync offset: ${getAudioVideoSyncOffset()} ms
                cache: ${if (options.enableCache) "启用" else "禁用"}
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
    
    private fun getAudioVideoSyncOffset(): Long {
        // 这个方法尝试估算音视频同步偏移量，实际上ExoPlayer内部已经处理了同步
        // 这里主要用于调试目的
        return 0
    }

    override val videoWidth: Int
        get() = mPlayer?.videoSize?.width ?: 0
    override val videoHeight: Int
        get() = mPlayer?.videoSize?.height ?: 0

    override fun onPlayerError(error: PlaybackException) {
        // 检查是否是HTTP相关错误
        val shouldRetry = when (error.errorCode) {
            // 处理HTTP错误，包括403 Forbidden
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                // 检查HTTP状态码是否为403
                val isHttp403 = error.message?.contains("403") == true
                isHttp403 || retryCount < maxRetryCount
            }
            else -> false
        }

        if (shouldRetry && retryCount < maxRetryCount) {
            retryCount++
            val position = currentPosition
            retryPlayback(position)
        } else {
            // 重试失败或不需要重试时，通知错误
            mPlayerEventListener?.onError(error)
        }
    }
    
    /**
     * 静默重试播放，不通知用户
     * @param position 重试时要恢复的播放位置
     */
    private fun retryPlayback(position: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(retryDelayMs)
            playUrl(currentVideoUrl, currentAudioUrl)
            prepare()
            seekTo(position)
            start()
        }
    }
    
    /**
     * 创建DASH MPD文件用于测试
     * 此方法仅用于开发测试，不应在生产环境中使用
     */
    private fun createDashMpd(videoUrl: String, audioUrl: String): String {
        val mpdContent = """
            <?xml version="1.0"?>
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" profiles="urn:mpeg:dash:profile:full:2011" minBufferTime="PT1.5S">
                <Period>
                    <AdaptationSet mimeType="video/mp4">
                        <Representation id="video" bandwidth="3200000">
                            <BaseURL>$videoUrl</BaseURL>
                            <SegmentBase>
                                <Initialization range="0-0"/>
                            </SegmentBase>
                        </Representation>
                    </AdaptationSet>
                    <AdaptationSet mimeType="audio/mp4">
                        <Representation id="audio" bandwidth="128000">
                            <BaseURL>$audioUrl</BaseURL>
                            <SegmentBase>
                                <Initialization range="0-0"/>
                            </SegmentBase>
                        </Representation>
                    </AdaptationSet>
                </Period>
            </MPD>
        """.trimIndent()
        
        val file = File(context.cacheDir, "temp_dash.mpd")
        FileWriter(file).use { it.write(mpdContent) }
        return Uri.fromFile(file).toString()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cache?.release()
                // 删除缓存文件夹
                if (cacheDirectory.exists()) {
                    cacheDirectory.deleteRecursively()
                }
                // 重新创建缓存
                cache = SimpleCache(cacheDirectory, cacheEvictor, databaseProvider)
            } catch (e: Exception) {
                // 处理清除缓存时的错误
            }
        }
    }
}
