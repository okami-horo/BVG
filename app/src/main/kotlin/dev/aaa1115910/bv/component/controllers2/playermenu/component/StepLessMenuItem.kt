package dev.aaa1115910.bv.component.controllers2.playermenu.component

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
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
import androidx.tv.material3.Icon

@Composable
fun StepLessMenuItem(
    modifier: Modifier = Modifier,
    value: Float = 1f,
    text: String,
    step: Float = 0.01f,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    onFocusBackToParent: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .focusable()
            .onFocusChanged { hasFocus = it.hasFocus }
            .onPreviewKeyEvent {
                println("StepLessMenuItem KeyEvent: ${it.key}, type: ${it.type}")
                when (it.key) {
                    Key.DirectionUp -> {
                        if (it.type == KeyEventType.KeyDown) {
                            val newValue = if (value >= range.endInclusive - step) {
                                range.endInclusive
                            } else {
                                value + step
                            }
                            println("StepLessMenuItem: Up pressed, changing value from $value to $newValue")
                            onValueChange(newValue)
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionDown -> {
                        if (it.type == KeyEventType.KeyDown) {
                            val newValue = if (value - step <= range.start) {
                                range.start
                            } else {
                                value - step
                            }
                            println("StepLessMenuItem: Down pressed, changing value from $value to $newValue")
                            onValueChange(newValue)
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionLeft, Key.DirectionRight -> {
                        if (it.type == KeyEventType.KeyDown) {
                            println("StepLessMenuItem: Left/Right pressed, going back to parent")
                            onFocusBackToParent()
                        }
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Rounded.ArrowDropUp, contentDescription = null)
        MenuListItem(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            selected = hasFocus
        ) { }
        Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
    }
}

@Composable
fun StepLessMenuItem(
    modifier: Modifier = Modifier,
    value: Int = 100,
    text: String,
    step: Int = 1,
    range: IntRange = 0..100,
    onValueChange: (Int) -> Unit,
    onFocusBackToParent: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .focusable()
            .onFocusChanged { hasFocus = it.hasFocus }
            .onPreviewKeyEvent {
                println("StepLessMenuItem Int KeyEvent: ${it.key}, type: ${it.type}")
                when (it.key) {
                    Key.DirectionUp -> {
                        if (it.type == KeyEventType.KeyDown) {
                            val newValue = if (value >= range.last - step) {
                                range.last
                            } else {
                                value + step
                            }
                            println("StepLessMenuItem Int: Up pressed, changing value from $value to $newValue")
                            onValueChange(newValue)
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionDown -> {
                        if (it.type == KeyEventType.KeyDown) {
                            val newValue = if (value - step <= range.first) {
                                range.first
                            } else {
                                value - step
                            }
                            println("StepLessMenuItem Int: Down pressed, changing value from $value to $newValue")
                            onValueChange(newValue)
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionLeft, Key.DirectionRight -> {
                        if (it.type == KeyEventType.KeyDown) {
                            println("StepLessMenuItem Int: Left/Right pressed, going back to parent")
                            onFocusBackToParent()
                        }
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Rounded.ArrowDropUp, contentDescription = null)
        MenuListItem(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            selected = hasFocus
        ) { }
        Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
    }
}
