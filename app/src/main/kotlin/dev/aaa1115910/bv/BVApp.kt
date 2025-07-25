package dev.aaa1115910.bv

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import de.schnettler.datastore.manager.DataStoreManager
import dev.aaa1115910.biliapi.http.BiliHttpProxyApi
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.biliapi.repositories.ChannelRepository
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import dev.aaa1115910.biliapi.repositories.HistoryRepository
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.biliapi.repositories.LoginRepository
import dev.aaa1115910.biliapi.repositories.PgcRepository
import dev.aaa1115910.biliapi.repositories.RecommendVideoRepository
import dev.aaa1115910.biliapi.repositories.SearchRepository
import dev.aaa1115910.biliapi.repositories.SeasonRepository
import dev.aaa1115910.biliapi.repositories.UgcRepository
import dev.aaa1115910.biliapi.repositories.VideoDetailRepository
import dev.aaa1115910.biliapi.repositories.VideoPlayRepository
import dev.aaa1115910.bv.dao.AppDatabase
import dev.aaa1115910.bv.entity.AuthData
import dev.aaa1115910.bv.entity.db.UserDB
import dev.aaa1115910.bv.network.HttpServer
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.screen.user.UserSwitchViewModel
import dev.aaa1115910.bv.util.AutoUpdateChecker
import dev.aaa1115910.bv.util.BuglyUtil
import dev.aaa1115910.bv.util.LogCatcherUtil
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.PlayerViewModel
import dev.aaa1115910.bv.viewmodel.TagViewModel
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import dev.aaa1115910.bv.viewmodel.index.PgcIndexViewModel
import dev.aaa1115910.bv.viewmodel.login.AppQrLoginViewModel
import dev.aaa1115910.bv.viewmodel.login.SmsLoginViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcAnimeViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcDocumentaryViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcGuoChuangViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcMovieViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcTvViewModel
import dev.aaa1115910.bv.viewmodel.pgc.PgcVarietyViewModel
import dev.aaa1115910.bv.viewmodel.search.SearchInputViewModel
import dev.aaa1115910.bv.viewmodel.search.SearchResultViewModel
import dev.aaa1115910.bv.viewmodel.user.FavoriteViewModel
import dev.aaa1115910.bv.viewmodel.user.FollowViewModel
import dev.aaa1115910.bv.viewmodel.user.FollowingSeasonViewModel
import dev.aaa1115910.bv.viewmodel.user.HistoryViewModel
import dev.aaa1115910.bv.viewmodel.user.ToViewViewModel
import dev.aaa1115910.bv.viewmodel.user.UpInfoViewModel
import dev.aaa1115910.bv.viewmodel.video.VideoDetailViewModel
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.slf4j.impl.HandroidLoggerAdapter

class BVApp : Application(), ImageLoaderFactory {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        lateinit var dataStoreManager: DataStoreManager
        lateinit var koinApplication: KoinApplication
        var instance: BVApp? = null

