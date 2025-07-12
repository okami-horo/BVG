# VLC播放器集成文档

## 概述

本项目已完成VLC播放器的完整集成，提供了比ExoPlayer更强大的音视频同步功能。VLC播放器基于LibVLC实现，支持运行时音频延迟调整和手动音视频同步控制。

## 主要特性

### 1. 完整的播放器功能
- ✅ 媒体播放控制（播放、暂停、停止、跳转）
- ✅ 播放状态监听和事件回调
- ✅ 播放速度调整
- ✅ 视频尺寸获取
- ✅ 播放进度和时长获取
- ✅ 缓冲状态监控

### 2. 音视频同步功能
- ✅ **运行时音频延迟调整**（VLC独有优势）
- ✅ 音频延迟配置（支持正负值）
- ✅ 音视频同步参数优化
- ✅ 手动同步控制

### 3. UI集成
- ✅ Compose UI集成
- ✅ VLCVideoLayout视频渲染
- ✅ 播放器实例管理
- ✅ 自动重组支持

### 4. 错误处理和重试
- ✅ 自动重试机制（最多3次）
- ✅ 错误事件处理
- ✅ 播放器状态恢复

## 与ExoPlayer的对比

| 功能 | VLC播放器 | ExoPlayer |
|------|-----------|-----------|
| 运行时音频延迟调整 | ✅ 支持 | ❌ 不支持 |
| 音视频同步质量 | ✅ 优秀 | ⚠️ 一般 |
| 播放器重建成本 | ✅ 低 | ❌ 高 |
| 手动同步控制 | ✅ 支持 | ❌ 需要重建播放器 |
| 多格式支持 | ✅ 广泛 | ⚠️ 有限 |
| 性能开销 | ⚠️ 中等 | ✅ 较低 |

## 使用方法

### 1. 播放器选择
用户可以在设置中选择播放器类型：
```kotlin
// 在设置中选择VLC播放器
Prefs.playerType = PlayerType.VLC
```

### 2. 音频延迟调整
```kotlin
// 设置音频延迟（毫秒）
videoPlayer.audioDelayMs = 200L  // 音频延迟200ms
videoPlayer.audioDelayMs = -100L // 音频提前100ms
```

### 3. 播放控制
```kotlin
// 基本播放控制
videoPlayer.playUrl(videoUrl, audioUrl)
videoPlayer.prepare()
videoPlayer.start()
videoPlayer.pause()
videoPlayer.seekTo(position)

// 播放速度调整
videoPlayer.speed = 1.5f
```

## 配置说明

### 1. 依赖配置
```kotlin
// build.gradle.kts
implementation("org.videolan.android:libvlc-all:${AppConfiguration.libVLCVersion}")
```

### 2. VLC初始化参数
VLC播放器在初始化时会设置以下优化参数：
- `--no-drop-late-frames`: 不丢弃延迟帧
- `--no-skip-frames`: 不跳过帧
- `--network-caching=150`: 网络缓存150ms
- `--clock-jitter=0`: 时钟抖动为0
- `--clock-synchro=0`: 时钟同步为0
- `--audio-desync=0`: 音频去同步为0

### 3. 音视频同步配置
```kotlin
val options = VideoPlayerOptions(
    userAgent = "CustomUserAgent",
    referer = "https://example.com",
    audioDelayMs = 0L // 默认无延迟
)
```

## 技术实现

### 1. 核心类结构
```
VlcMediaPlayer
├── LibVLC实例管理
├── MediaPlayer控制
├── 事件监听处理
├── UI集成支持
└── 错误处理和重试
```

### 2. 事件处理
VLC播放器处理以下关键事件：
- `Opening`: 媒体开始打开
- `Buffering`: 缓冲状态更新
- `Playing/Paused/Stopped`: 播放状态变化
- `Vout`: 视频输出（获取视频尺寸）
- `LengthChanged/TimeChanged`: 时长和位置更新
- `EncounteredError`: 错误处理和重试

### 3. UI集成
```kotlin
// Compose UI集成
AndroidView(
    factory = { ctx ->
        VLCVideoLayout(ctx).apply {
            setBackgroundColor(Color.BLACK)
        }
    },
    update = { vlcVideoLayout ->
        videoPlayer.vlcVideoLayout = vlcVideoLayout
        videoPlayer.mediaPlayer?.attachViews(vlcVideoLayout, null, false, false)
    }
)
```

## 测试

项目包含单元测试来验证VLC播放器的基本功能：
```bash
./gradlew :bv-player:test
```

## 注意事项

1. **依赖版本**: 确保VLC依赖版本一致性
2. **权限要求**: VLC播放器需要网络和存储权限
3. **性能考虑**: VLC相比ExoPlayer有更高的内存占用
4. **兼容性**: 建议在Android 5.0+设备上使用

## 未来优化

- [ ] 添加字幕支持
- [ ] 多音轨切换
- [ ] 硬件解码优化
- [ ] 更多音视频格式支持
- [ ] 性能监控和优化
