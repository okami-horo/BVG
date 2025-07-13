package dev.aaa1115910.bv.network

import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.network.entity.Release
import dev.aaa1115910.bv.util.Prefs
import io.ktor.client.HttpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

object GithubApi {
    private var endPoint = "api.github.com"
    private const val OWNER = "okami-horo"
    private const val REPO = "BVG"
    private lateinit var client: HttpClient
    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val isDebug get() = BuildConfig.DEBUG
    private val isAlpha get() = BuildConfig.BUILD_TYPE_NAME == "alpha"
    private val isRelease get() = BuildConfig.BUILD_TYPE_NAME == "release"
    private val buildTypeName get() = BuildConfig.BUILD_TYPE_NAME

    init {
        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BrowserUserAgent()
            install(ContentNegotiation) {
                json(json)
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTPS
                    host = endPoint
                }
            }
        }
    }

    private suspend fun getReleases(
        owner: String = OWNER,
        repo: String = REPO,
        pageSize: Int = 30,
        page: Int = 1
    ): List<Release> {
        val response = client.get("repos/$owner/$repo/releases") {
            parameter("per_page", pageSize)
            parameter("page", page)
        }.bodyAsText()
        checkErrorMessage(response)
        return json.decodeFromString<List<Release>>(response)
    }

    private suspend fun getLatestRelease(
        owner: String = OWNER,
        repo: String = REPO
    ): Release {
        val response = client.get("repos/$owner/$repo/releases/latest").bodyAsText()
        checkErrorMessage(response)
        return json.decodeFromString<Release>(response)
    }

    suspend fun getLatestPreReleaseBuild(): Release {
        var release: Release? = null
        var page = 1
        while (release == null) {
            val releases = getReleases(page = page)
            if (releases.isEmpty()) break
            // 根据构建类型筛选对应的预发布版本
            release = when (buildTypeName) {
                "alpha" -> releases.firstOrNull {
                    it.isPreRelease && it.assets.any { asset -> asset.name.contains("alpha") }
                }
                "debug" -> releases.firstOrNull {
                    it.isPreRelease && it.assets.any { asset -> asset.name.contains("debug") }
                }
                else -> releases.firstOrNull { it.isPreRelease }
            }
            page++
        }
        return release ?: throw IllegalStateException("No pre-release found for build type: $buildTypeName")
    }

    suspend fun getLatestReleaseBuild(): Release = getLatestRelease()

    suspend fun getLatestBuild(): Release = when (buildTypeName) {
        "alpha" -> getLatestPreReleaseBuild()
        "debug" -> getLatestPreReleaseBuild() // debug 版本也使用 prerelease
        else -> getLatestReleaseBuild()
    }

    private fun checkErrorMessage(data: String) {
        val responseElement = json.parseToJsonElement(data)
        if (responseElement !is JsonObject) return
        val responseObject = responseElement.jsonObject
        check(responseObject.size != 2 && responseObject["message"] == null) { responseObject["message"]!!.jsonPrimitive.content }
    }

    suspend fun downloadUpdate(
        release: Release,
        file: File,
        downloadListener: ProgressListener
    ) {
        val originalDownloadUrl = when (buildTypeName) {
            "debug" -> release.assets.firstOrNull { it.name.contains("debug") }?.browserDownloadUrl
            "alpha" -> release.assets.firstOrNull { it.name.contains("alpha") }?.browserDownloadUrl
            "release" -> release.assets.firstOrNull { it.name.contains("release") }?.browserDownloadUrl
            "r8Test" -> release.assets.firstOrNull { it.name.contains("release") }?.browserDownloadUrl
            else -> release.assets.firstOrNull { it.name.contains("release") }?.browserDownloadUrl
        }
        originalDownloadUrl ?: throw IllegalStateException("Didn't find download url for build type: $buildTypeName")

        // 使用gitclone.com镜像加速下载
        val downloadUrl = originalDownloadUrl.replace("github.com", "gitclone.com")

        client.prepareRequest {
            url(downloadUrl)
            onDownload(downloadListener)
        }.execute { response ->
            response.bodyAsChannel().copyAndClose(file.writeChannel())
        }
    }
}