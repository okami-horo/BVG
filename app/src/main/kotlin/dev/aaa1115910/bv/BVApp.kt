package dev.aaa1115910.bv

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import de.schnettler.datastore.manager.DataStoreManager
import dev.aaa1115910.biliapi.http.BiliHttpProxyApi
import dev.aaa1115910.biliapi.repositories.*
import dev.aaa1115910.bv.dao.AppDatabase
import dev.aaa1115910.bv.entity.AuthData
import dev.aaa1115910.bv.entity.db.UserDB
import dev.aaa1115910.bv.network.HttpServer
import dev.aaa1115910.bv.repository.*
import dev.aaa1115910.bv.util.*
import dev.aaa1115910.bv.viewmodel.*
import dev.aaa1115910.bv.viewmodel.home.*
import dev.aaa1115910.bv.viewmodel.index.PgcIndexViewModel
import dev.aaa1115910.bv.viewmodel.login.*
import dev.aaa1115910.bv.viewmodel.pgc.*
import dev.aaa1115910.bv.viewmodel.search.*
import dev.aaa1115910.bv.viewmodel.user.*
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

class BVApp : Application() {
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
        context = applicationContext
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        dataStoreManager = DataStoreManager(applicationContext.dataStore)
        koinApplication = startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@BVApp)
            modules(appModule)
        }
        FirebaseUtil.init(applicationContext)
        LogCatcherUtil.installLogCatcher()
        initRepository()
        initProxy()
        instance = this
        updateMigration()
        HttpServer.startServer()
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
                channelRepository.initProxyChannel(Prefs.accessToken, Prefs.buvid, Prefs.proxyGRPCServer)
            }
        }
    }

    private fun updateMigration() {
        val lastVersionCode = Prefs.lastVersionCode
        if (lastVersionCode >= BuildConfig.VERSION_CODE) return
        Log.i("BVApp", "updateMigration from $lastVersionCode")
        if (lastVersionCode < 576 && Prefs.isLogin) {
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
        Prefs.lastVersionCode = BuildConfig.VERSION_CODE
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
    viewModel { FollowingSeasonViewModel(get()) }
    viewModel { SearchInputViewModel(get()) }
    viewModel { SearchResultViewModel(get()) }
    viewModel { TagViewModel() }
    viewModel { VideoPlayerV3ViewModel(get(), get()) }
    viewModel { VideoDetailViewModel(get()) }
    viewModel { PgcIndexViewModel(get()) }
    viewModel { PgcAnimeViewModel(get()) }
    viewModel { PgcGuoChuangViewModel(get()) }
    viewModel { PgcDocumentaryViewModel(get()) }
    viewModel { PgcMovieViewModel(get()) }
    viewModel { PgcTvViewModel(get()) }
    viewModel { PgcVarietyViewModel(get()) }
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "Settings")
