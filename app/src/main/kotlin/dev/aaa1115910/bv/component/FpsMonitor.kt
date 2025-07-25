package dev.aaa1115910.bv.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

@Composable
fun FpsMonitor(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var fpsCount by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(0) }
    var lastUpdate by remember { mutableLongStateOf(0L) }
    var isActive by remember { mutableIntStateOf(1) } // 使用Int而不是Boolean，避免重组

    // 使用更高效的方式监控FPS
    LaunchedEffect(Unit) {
        // 初始化时间
        lastUpdate = withFrameMillis { it }
        
        while (true) {
            delay(200) // 每200ms更新一次，减少计算频率
            
            if (isActive > 0) {
                withFrameMillis { currentMillis ->
                    val elapsedMillis = currentMillis - lastUpdate
                    if (elapsedMillis > 0) {
                        fps = ((fpsCount * 1000) / elapsedMillis).toInt()
                        fpsCount = 0
                        lastUpdate = currentMillis
                    }
                }
            }
        }
    }
    
    // 单独的Effect用于计数帧
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { _ ->
                if (isActive > 0) {
                    fpsCount++
                }
            }
        }
    }
    
    // 当组件不可见时停止计算
    DisposableEffect(Unit) {
        isActive = 1
        onDispose {
            isActive = 0
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        content()
        Text(
            modifier = modifier
                .size(60.dp)
                .align(Alignment.TopStart),
            text = "Fps: $fps",
            color = Color.Red,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}