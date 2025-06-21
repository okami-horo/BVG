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
    BuglyUtil.log(msg)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun (() -> Any?).toStringSafe(): String {
    return try {
        invoke().toString()
    } catch (e: Exception) {
        ErrorMessageProducer.getErrorLog(e)
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
