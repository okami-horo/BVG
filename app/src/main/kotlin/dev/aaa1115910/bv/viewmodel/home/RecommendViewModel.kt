package dev.aaa1115910.bv.viewmodel.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.home.RecommendPage
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.repositories.RecommendVideoRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fError
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecommendViewModel(
    private val recommendVideoRepository: RecommendVideoRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger {}
    var loading by mutableStateOf(false)
    var refreshing by mutableStateOf(false)
    var isEnd by mutableStateOf(false)

    val recommendVideoList = mutableStateListOf<UgcItem>()
    private var nextPage = RecommendPage()
    private var preloadedData = mutableListOf<UgcItem>() // 预加载的数据

    suspend fun loadMore() {
        var loadCount = 0
        val maxLoadMoreCount = 3
        if (!loading) {
            if (recommendVideoList.size == 0) {
                // first load data
                while (recommendVideoList.size < 14 && loadCount < maxLoadMoreCount) {
                    loadData()
                    if (loadCount != 0) logger.fInfo { "Load more recommend videos because items too less" }
                    loadCount++
                }
            } else {
                loadData()
            }
            
            // 加载完当前页后，预加载下一页数据
            preloadNextPage()
        }
    }

    private suspend fun loadData() {
        loading = true
        logger.fInfo { "Load more recommend videos" }
        runCatching {
            // 如果有预加载的数据，直接使用
            if (preloadedData.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    recommendVideoList.addAll(preloadedData)
                    preloadedData.clear()
                }
                logger.fInfo { "Using preloaded data" }
            } else {
                // 没有预加载数据，正常加载
                val recommendData = recommendVideoRepository.getRecommendVideos(
                    page = nextPage,
                    preferApiType = Prefs.apiType
                )
                nextPage = recommendData.nextPage
                recommendVideoList.addAllWithMainContext(recommendData.items)
            }
        }.onSuccess {
            // 成功处理已经在前面完成
        }.onFailure {
            logger.fError { "Load recommend video list failed: ${it.message ?: "Unknown error"}" }
            withContext(Dispatchers.Main) {
                "加载推荐视频失败: ${it.localizedMessage}".toast(BVApp.context)
            }
        }
        loading = false
    }

    // 预加载下一页数据
    private fun preloadNextPage() {
        if (isEnd) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                logger.fInfo { "Preloading next page data" }
                val recommendData = recommendVideoRepository.getRecommendVideos(
                    page = nextPage,
                    preferApiType = Prefs.apiType
                )
                
                if (recommendData.items.isEmpty()) {
                    isEnd = true
                    logger.fInfo { "Reached end of recommend list" }
                } else {
                    preloadedData.clear() // 确保不会累积过多预加载数据
                    preloadedData.addAll(recommendData.items)
                    // 保存下一页的页码，但不立即更新nextPage，等到真正加载时再更新
                    logger.fInfo { "Preloaded ${recommendData.items.size} items" }
                }
            }.onFailure {
                logger.fError { "Preload next page failed: ${it.message ?: "Unknown error"}" }
            }
        }
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                logger.fInfo { "Refreshing recommend videos" }
                // 刷新时使用新的RecommendPage
                val recommendData = recommendVideoRepository.getRecommendVideos(
                    page = RecommendPage(),
                    preferApiType = Prefs.apiType
                )
                
                withContext(Dispatchers.Main) {
                    recommendVideoList.clear()
                    recommendVideoList.addAll(recommendData.items)
                    nextPage = recommendData.nextPage
                    isEnd = false
                }
                
                // 刷新后预加载下一页
                preloadNextPage()
            }.onFailure {
                logger.fError { "Refresh recommend videos failed: ${it.message ?: "Unknown error"}" }
                withContext(Dispatchers.Main) {
                    "刷新推荐视频失败: ${it.localizedMessage}".toast(BVApp.context)
                }
            }
            refreshing = false
        }
    }

    fun clearData() {
        recommendVideoList.clear()
        nextPage = RecommendPage()
        preloadedData.clear() // 清除预加载数据
        loading = false
        isEnd = false
    }
}