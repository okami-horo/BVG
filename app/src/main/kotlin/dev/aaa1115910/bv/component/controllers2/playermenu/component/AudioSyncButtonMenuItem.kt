package dev.aaa1115910.bv.component.controllers2.playermenu.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

@Composable
fun AudioSyncButtonMenuItem(
    modifier: Modifier = Modifier,
    value: Long = 0L,
    step: Long = 50L,
    range: LongRange = -1000L..1000L,
    onValueChange: (Long) -> Unit,
    onFocusBackToParent: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .onFocusChanged { hasFocus = it.hasFocus }
            .onPreviewKeyEvent {
                when (it.key) {
                    Key.Back, Key.DirectionLeft -> {
                        if (it.type == KeyEventType.KeyDown) {
                            onFocusBackToParent()
                        }
                        return@onPreviewKeyEvent true
                    }
                    else -> false
                }
            }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 增加按钮 (+50ms)
        Button(
            onClick = {
                val newValue = if (value >= range.last - step) {
                    range.last
                } else {
                    value + step
                }
                onValueChange(newValue)
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Text(text = "+${step}ms")
            }
        }

        // 显示当前值
        MenuListItem(
            modifier = Modifier.fillMaxWidth(),
            text = "${value}ms",
            selected = hasFocus
        ) { }

        // 减少按钮 (-50ms)
        Button(
            onClick = {
                val newValue = if (value <= range.first + step) {
                    range.first
                } else {
                    value - step
                }
                onValueChange(newValue)
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Remove, contentDescription = null)
                Text(text = "-${step}ms")
            }
        }

        // 重置按钮
        Button(
            onClick = {
                onValueChange(0L)
            }
        ) {
            Text(text = "重置")
        }
    }
}
