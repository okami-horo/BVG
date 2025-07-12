package dev.aaa1115910.bv.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.network.GithubApi
import dev.aaa1115910.bv.network.entity.Release
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.content.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object AutoUpdateChecker {
    private val logger = KotlinLogging.logger("AutoUpdateChecker")
    
    // 检查更新的间隔时间（24小时）
    private const val UPDATE_CHECK_INTERVAL = 24 * 60 * 60 * 1000L
    
    /**
     * 检查是否需要自动检查更新
     */
    fun shouldCheckUpdate(): Boolean {
        if (!Prefs.autoCheckUpdate) return false
        
        val lastCheckTime = Prefs.lastUpdateCheckTime
        val currentTime = System.currentTimeMillis()
        
        return currentTime - lastCheckTime > UPDATE_CHECK_INTERVAL
    }
    
    /**
     * 检查是否有可用更新
     * @return Pair<Boolean, Release?> - 是否有更新和最新版本信息
     */
    suspend fun checkForUpdate(): Pair<Boolean, Release?> = withContext(Dispatchers.IO) {
        try {
            logger.info { "Checking for updates for build type: ${BuildConfig.BUILD_TYPE_NAME}" }
            
            val latestRelease = GithubApi.getLatestBuild()
            val latestVersionCode = latestRelease.assets
                .firstOrNull { it.name.startsWith("BV") }
                ?.name?.split("_")?.getOrNull(1)?.toIntOrNull()
                ?: return@withContext Pair(false, null)
            
            val currentVersionCode = BuildConfig.VERSION_CODE
            val hasUpdate = latestVersionCode > currentVersionCode
            
            // 更新最后检查时间
            Prefs.lastUpdateCheckTime = System.currentTimeMillis()
            
            logger.info { 
                "Update check result: current=$currentVersionCode, latest=$latestVersionCode, hasUpdate=$hasUpdate" 
            }
            
            Pair(hasUpdate, if (hasUpdate) latestRelease else null)
        } catch (e: Exception) {
            logger.error(e) { "Failed to check for updates" }
            Pair(false, null)
        }
    }
    
    /**
     * 下载更新文件
     */
    suspend fun downloadUpdate(
        context: Context,
        release: Release,
        progressListener: ProgressListener? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            val tempFilename = "${UUID.randomUUID()}.apk"
            val tempDir = File(context.cacheDir, "auto_update_downloader")
            if (!tempDir.exists()) tempDir.mkdirs()
            val tempFile = File(tempDir, tempFilename)
            tempFile.createNewFile()
            
            logger.info { "Downloading update to: ${tempFile.absolutePath}" }
            
            GithubApi.downloadUpdate(release, tempFile, progressListener ?: object : ProgressListener {
                override suspend fun onProgress(downloaded: Long, total: Long?) {
                    // 默认空实现
                }
            })
            
            logger.info { "Update downloaded successfully" }
            tempFile
        } catch (e: Exception) {
            logger.error(e) { "Failed to download update" }
            null
        }
    }
    
    /**
     * 安装更新
     */
    fun installUpdate(context: Context, file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context, 
                "${BuildConfig.APPLICATION_ID}.provider", 
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            logger.info { "Update installation started" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to install update" }
            false
        }
    }
    
    /**
     * 获取构建类型的显示名称
     */
    fun getBuildTypeDisplayName(): String {
        return when (BuildConfig.BUILD_TYPE_NAME) {
            "alpha" -> "Alpha"
            "debug" -> "Debug"
            "release" -> "Release"
            "r8Test" -> "R8 Test"
            else -> BuildConfig.BUILD_TYPE_NAME.uppercase()
        }
    }
    
    /**
     * 清理旧的更新文件
     */
    fun cleanupOldUpdateFiles(context: Context) {
        try {
            val tempDir = File(context.cacheDir, "auto_update_downloader")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".apk")) {
                        // 删除超过1天的文件
                        if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000L) {
                            file.delete()
                            logger.info { "Cleaned up old update file: ${file.name}" }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to cleanup old update files" }
        }
    }
}
