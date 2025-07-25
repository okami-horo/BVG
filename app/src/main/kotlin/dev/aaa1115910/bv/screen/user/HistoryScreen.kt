package dev.aaa1115910.bv.screen.user

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.component.videocard.SmallVideoCard
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = koinViewModel(),
    navFocusRequester: FocusRequester = remember { FocusRequester() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("HistoryScreen")
    val gridState = rememberLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    
    var currentIndex by remember { mutableIntStateOf(0) }
    var focusOnContent by remember { mutableStateOf(false) }
    
    // 使用remember { derivedStateOf } 减少重组
    val showLargeTitle by remember { derivedStateOf { currentIndex < 4 } }
    val titleFontSize by animateFloatAsState(
        targetValue = if (showLargeTitle) 48f else 24f,
        label = "title font size"
    )
    
    // 使用remember { derivedStateOf } 计算预加载阈值，避免每次焦点变化都重新计算
    val shouldPreload by remember { derivedStateOf { 
        val threshold = historyViewModel.histories.size - 15 // 提前15项开始预加载
        currentIndex > threshold && !historyViewModel.noMore && historyViewModel.histories.isNotEmpty()
    }}
    
    // 监听预加载状态变化，而不是在每个item的onFocus中触发
    LaunchedEffect(shouldPreload) {
        if (shouldPreload) {
            historyViewModel.update()
        }
    }

    LaunchedEffect(Unit) {
        // 初始化加载，使用轻量级检查避免不必要的加载
        if (historyViewModel.histories.isEmpty()) {
            historyViewModel.update()
        }
    }
    
    // 监听历史记录数据变化，当数据加载完成后自动聚焦到第一个视频卡片
    LaunchedEffect(historyViewModel.histories.size) {
        if (historyViewModel.histories.isNotEmpty()) {
            // 短暂延迟确保UI已完全渲染，但不要延迟太久
            delay(100)
            runCatching {
                firstItemFocusRequester.requestFocus(scope)
            }.getOrElse {
                logger.fInfo { "Failed to request focus on first history item: ${it.message}" }
            }
        }
    }
    
    // 当历史记录数据被清空时，自动滚动到顶部
    LaunchedEffect(historyViewModel.histories.isEmpty()) {
        if (historyViewModel.histories.isEmpty()) {
            // 使用无动画滚动，减少性能开销
            gridState.scrollToItem(0)
        }
    }
    
    BackHandler(focusOnContent) {
        logger.fInfo { "onFocusBackToNav" }
        navFocusRequester.requestFocus(scope)
        // 使用不会阻塞UI线程的方式滚动到顶部
        scope.launch {
            gridState.scrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(
                modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 8.dp, end = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.user_homepage_recent),
                        fontSize = titleFontSize.sp
                    )
                    if (historyViewModel.noMore) {
                        Text(
                            text = stringResource(
                                R.string.load_data_count_no_more,
                                historyViewModel.histories.size
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            text = stringResource(
                                R.string.load_data_count,
                                historyViewModel.histories.size
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus },
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            itemsIndexed(
                items = historyViewModel.histories,
                // 添加key帮助Compose更有效地重用item
                key = { _, item -> item.avid }
            ) { index, history ->
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    SmallVideoCard(
                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                        data = history,
                        onClick = {
                            VideoInfoActivity.actionStart(
                                context = context,
                                aid = history.avid,
                                proxyArea = ProxyArea.checkProxyArea(history.title)
                            )
                        },
                        onFocus = {
                            currentIndex = index
                        }
                    )
                }
            }
        }
    }
}
