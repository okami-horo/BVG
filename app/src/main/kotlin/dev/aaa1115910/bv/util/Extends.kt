package dev.aaa1115910.bv.util

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.core.text.HtmlCompat
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

fun String.toast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this, duration).show()
}

fun Int.toast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, context.getText(this), duration).show()
}

fun <T> SnapshotStateList<T>.swapList(newList: List<T>) {
    clear()
    addAll(newList)
}

suspend fun <T> SnapshotStateList<T>.swapListWithMainContext(newList: List<T>) =
    withContext(Dispatchers.Main) { this@swapListWithMainContext.swapList(newList) }

suspend fun <T> SnapshotStateList<T>.swapListWithMainContext(
    newList: List<T>,
    delay: Long,
    afterSwap: () -> Unit
) {
    this@swapListWithMainContext.swapListWithMainContext(newList)
    delay(delay)
    afterSwap()
}

suspend fun <T> SnapshotStateList<T>.addAllWithMainContext(newList: List<T>) =
    withContext(Dispatchers.Main) { addAll(newList) }

suspend fun <T> SnapshotStateList<T>.addAllWithMainContext(newListBlock: suspend () -> List<T>) {
    val newList = newListBlock()
    withContext(Dispatchers.Main) { addAll(newList) }
}


suspend fun <T> SnapshotStateList<T>.addWithMainContext(item: T) =
    withContext(Dispatchers.Main) { add(item) }


fun <K, V> SnapshotStateMap<K, V>.swapMap(newMap: Map<K, V>) {
    clear()
    putAll(newMap)
}

suspend fun <K, V> SnapshotStateMap<K, V>.swapMapWithMainContext(newMap: Map<K, V>) =
    withContext(Dispatchers.Main) { this@swapMapWithMainContext.swapMap(newMap) }

fun <K, V> SnapshotStateMap<K, V>.swapMap(newMap: Map<K, V>, afterSwap: () -> Unit) {
    this.swapMap(newMap)
    afterSwap()
}

fun Date.formatPubTimeString(context: Context = BVApp.context): String {
    val temp = System.currentTimeMillis() - time
    return when {
        temp > 1000L * 60 * 60 * 24 -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(this)
        temp > 1000L * 60 * 60 -> context.getString(
            R.string.date_format_hours_age, temp / (1000 * 60 * 60)
        )

        temp > 1000L * 60 -> context.getString(
            R.string.date_format_minutes_age, temp / (1000 * 60)
        )

        else -> context.getString(R.string.date_format_just_now)
    }
}

fun Long.formatMinSec(): String {
    return if (this < 0L) {
        "..."
    } else {
        String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) -
                    TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(this)
                    )
        )
    }
}

/**
 * 改进的请求焦点的方法，失败后等待 100ms 后重试
 * 添加更多错误处理，避免在组件未准备好时请求焦点导致的崩溃
 */
fun FocusRequester.requestFocus(scope: CoroutineScope) {
    scope.launch(Dispatchers.Main) {
        try {
            // 先尝试在主线程请求焦点
            requestFocus()
        } catch (e: Exception) {
            // 如果失败，等待组件准备好后再尝试
            delay(100)
            try {
                requestFocus()
            } catch (e: Exception) {
                // 如果再次失败，忽略错误，避免应用崩溃
                // 可能是因为组件已被销毁或尚未附加到Compose层次结构
            }
        }
    }
}

fun String.removeHtmlTags(): String = HtmlCompat.fromHtml(
    this, HtmlCompat.FROM_HTML_MODE_LEGACY
).toString()

fun KeyEvent.isKeyDown(): Boolean = type == KeyEventType.KeyDown
fun KeyEvent.isKeyUp(): Boolean = type == KeyEventType.KeyUp
fun KeyEvent.isDpadUp(): Boolean = key == Key.DirectionUp
fun KeyEvent.isDpadDown(): Boolean = key == Key.DirectionDown
fun KeyEvent.isDpadLeft(): Boolean = key == Key.DirectionLeft
fun KeyEvent.isDpadRight(): Boolean = key == Key.DirectionRight

fun Int.stringRes(context: Context): String = context.getString(this)