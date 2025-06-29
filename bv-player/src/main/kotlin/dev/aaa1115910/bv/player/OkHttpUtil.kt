package dev.aaa1115910.bv.player

import android.content.Context
import android.os.Build
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object OkHttpUtil {
    // 全局连接池配置，限制连接数量
    private val connectionPool = ConnectionPool(
        maxIdleConnections = 5, // 最大空闲连接数
        keepAliveDuration = 30, // 连接保持时间
        timeUnit = TimeUnit.SECONDS // 时间单位
    )
    
    /**
     * 生成自定义SSL配置的OkHttpClient
     * 限制了连接池大小，避免okio.Segment内存问题
     */
    fun generateCustomSslOkHttpClient(context: Context): OkHttpClient {
        // 创建一个信任所有证书的TrustManager
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // 创建SSL上下文
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        // 构建OkHttpClient
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectionPool(connectionPool) // 使用限制连接数的连接池
            .protocols(listOf(Protocol.HTTP_1_1, Protocol.HTTP_2)) // 显式指定协议
            .connectTimeout(15, TimeUnit.SECONDS) // 连接超时
            .readTimeout(15, TimeUnit.SECONDS) // 读取超时
            .writeTimeout(15, TimeUnit.SECONDS) // 写入超时
            .retryOnConnectionFailure(true) // 允许重试
            .build()
    }
}