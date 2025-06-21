package dev.aaa1115910.bv.util

import dev.aaa1115910.bv.BuildConfig
import io.github.oshai.kotlinlogging.KLogger

fun KLogger.fInfo(msg: () -> Any?) {
    info(msg)
    buglyLog("[Info] ${msg.toStringSafe()}")
}

fun KLogger.fWarn(msg: () -> Any?) {
    warn(msg)
    buglyLog("[Warn] ${msg.toStringSafe()}")
}

fun KLogger.fDebug(msg: () -> Any?) {
    if (BuildConfig.DEBUG) {
        info(msg)
        buglyLog("[Debug] ${msg.toStringSafe()}")
    }
}

fun KLogger.fError(msg: () -> Any?) {
    error(msg)
    buglyLog("[Error] ${msg.toStringSafe()}")
}

fun KLogger.fException(throwable: Throwable, msg: () -> Any?) {
    warn { "$msg: ${throwable.stackTraceToString()}" }
    buglyLog("[Exception] ${msg.toStringSafe()}: ${throwable.localizedMessage}")
    BuglyUtil.recordException(throwable)
}

private fun buglyLog(msg: String) {
    try {
        BuglyUtil.log(msg)
    } catch (e: Exception) {
        // 静默处理异常，防止崩溃
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun (() -> Any?).toStringSafe(): String {
    return try {
        val result = invoke()
        try {
            result?.toString() ?: "null"
        } catch (e: Exception) {
            "转换结果为字符串时出错: ${e.message}"
        }
    } catch (e: Exception) {
        "日志消息调用失败: ${e.message}"
    }
}

internal object ErrorMessageProducer {
    fun getErrorLog(e: Exception): String {
        if (System.getProperties().containsKey("kotlin-logging.throwOnMessageError")) {
            throw e
        } else {
            return "Log message invocation failed: $e"
        }
    }
}
