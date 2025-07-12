package dev.aaa1115910.bv.player.impl.vlc

import android.content.Context
import dev.aaa1115910.bv.player.VideoPlayerOptions
import dev.aaa1115910.bv.player.VideoPlayerListener
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * VLC播放器单元测试
 * 
 * 注意：这些测试主要验证播放器的基本逻辑，
 * 实际的VLC功能需要在Android设备上进行集成测试
 */
class VlcMediaPlayerTest {

    private lateinit var context: Context
    private lateinit var options: VideoPlayerOptions
    private lateinit var vlcPlayer: VlcMediaPlayer
    private lateinit var mockListener: VideoPlayerListener

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        options = VideoPlayerOptions(
            userAgent = "TestUserAgent",
            referer = "https://test.com",
            enableFfmpegAudioRenderer = false,
            audioDelayMs = 100L
        )
        mockListener = mockk(relaxed = true)
        vlcPlayer = VlcMediaPlayer(context, options)
        vlcPlayer.setPlayerEventListener(mockListener)
    }

    @Test
    fun testInitialState() {
        // 测试初始状态
        assertFalse("播放器初始状态应该是未播放", vlcPlayer.isPlaying)
        assertEquals("初始位置应该是0", 0L, vlcPlayer.currentPosition)
        assertEquals("初始时长应该是0", 0L, vlcPlayer.duration)
        assertEquals("初始缓冲百分比应该是0", 0, vlcPlayer.bufferedPercentage)
        assertEquals("初始播放速度应该是1.0", 1.0f, vlcPlayer.speed)
        assertEquals("初始音频延迟应该是配置值", 100L, vlcPlayer.audioDelayMs)
    }

    @Test
    fun testAudioDelayConfiguration() {
        // 测试音频延迟配置
        val newDelay = 200L
        vlcPlayer.audioDelayMs = newDelay
        assertEquals("音频延迟应该被正确设置", newDelay, vlcPlayer.audioDelayMs)
    }

    @Test
    fun testPlaybackSpeedConfiguration() {
        // 测试播放速度配置
        val newSpeed = 1.5f
        vlcPlayer.speed = newSpeed
        assertEquals("播放速度应该被正确设置", newSpeed, vlcPlayer.speed, 0.01f)
    }

    @Test
    fun testVideoPlayerOptions() {
        // 测试播放器选项
        assertEquals("用户代理应该匹配", "TestUserAgent", options.userAgent)
        assertEquals("Referer应该匹配", "https://test.com", options.referer)
        assertEquals("音频延迟应该匹配", 100L, options.audioDelayMs)
        assertFalse("FFmpeg音频渲染器应该被禁用", options.enableFfmpegAudioRenderer)
    }

    @Test
    fun testPlayerInstanceId() {
        // 测试播放器实例ID
        val initialId = vlcPlayer.playerInstanceId
        
        // 初始化播放器应该增加实例ID
        vlcPlayer.initPlayer()
        assertTrue("播放器实例ID应该增加", vlcPlayer.playerInstanceId > initialId)
    }

    @Test
    fun testDebugInfo() {
        // 测试调试信息
        val debugInfo = vlcPlayer.debugInfo
        assertTrue("调试信息应该包含播放器类型", debugInfo.contains("VLC"))
        assertTrue("调试信息应该包含时间信息", debugInfo.contains("time:"))
        assertTrue("调试信息应该包含缓冲信息", debugInfo.contains("buffered:"))
        assertTrue("调试信息应该包含播放速度", debugInfo.contains("rate:"))
        assertTrue("调试信息应该包含音频延迟", debugInfo.contains("audio delay:"))
    }

    @Test
    fun testVideoSizeInitialValues() {
        // 测试视频尺寸初始值
        assertEquals("初始视频宽度应该是0", 0, vlcPlayer.videoWidth)
        assertEquals("初始视频高度应该是0", 0, vlcPlayer.videoHeight)
    }

    @Test
    fun testTcpSpeedProperty() {
        // 测试TCP速度属性
        assertEquals("TCP速度初始值应该是0", 0L, vlcPlayer.tcpSpeed)
        
        // 设置TCP速度（VLC不支持，但不应该抛出异常）
        vlcPlayer.tcpSpeed = 1000L
        // VLC不支持TCP速度设置，所以值应该保持0
        assertEquals("TCP速度应该保持0", 0L, vlcPlayer.tcpSpeed)
    }
}
