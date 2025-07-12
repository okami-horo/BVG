package dev.aaa1115910.bv.player.impl.exo

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/**
 * 音频延迟处理器
 * 用于实现音画同步功能，通过延迟音频来调整音画同步
 */
@UnstableApi
class AudioDelayProcessor : AudioProcessor {
    
    private var delayMs: Long = 0L
    private var sampleRate: Int = 0
    private var channelCount: Int = 0
    private var encoding: Int = 0
    private var bytesPerFrame: Int = 0
    
    // 延迟缓冲区
    private var delayBuffer: ByteArray? = null
    private var delayBufferSize: Int = 0
    private var delayBufferPosition: Int = 0
    private var delayBufferFilled: Boolean = false
    
    // 输出缓冲区
    private var outputBuffer: ByteBuffer? = null
    private var inputEnded: Boolean = false
    
    /**
     * 设置音频延迟时间
     * @param delayMs 延迟时间，单位毫秒
     */
    fun setAudioDelay(delayMs: Long) {
        this.delayMs = max(0, delayMs)
        initializeDelayBuffer()
    }
    
    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        encoding = inputAudioFormat.encoding
        
        // 计算每帧字节数
        bytesPerFrame = when (encoding) {
            androidx.media3.common.C.ENCODING_PCM_16BIT -> channelCount * 2
            androidx.media3.common.C.ENCODING_PCM_24BIT -> channelCount * 3
            androidx.media3.common.C.ENCODING_PCM_32BIT -> channelCount * 4
            androidx.media3.common.C.ENCODING_PCM_FLOAT -> channelCount * 4
            else -> channelCount * 2 // 默认16位
        }
        
        initializeDelayBuffer()
        
        return inputAudioFormat
    }
    
    private fun initializeDelayBuffer() {
        if (sampleRate > 0 && bytesPerFrame > 0 && delayMs > 0) {
            // 计算延迟缓冲区大小
            val delaySamples = (delayMs * sampleRate / 1000).toInt()
            delayBufferSize = delaySamples * bytesPerFrame
            delayBuffer = ByteArray(delayBufferSize)
            delayBufferPosition = 0
            delayBufferFilled = false
        } else {
            delayBuffer = null
            delayBufferSize = 0
        }
    }
    
    override fun isActive(): Boolean {
        return delayMs > 0 && delayBuffer != null
    }
    
    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive()) {
            // 如果没有延迟，直接输出
            outputBuffer = inputBuffer
            return
        }
        
        val inputSize = inputBuffer.remaining()
        if (inputSize == 0) return
        
        val inputData = ByteArray(inputSize)
        inputBuffer.get(inputData)
        
        if (delayBuffer == null) {
            // 如果延迟缓冲区未初始化，直接输出
            outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
            outputBuffer?.put(inputData)
            outputBuffer?.flip()
            return
        }
        
        val outputData = ByteArray(inputSize)
        
        for (i in inputData.indices) {
            if (delayBufferFilled) {
                // 从延迟缓冲区读取数据到输出
                outputData[i] = delayBuffer!![delayBufferPosition]
            } else {
                // 延迟缓冲区还未填满，输出静音
                outputData[i] = 0
            }
            
            // 将新数据写入延迟缓冲区
            delayBuffer!![delayBufferPosition] = inputData[i]
            delayBufferPosition++
            
            if (delayBufferPosition >= delayBufferSize) {
                delayBufferPosition = 0
                delayBufferFilled = true
            }
        }
        
        // 创建输出缓冲区
        outputBuffer = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder())
        outputBuffer?.put(outputData)
        outputBuffer?.flip()
    }
    
    override fun getOutput(): ByteBuffer {
        val result = outputBuffer ?: ByteBuffer.allocate(0)
        outputBuffer = null
        return result
    }
    
    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer == null
    }
    
    override fun flush() {
        outputBuffer = null
        delayBufferPosition = 0
        delayBufferFilled = false
        delayBuffer?.fill(0)
    }
    
    override fun reset() {
        flush()
        delayBuffer = null
        delayBufferSize = 0
        sampleRate = 0
        channelCount = 0
        encoding = 0
        bytesPerFrame = 0
        inputEnded = false
    }
    
    override fun queueEndOfStream() {
        inputEnded = true
    }
}
