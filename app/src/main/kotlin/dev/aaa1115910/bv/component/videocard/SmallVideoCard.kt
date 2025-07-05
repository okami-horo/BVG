package dev.aaa1115910.bv.component.videocard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import dev.aaa1115910.bv.component.UpIcon
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.focusedBorder

@Composable
fun SmallVideoCard(
    modifier: Modifier = Modifier,
    data: VideoCardData,
    onClick: () -> Unit = {},
    onFocus: () -> Unit = {}
) {
    val view = LocalView.current
    val context = LocalContext.current

    var hasFocus by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (hasFocus) 1f else 0.95f,
        animationSpec = tween(durationMillis = 100), // 减少动画时间
        label = "small video card scale"
    )

    LaunchedEffect(hasFocus) {
        if (hasFocus) onFocus()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { hasFocus = it.isFocused }
            .focusedBorder(MaterialTheme.shapes.medium)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Box {
                if (!view.isInEditMode) {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f)
                            .clip(MaterialTheme.shapes.medium),
                        model = ImageRequest.Builder(context)
                            .data(data.cover)
                            .crossfade(true)
                            .memoryCacheKey(data.cover)
                            .scale(Scale.FILL)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f),
                        shape = MaterialTheme.shapes.medium,
                        colors = SurfaceDefaults.colors(
                            containerColor = Color.White
                        )
                    ) {}
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    colors = SurfaceDefaults.colors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(4.dp),
                        text = data.timeString,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = data.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UpIcon()
                    Text(
                        text = data.upName,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start)
                ) {
                    Text(
                        text = "P${data.playString}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "D${data.danmakuString}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SmallVideoCardPreview() {
    val data = VideoCardData(
        avid = 0,
        title = "震惊！太震惊了！真的是太震惊了！我的天呐！真TMD震惊！",
        cover = "http://i2.hdslb.com/bfs/archive/af17fc07b8f735e822563cc45b7b5607a491dfff.jpg",
        reason = "",
        upName = "bishi",
        play = 2333,
        danmaku = 666,
        time = 2333 * 1000
    )
    BVTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            SmallVideoCard(
                data = data
            )
        }
    }
}
