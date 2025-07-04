package dev.aaa1115910.bv.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.component.HomeTopNavItem
import dev.aaa1115910.bv.component.TopNav
import dev.aaa1115910.bv.screen.main.home.DynamicsScreen
import dev.aaa1115910.bv.screen.main.home.PopularScreen
import dev.aaa1115910.bv.screen.main.home.RecommendScreen
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    recommendViewModel: RecommendViewModel = koinViewModel(),
    popularViewModel: PopularViewModel = koinViewModel(),
    dynamicViewModel: DynamicViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("HomeContent")

    val recommendState = rememberLazyListState()
    val popularState = rememberLazyListState()
    val dynamicState = rememberLazyListState()

    var selectedTab by remember { mutableStateOf(HomeTopNavItem.Recommend) }
    var focusOnContent by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    val currentListOnTop by remember {
        derivedStateOf {
            with(
                when (selectedTab) {
                    HomeTopNavItem.Recommend -> recommendState
                    HomeTopNavItem.Popular -> popularState
                    HomeTopNavItem.Dynamics -> dynamicState
                }
            ) {
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
    }

    //启动时刷新数据
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            recommendViewModel.loadMore()
        }
        scope.launch(Dispatchers.IO) {
            userViewModel.updateUserInfo()
        }
    }

    //监听登录变化
    LaunchedEffect(userViewModel.isLogin) {
        if (userViewModel.isLogin) {
            //login
            userViewModel.updateUserInfo()
        } else {
            //logout
            userViewModel.clearUserInfo()
        }
    }

    LaunchedEffect(hasFocus) {
        if (hasFocus) {
            runCatching {
                navFocusRequester.requestFocus()
            }.getOrThrow()
        }
    }

    BackHandler(focusOnContent) {
        logger.fInfo { "onFocusBackToNav" }
        runCatching {
            navFocusRequester.requestFocus(scope)
        }.getOrThrow()
        // scroll to top
        scope.launch(Dispatchers.Main) {
            when (selectedTab) {
                HomeTopNavItem.Recommend -> recommendState.animateScrollToItem(0)
                HomeTopNavItem.Popular -> popularState.animateScrollToItem(0)
                HomeTopNavItem.Dynamics -> dynamicState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .onFocusChanged { hasFocus = it.hasFocus },
        topBar = {
            TopNav(
                modifier = Modifier
                    .focusRequester(navFocusRequester)
                    .padding(end = 80.dp),
                items = HomeTopNavItem.entries,
                isLargePadding = !focusOnContent && currentListOnTop,
                onSelectedChanged = { nav ->
                    selectedTab = nav as HomeTopNavItem
                    when (nav) {
                        HomeTopNavItem.Recommend -> {}
                        HomeTopNavItem.Popular -> {
                            if (popularViewModel.popularVideoList.isEmpty()) {
                                scope.launch(Dispatchers.IO) { popularViewModel.loadMore() }
                            }
                        }

                        HomeTopNavItem.Dynamics -> {
                            if (dynamicViewModel.isLogin && dynamicViewModel.dynamicList.isEmpty()) {
                                scope.launch(Dispatchers.IO) { dynamicViewModel.loadMore() }
                            }
                        }
                    }
                },
                onClick = { nav ->
                    when (nav) {
                        HomeTopNavItem.Recommend -> {
                            logger.fInfo { "clear recommend data" }
                            recommendViewModel.clearData()
                            logger.fInfo { "reload recommend data" }
                            scope.launch(Dispatchers.IO) { recommendViewModel.loadMore() }
                        }

                        HomeTopNavItem.Popular -> {
                            logger.fInfo { "clear popular data" }
                            popularViewModel.clearData()
                            logger.fInfo { "reload popular data" }
                            scope.launch(Dispatchers.IO) { popularViewModel.loadMore() }
                        }

                        HomeTopNavItem.Dynamics -> {
                            dynamicViewModel.clearData()
                            scope.launch(Dispatchers.IO) { dynamicViewModel.loadMore() }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .onFocusChanged { focusOnContent = it.hasFocus }
        ) {
            when (selectedTab) {
                HomeTopNavItem.Recommend -> RecommendScreen(
                    lazyListState = recommendState,
                    recommendViewModel = recommendViewModel
                )

                HomeTopNavItem.Popular -> PopularScreen(
                    lazyListState = popularState,
                    popularViewModel = popularViewModel
                )

                HomeTopNavItem.Dynamics -> DynamicsScreen(
                    lazyListState = dynamicState,
                    dynamicViewModel = dynamicViewModel
                )
            }
        }
    }
}
