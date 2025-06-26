package dev.aaa1115910.bv.viewmodel.user

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.HistoryRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.formatMinSec
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(
    private val userRepository: UserRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var histories = mutableStateListOf<VideoCardData>()
    var noMore by mutableStateOf(false)
    
    // 加载状态指示器
    var isLoading by mutableStateOf(false)
        private set
    
    private var cursor = 0L
    private var updating = false
    
    // 存储当前更新任务的Job引用
    private var updateJob: Job? = null

    fun update() {
        // 如果已经在更新或无更多数据，则不再触发更新
        if (updating || noMore) return
        
        // 取消正在进行的更新任务
        updateJob?.cancel()
        
        // 启动新的更新任务
        updateJob = viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            updateHistories()
            isLoading = false
        }
    }
    
    // 重置并刷新数据
    fun resetAndUpdate() {
        // 取消所有正在进行的更新操作
        updateJob?.cancel()
        
        viewModelScope.launch {
            // 在UI线程清空数据，确保UI立即更新
            withContext(Dispatchers.Main) {
                // 清空数据
                histories.clear()
                // 重置状态
                noMore = false
                isLoading = false
            }
            
            // 重置分页参数
            cursor = 0L
            updating = false
            
            // 使用IO线程重新加载数据
            withContext(Dispatchers.IO) {
                isLoading = true
                updateHistories()
                isLoading = false
            }
        }
    }

    private suspend fun updateHistories(context: Context = BVApp.context) {
        if (updating || noMore) return
        logger.fInfo { "Updating histories with params [cursor=$cursor, apiType=${Prefs.apiType}]" }
        updating = true
        
        // 创建临时列表收集新数据，避免频繁更新UI状态
        val tempList = mutableListOf<VideoCardData>()
        
        runCatching {
            val data = historyRepository.getHistories(
                cursor = cursor,
                preferApiType = Prefs.apiType
            )

            // 在IO线程中处理数据转换
            data.data.forEach { historyItem ->
                tempList.add(
                    VideoCardData(
                        avid = historyItem.oid,
                        title = historyItem.title,
                        cover = historyItem.cover,
                        upName = historyItem.author,
                        timeString = if (historyItem.progress == -1) context.getString(R.string.play_time_finish)
                        else context.getString(
                            R.string.play_time_history,
                            (historyItem.progress * 1000L).formatMinSec(),
                            (historyItem.duration * 1000L).formatMinSec()
                        )
                    )
                )
            }
            
            // 批量更新UI，减少UI线程操作次数
            withContext(Dispatchers.Main) {
                histories.addAll(tempList)
                
                //update cursor
                cursor = data.cursor
                if (cursor == 0L) {
                    noMore = true
                    logger.fInfo { "No more history" }
                }
            }
            
            logger.fInfo { "Update history cursor: [cursor=$cursor]" }
            logger.fInfo { "Update histories success, added ${tempList.size} items" }
            
        }.onFailure {
            logger.fWarn { "Update histories failed: ${it.message ?: "Unknown error"}" }
            when (it) {
                is AuthFailureException -> {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.exception_auth_failure)
                            .toast(BVApp.context)
                    }
                    logger.fInfo { "User auth failure" }
                    if (!BuildConfig.DEBUG) userRepository.logout()
                }

                else -> {}
            }
        }
        updating = false
    }
}