        fun getAppDatabase(context: Context = this.context) = AppDatabase.getDatabase(context)
    }

    override fun onCreate() {
        super.onCreate()
        context = this.applicationContext
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        dataStoreManager = DataStoreManager(applicationContext.dataStore)
        koinApplication = startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@BVApp)
            modules(appModule)
        }
        BuglyUtil.init(applicationContext)
        LogCatcherUtil.installLogCatcher()
        initRepository()
        initProxy()
        instance = this
        updateMigration()
        initAutoUpdateChecker()
        HttpServer.startServer()
    }

    // 实现ImageLoaderFactory接口，提供优化的Coil图片加载器配置
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                // 使用更少的内存，避免OOM
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 使用25%的可用内存
                    .build()
            }
            .diskCache {
                // 配置磁盘缓存
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05) // 使用5%的可用磁盘空间
                    .build()
            }
            .respectCacheHeaders(false) // 忽略服务器缓存控制头
            .crossfade(true) // 启用淡入淡出效果
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    fun initRepository() {
        val channelRepository by koinApplication.koin.inject<ChannelRepository>()
        channelRepository.initDefaultChannel(Prefs.accessToken, Prefs.buvid)

        val authRepository by koinApplication.koin.inject<AuthRepository>()
        authRepository.sessionData = Prefs.sessData.takeIf { it.isNotEmpty() }
        authRepository.biliJct = Prefs.biliJct.takeIf { it.isNotEmpty() }
        authRepository.accessToken = Prefs.accessToken.takeIf { it.isNotEmpty() }
        authRepository.mid = Prefs.uid.takeIf { it != 0L }
        authRepository.buvid3 = Prefs.buvid3
        authRepository.buvid = Prefs.buvid
    }

    fun initProxy() {
        if (Prefs.enableProxy) {
            BiliHttpProxyApi.createClient(Prefs.proxyHttpServer)

            val channelRepository by koinApplication.koin.inject<ChannelRepository>()
            runCatching {
                channelRepository.initProxyChannel(
                    Prefs.accessToken,
                    Prefs.buvid,
                    Prefs.proxyGRPCServer
                )
            }
        }
    }

    private fun updateMigration() {
        val lastVersionCode = Prefs.lastVersionCode
        if (lastVersionCode >= BuildConfig.VERSION_CODE) return
        Log.i("BVApp", "updateMigration from $lastVersionCode")
        if (lastVersionCode < 576) {
            // 从 Prefs 中读取登录数据写入 UserDB
            if (Prefs.isLogin) {
                runBlocking {
                    val existedUser = getAppDatabase().userDao().findUserByUid(Prefs.uid)
                    if (existedUser == null) {
                        val user = UserDB(
                            uid = Prefs.uid,
                            username = "Unknown",
                            avatar = "",
                            auth = AuthData.fromPrefs().toJson()
                        )
                        getAppDatabase().userDao().insert(user)
                    }
                }
            }
        }
        Prefs.lastVersionCode = BuildConfig.VERSION_CODE
    }

    private fun initAutoUpdateChecker() {
        // 清理旧的更新文件
        AutoUpdateChecker.cleanupOldUpdateFiles(applicationContext)

        // 在后台检查更新
        if (AutoUpdateChecker.shouldCheckUpdate()) {
            Log.i("BVApp", "Starting background update check for ${BuildConfig.BUILD_TYPE_NAME}")
            // 这里可以添加后台检查逻辑，但为了简单起见，我们只记录日志
            // 实际的检查会在用户主动触发时进行
        }
    }
}

val appModule = module {
    single { AuthRepository() }
    single { UserRepository(get()) }
    single { LoginRepository() }
    single { VideoInfoRepository() }
    single { ChannelRepository() }
    single { FavoriteRepository(get()) }
    single { HistoryRepository(get(), get()) }
    single { ToViewRepository(get(), get()) }    
    single { SearchRepository(get(), get()) }
    single { VideoPlayRepository(get(), get()) }
    single { RecommendVideoRepository(get(), get()) }
    single { VideoDetailRepository(get(), get(), get()) }
    single { SeasonRepository(get()) }
    single { dev.aaa1115910.biliapi.repositories.UserRepository(get(), get()) }
    single { PgcRepository() }
    single { UgcRepository(get()) }
    viewModel { DynamicViewModel(get(), get()) }
    viewModel { RecommendViewModel(get()) }
    viewModel { PopularViewModel(get()) }
    viewModel { AppQrLoginViewModel(get(), get()) }
    viewModel { SmsLoginViewModel(get(), get()) }
    viewModel { PlayerViewModel(get()) }
    viewModel { UserViewModel(get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { ToViewViewModel(get(), get()) }
    viewModel { FavoriteViewModel(get()) }
    viewModel { UpInfoViewModel(get()) }
    viewModel { FollowViewModel(get()) }
    viewModel { SearchInputViewModel(get()) }
    viewModel { SearchResultViewModel(get()) }
    viewModel { FollowingSeasonViewModel(get()) }
    viewModel { TagViewModel() }
    viewModel { VideoPlayerV3ViewModel(get(), get()) }
    viewModel { VideoDetailViewModel(get()) }
    viewModel { UserSwitchViewModel(get()) }
    viewModel { PgcIndexViewModel(get()) }
    viewModel { PgcAnimeViewModel(get()) }
    viewModel { PgcGuoChuangViewModel(get()) }
    viewModel { PgcDocumentaryViewModel(get()) }
    viewModel { PgcMovieViewModel(get()) }
    viewModel { PgcTvViewModel(get()) }
    viewModel { PgcVarietyViewModel(get()) }
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "Settings")
