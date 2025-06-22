package dev.aaa1115910.bv.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 安全的焦点管理器
 *
 * 用于处理焦点请求，确保只在UI准备好的情况下请求焦点
 * 实现了队列管理和基于UI状态的安全焦点请求
 */
class SafeFocusManager(private val scope: CoroutineScope) {
    private val logger = KotlinLogging.logger("SafeFocusManager")
    private var isUiReady = false
    private val pendingFocusRequests = mutableListOf<FocusRequester>()
    
    /**
     * 标记UI已准备好
     * 将处理之前所有等待的焦点请求
     */
    fun markUiReady() {
        isUiReady = true
        processPendingRequests()
    }
    
    /**
     * 安全地请求焦点
     * 如果UI已准备好，直接尝试请求焦点
     * 如果UI未准备好，将请求添加到队列中等待处理
     */
    fun requestFocus(focusRequester: FocusRequester) {
        if (isUiReady) {
            processFocusRequest(focusRequester)
        } else {
            logger.debug { "UI not ready, queuing focus request" }
            pendingFocusRequests.add(focusRequester)
        }
    }
    
    /**
     * 处理队列中的焦点请求
     */
    private fun processPendingRequests() {
        if (pendingFocusRequests.isEmpty()) return
        
        logger.debug { "Processing ${pendingFocusRequests.size} pending focus requests" }
        scope.launch(Dispatchers.Main) {
            // 只处理最后一个焦点请求，因为用户可能已经切换了多次页面
            val lastRequester = pendingFocusRequests.last()
            processFocusRequest(lastRequester)
            pendingFocusRequests.clear()
        }
    }
    
    /**
     * 执行焦点请求
     */
    private fun processFocusRequest(focusRequester: FocusRequester) {
        // 创建本地函数避免递归调用问题
        fun doRequestFocus() {
            scope.launch(Dispatchers.Main) {
                runCatching { 
                    focusRequester.requestFocus()
                    logger.debug { "Focus request successful" }
                }.onFailure { e ->
                    logger.debug(e) { "Focus request failed, will retry once" }
                    // 失败后等待100ms再次尝试
                    withContext(Dispatchers.Main) {
                        kotlinx.coroutines.delay(100)
                        runCatching {
                            focusRequester.requestFocus()
                            logger.debug { "Retry focus request successful" }
                        }.onFailure { retryError ->
                            logger.debug(retryError) { "Retry focus request failed" }
                        }
                    }
                }
            }
        }
        
        // 执行本地函数
        doRequestFocus()
    }
}

/**
 * Composable函数创建SafeFocusManager
 */
@Composable
fun rememberSafeFocusManager(scope: CoroutineScope): SafeFocusManager {
    return remember { SafeFocusManager(scope) }
}

/**
 * 监听UI准备状态的Composable
 */
@Composable
fun rememberUiReadyState(): MutableState<Boolean> {
    return remember { mutableStateOf(false) }
}

/**
 * FocusRequester的拓展函数，与原requestFocus方法签名兼容
 * 但内部使用SafeFocusManager处理焦点请求
 * 
 * 这个函数是为了方便迁移，让现有代码可以平滑过渡到新的焦点管理方式
 * 
 * @param scope 协程作用域
 * @param focusManager 可选参数，如果提供则使用提供的实例，否则创建新的SafeFocusManager
 */
fun FocusRequester.requestFocusWithManager(
    scope: CoroutineScope,
    focusManager: SafeFocusManager? = null
) {
    val manager = focusManager ?: SafeFocusManager(scope)
    manager.requestFocus(this)
}

/**
 * 在UI组件上添加onGloballyPositioned修饰符，当组件定位完成后自动请求焦点
 */
@Composable
fun Modifier.requestFocusOnPositioned(
    focusRequester: FocusRequester,
    scope: CoroutineScope
): Modifier {
    val manager = rememberSafeFocusManager(scope)
    return this.then(
        onGloballyPositioned {
            manager.requestFocus(focusRequester)
        }
    )
} 