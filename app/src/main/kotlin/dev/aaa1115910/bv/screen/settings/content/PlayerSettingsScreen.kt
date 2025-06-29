package dev.aaa1115910.bv.screen.settings.content

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.player.impl.exo.ExoMediaPlayer
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.viewmodel.VideoPlayerV3ViewModel
import kotlinx.coroutines.launch

@Composable
fun PlayerSettingsScreen(
    playerViewModel: VideoPlayerV3ViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { Prefs.preferOfficialCDN = !Prefs.preferOfficialCDN }
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.prefer_official_cdn))
            Switch(
                checked = Prefs.preferOfficialCDN,
                onCheckedChange = { Prefs.preferOfficialCDN = it }
            )
        }
        
        // 添加DASH格式设置选项
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { Prefs.useDashFormat = !Prefs.useDashFormat }
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "使用DASH格式")
            Switch(
                checked = Prefs.useDashFormat,
                onCheckedChange = { Prefs.useDashFormat = it }
            )
        }
        
        // 添加说明文字
        Text(
            text = "DASH格式可以提高音视频同步性能，但可能在某些设备上不兼容",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // 添加缓存设置选项
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { Prefs.enableCache = !Prefs.enableCache }
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "启用视频缓存")
            Switch(
                checked = Prefs.enableCache,
                onCheckedChange = { Prefs.enableCache = it }
            )
        }
        
        // 添加说明文字
        Text(
            text = "缓存可以提高播放流畅度，减少重复加载，但会占用存储空间",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        
        // 添加清除缓存按钮
        Button(
            onClick = {
                scope.launch {
                    // 获取ExoMediaPlayer实例并清除缓存
                    val player = playerViewModel.videoPlayer as? ExoMediaPlayer
                    player?.clearCache()
                    Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text("清除视频缓存")
        }
        
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { Prefs.enableFfmpegAudioRenderer = !Prefs.enableFfmpegAudioRenderer }
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.enable_ffmpeg_audio_renderer))
            Switch(
                checked = Prefs.enableFfmpegAudioRenderer,
                onCheckedChange = { Prefs.enableFfmpegAudioRenderer = it }
            )
        }
    }
} 