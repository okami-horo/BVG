package dev.aaa1115910.bv.util

import android.content.Context
import com.tencent.bugly.crashreport.CrashReport
import dev.aaa1115910.bv.BuildConfig
import java.io.BufferedReader
import java.io.FileReader

object BuglyUtil {
    private var initialized = false
    private var appContext: Context? = null

    /**
     * 初始化Bugly
     * 尝试从以下位置读取AppID（按优先级）:
     * 1. BuildConfig.BUGLY_APP_ID (由GitHub Secret注入)
     * 2. assets/bugly/app_id.txt文件
     */
    fun init(context: Context) {
        try {
            appContext = context.applicationContext
            
            // 尝试获取AppID
            var appId = getBuglyAppId(context)
            if (appId.isBlank()) {
                return
            }

            // 初始化策略
            val strategy = CrashReport.UserStrategy(context).apply {
                // 设置是否为上报进程
                isUploadProcess = true
                // 设置渠道信息
                appChannel = BuildConfig.FLAVOR
                // 设置版本号
                appVersion = BuildConfig.VERSION_NAME
                // 设置设备ID
                deviceID = getDeviceId(context)
            }

            // 初始化Bugly，无论是否为Debug版本都上传崩溃报告
            CrashReport.initCrashReport(context, appId, BuildConfig.DEBUG, strategy)
            initialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            // 初始化失败，不影响应用正常运行
        }
    }

    /**
     * 获取Bugly AppID，按优先级尝试不同来源
     */
    private fun getBuglyAppId(context: Context): String {
        // 1. 直接从BuildConfig读取（由GitHub Actions注入）
        if (BuildConfig.BUGLY_APP_ID.isNotBlank()) {
            return BuildConfig.BUGLY_APP_ID
        }
        
        // 2. 尝试从assets/bugly/app_id.txt读取
        try {
            val appId = context.assets.open("bugly/app_id.txt").bufferedReader().use { 
                it.readText().trim().lines().firstOrNull { line -> 
                    !line.startsWith("#") && line.isNotBlank() 
                } ?: ""
            }
            if (appId.isNotBlank()) {
                return appId
            }
        } catch (e: Exception) {
            // 文件不存在或读取失败
        }
        
        return ""
    }

    /**
     * 获取设备唯一标识符
     * 这里使用了Android ID作为唯一标识
     */
    private fun getDeviceId(context: Context): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }

    /**
     * 记录日志
     */
    fun log(msg: String) {
        if (!initialized) return
        try {
            // 限制日志长度，避免过长导致崩溃
            val limitedMsg = if (msg.length > 500) msg.substring(0, 500) + "...(日志已截断)" else msg
            // 使用 putUserData 而不是 postCatchedException 记录日志
            // 这样不会在控制台显示为崩溃或异常
            val logKey = "log_${System.currentTimeMillis()}"
            CrashReport.putUserData(appContext, logKey, "[BuglyLog] $limitedMsg")
        } catch (e: Exception) {
            // 捕获可能的异常，避免崩溃
            e.printStackTrace()
        }
    }

    /**
     * 上报异常
     */
    fun recordException(throwable: Throwable) {
        if (!initialized) return
        CrashReport.postCatchedException(throwable)
    }

    /**
     * 控制是否启用崩溃收集
     */
    fun setCrashlyticsCollectionEnabled(enable: Boolean) {
        if (!initialized) return
        // 修复命名参数错误，改为位置参数
        CrashReport.setIsDevelopmentDevice(null, !enable) // 反转逻辑，开启收集时不是开发设备
    }
} 