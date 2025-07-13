@file:Suppress("SpellCheckingInspection")

package dev.aaa1115910.bv.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import de.schnettler.datastore.manager.PreferenceRequest
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.util.generateBuvid
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.component.controllers2.DanmakuType
import dev.aaa1115910.bv.entity.Audio
import dev.aaa1115910.bv.entity.PlayerType
import dev.aaa1115910.bv.entity.Resolution
import dev.aaa1115910.bv.entity.VideoCodec
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.UUID
import kotlin.math.roundToInt

object Prefs {
    private val dsm = BVApp.dataStoreManager
    val logger = KotlinLogging.logger { }

    var isLogin: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefIsLoginRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefIsLoginKey, value) }

    var uid: Long
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUidRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUidKey, value) }

    var sid: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefSidRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefSidKey, value) }

    var sessData: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefSessDataRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefSessDataKey, value) }

    var biliJct: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefBiliJctRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBiliJctKey, value) }

    var uidCkMd5: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUidCkMd5Request).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUidCkMd5Key, value) }

    var tokenExpiredData: Date
        get() = Date(runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefTokenExpiredDateRequest).first()
        })
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefTokenExpiredDateKey, value.time)
        }

    var defaultQuality: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDefaultQualityRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultQualityKey, value) }

    var defaultPlaySpeed: Float
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDefaultPlaySpeedRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultPlaySpeedKey, value) }

    var defaultAudio: Audio
        get() = runBlocking {
            Audio.fromCode(dsm.getPreferenceFlow(PrefKeys.prefDefaultAudioRequest).first())
                ?: Audio.A192K
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultAudioKey, value.code) }

    var defaultDanmakuSize: Int
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuSizeRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuSizeKey, value) }

    var defaultDanmakuScale: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuScaleRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuScaleKey, value) }

    var defaultDanmakuTransparency: Int
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuTransparencyRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuTransparencyKey, value)
        }

    var defaultDanmakuOpacity: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuOpacityRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuOpacityKey, value)
        }

    var defaultDanmakuEnabled: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuEnabledRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultDanmakuEnabledKey, value)
        }

    var defaultDanmakuTypes: List<DanmakuType>
        get() = runBlocking {
            val danmakuTypeIdsString =
                dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuTypesRequest).first()
            if (danmakuTypeIdsString == "") {
                emptyList()
            } else {
                danmakuTypeIdsString.split(",").map { DanmakuType.entries[it.toInt()] }
            }
        }
        set(value) = runBlocking {
            dsm.editPreference(
                PrefKeys.prefDefaultDanmakuTypesKey,
                value.map { it.ordinal }.joinToString(",")
            )
        }

    var defaultDanmakuArea: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuAreaRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuAreaKey, value) }

    var defaultVideoCodec: VideoCodec
        get() = VideoCodec.fromCode(
            runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDefaultVideoCodecRequest).first() }
        )
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultVideoCodecKey, value.ordinal)
        }

    var enableFirebaseCollection: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefEnabledFirebaseCollectionRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefEnabledFirebaseCollectionKey, value)
        }

    var incognitoMode: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefIncognitoModeRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefIncognitoModeKey, value) }

    var defaultSubtitleFontSize: TextUnit
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleFontSizeRequest).first().sp
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultSubtitleFontSizeKey, value.value.roundToInt())
        }

    var defaultSubtitleBackgroundOpacity: Float
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleBackgroundOpacityRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultSubtitleBackgroundOpacityKey, value)
        }

    var defaultSubtitleBottomPadding: Dp
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultSubtitleBottomPaddingRequest).first().dp
        }
        set(value) = runBlocking {
            dsm.editPreference(
                PrefKeys.prefDefaultSubtitleBottomPaddingKey, value.value.roundToInt()
            )
        }

    var showFps: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefShowFpsRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefShowFpsKey, value) }

    var buvid: String
        get() = runBlocking {
            val id = dsm.getPreferenceFlow(PrefKeys.prefBuvidRequest).first()
            if (id != "") {
                id
            } else {
                val randomBuvid = generateBuvid()
                buvid3 = randomBuvid
                randomBuvid
            }
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBuvidKey, value) }

    var buvid3: String
        get() = runBlocking {
            val id = dsm.getPreferenceFlow(PrefKeys.prefBuvid3Request).first()
            if (id != "") {
                id
            } else {
                //random buvid3
                val randomBuvid3 = "${UUID.randomUUID()}${(0..9).random()}infoc"
                buvid3 = randomBuvid3
                randomBuvid3
            }
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefBuvid3Key, value) }

    var playerType: PlayerType
        get() = runBlocking {
            runCatching {
                PlayerType.entries[dsm.getPreferenceFlow(PrefKeys.prefPlayerTypeRequest).first()]
            }.getOrDefault(PlayerType.Media3)
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPlayerTypeKey, value.ordinal) }

    val densityFlow: Flow<Float> get() = dsm.getPreferenceFlow(PrefKeys.prefDensityRequest)
    var density: Float
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefDensityRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDensityKey, value) }

    var useOldPlayer: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefUseOldPlayerRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefUseOldPlayerKey, value) }

    var updateAlpha: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefAlphaRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefAlphaKey, value) }

    var githubMirrorPrefix: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefGithubMirrorPrefixRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefGithubMirrorPrefixKey, value) }

    var accessToken: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefAccessTokenRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefAccessTokenKey, value) }

    var refreshToken: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefRefreshTokenRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefRefreshTokenKey, value) }

    var apiType: ApiType
        get() = runBlocking {
            ApiType.entries[dsm.getPreferenceFlow(PrefKeys.prefApiTypeRequest).first()]
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefApiTypeKey, value.ordinal) }

    var enableProxy: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefEnabelProxyRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefEnableProxyKey, value) }

    var proxyHttpServer: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefProxyHttpServerRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefProxyHttpServerKey, value) }

    var proxyGRPCServer: String
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefProxyGRPCServerRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefProxyGRPCServerKey, value) }

    var lastVersionCode: Int
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefLastVersionCodeRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefLastVersionCodeKey, value) }

    var autoCheckUpdate: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefAutoCheckUpdateRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefAutoCheckUpdateKey, value) }

    var lastUpdateCheckTime: Long
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefLastUpdateCheckTimeRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefLastUpdateCheckTimeKey, value) }

    var showedRemoteControllerPanelDemo: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefShowedRemoteControllerPanelDemoRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefShowedRemoteControllerPanelDemoKey, value)
        }

    var preferOfficialCdn: Boolean
        get() = runBlocking { dsm.getPreferenceFlow(PrefKeys.prefPreferOfficialCdnRequest).first() }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefPreferOfficialCdn, value) }

    var defaultDanmakuMask: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultDanmakuMaskRequest).first()
        }
        set(value) = runBlocking { dsm.editPreference(PrefKeys.prefDefaultDanmakuMask, value) }

    var enableFfmpegAudioRenderer: Boolean
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefEnableFfmpegEndererRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(
                PrefKeys.prefEnableFfmpegAudioRenderer,
                value
            )
        }

    var defaultAudioDelayMs: Long
        get() = runBlocking {
            dsm.getPreferenceFlow(PrefKeys.prefDefaultAudioDelayMsRequest).first()
        }
        set(value) = runBlocking {
            dsm.editPreference(PrefKeys.prefDefaultAudioDelayMs, value)
        }
}

private object PrefKeys {
    val prefIsLoginKey = booleanPreferencesKey("il")
    val prefUidKey = longPreferencesKey("uid")
    val prefSidKey = stringPreferencesKey("sid")
    val prefSessDataKey = stringPreferencesKey("sd")
    val prefBiliJctKey = stringPreferencesKey("bj")
    val prefUidCkMd5Key = stringPreferencesKey("ucm")
    val prefTokenExpiredDateKey = longPreferencesKey("ted")
    val prefDefaultQualityKey = intPreferencesKey("dq")
    val prefDefaultAudioKey = intPreferencesKey("da")
    val prefDefaultPlaySpeedKey = floatPreferencesKey("dps")
    val prefDefaultDanmakuSizeKey = intPreferencesKey("dds")
    val prefDefaultDanmakuScaleKey = floatPreferencesKey("dds2")
    val prefDefaultDanmakuTransparencyKey = intPreferencesKey("ddt")
    val prefDefaultDanmakuOpacityKey = floatPreferencesKey("ddo")
    val prefDefaultDanmakuEnabledKey = booleanPreferencesKey("dde")
    val prefDefaultDanmakuTypesKey = stringPreferencesKey("ddts")
    val prefDefaultDanmakuAreaKey = floatPreferencesKey("dda")
    val prefDefaultVideoCodecKey = intPreferencesKey("dvc")
    val prefEnabledFirebaseCollectionKey = booleanPreferencesKey("efc")
    val prefIncognitoModeKey = booleanPreferencesKey("im")
    val prefDefaultSubtitleFontSizeKey = intPreferencesKey("dsfs")
    val prefDefaultSubtitleBackgroundOpacityKey = floatPreferencesKey("dsbo")
    val prefDefaultSubtitleBottomPaddingKey = intPreferencesKey("dsbp")
    val prefShowFpsKey = booleanPreferencesKey("sf")
    val prefBuvidKey = stringPreferencesKey("random_buvid")
    val prefBuvid3Key = stringPreferencesKey("random_buvid3")
    val prefPlayerTypeKey = intPreferencesKey("pt")
    val prefDensityKey = floatPreferencesKey("density")
    val prefUseOldPlayerKey = booleanPreferencesKey("uop")
    val prefAlphaKey = booleanPreferencesKey("alpha")
    val prefAccessTokenKey = stringPreferencesKey("access_token")
    val prefRefreshTokenKey = stringPreferencesKey("refresh_token")
    val prefApiTypeKey = intPreferencesKey("api_type")
    val prefEnableProxyKey = booleanPreferencesKey("enable_proxy")
    val prefProxyHttpServerKey = stringPreferencesKey("proxy_http_server")
    val prefProxyGRPCServerKey = stringPreferencesKey("proxy_grpc_server")
    val prefLastVersionCodeKey = intPreferencesKey("last_version_code")
    val prefAutoCheckUpdateKey = booleanPreferencesKey("auto_check_update")
    val prefLastUpdateCheckTimeKey = longPreferencesKey("last_update_check_time")
    val prefShowedRemoteControllerPanelDemoKey = booleanPreferencesKey("showed_rcpd")
    val prefPreferOfficialCdn = booleanPreferencesKey("prefer_official_cdn")
    val prefDefaultDanmakuMask = booleanPreferencesKey("prefer_enable_webmark")
    val prefEnableFfmpegAudioRenderer = booleanPreferencesKey("enable_ffmpeg_audio_renderer")
    val prefDefaultAudioDelayMs = longPreferencesKey("default_audio_delay_ms")
    val prefGithubMirrorPrefixKey = stringPreferencesKey("github_mirror_prefix")

    val prefIsLoginRequest = PreferenceRequest(prefIsLoginKey, false)
    val prefUidRequest = PreferenceRequest(prefUidKey, 0)
    val prefSidRequest = PreferenceRequest(prefSidKey, "")
    val prefSessDataRequest = PreferenceRequest(prefSessDataKey, "")
    val prefBiliJctRequest = PreferenceRequest(prefBiliJctKey, "")
    val prefUidCkMd5Request = PreferenceRequest(prefUidCkMd5Key, "")
    val prefTokenExpiredDateRequest = PreferenceRequest(prefTokenExpiredDateKey, 0)
    val prefDefaultPlaySpeedRequest = PreferenceRequest(prefDefaultPlaySpeedKey, 1f)
    val prefDefaultQualityRequest = PreferenceRequest(prefDefaultQualityKey, Resolution.R1080P.code)
    val prefDefaultAudioRequest = PreferenceRequest(prefDefaultAudioKey, Audio.A192K.code)
    val prefDefaultDanmakuSizeRequest = PreferenceRequest(prefDefaultDanmakuSizeKey, 6)
    val prefDefaultDanmakuScaleRequest = PreferenceRequest(prefDefaultDanmakuScaleKey, 1f)
    val prefDefaultDanmakuTransparencyRequest =
        PreferenceRequest(prefDefaultDanmakuTransparencyKey, 0)
    val prefDefaultDanmakuOpacityRequest = PreferenceRequest(prefDefaultDanmakuOpacityKey, 1f)
    val prefDefaultDanmakuEnabledRequest = PreferenceRequest(prefDefaultDanmakuEnabledKey, true)
    val prefDefaultDanmakuTypesRequest =
        PreferenceRequest(prefDefaultDanmakuTypesKey, "0,1,2,3")
    val prefDefaultDanmakuAreaRequest = PreferenceRequest(prefDefaultDanmakuAreaKey, 1f)
    val prefDefaultVideoCodecRequest =
        PreferenceRequest(prefDefaultVideoCodecKey, VideoCodec.AVC.ordinal)
    val prefEnabledFirebaseCollectionRequest =
        PreferenceRequest(prefEnabledFirebaseCollectionKey, true)
    val prefIncognitoModeRequest = PreferenceRequest(prefIncognitoModeKey, false)
    val prefDefaultSubtitleFontSizeRequest = PreferenceRequest(prefDefaultSubtitleFontSizeKey, 24)
    val prefDefaultSubtitleBackgroundOpacityRequest =
        PreferenceRequest(prefDefaultSubtitleBackgroundOpacityKey, 0.4f)
    val prefDefaultSubtitleBottomPaddingRequest =
        PreferenceRequest(prefDefaultSubtitleBottomPaddingKey, 12)
    val prefShowFpsRequest = PreferenceRequest(prefShowFpsKey, false)
    val prefBuvidRequest = PreferenceRequest(prefBuvidKey, "")
    val prefBuvid3Request = PreferenceRequest(prefBuvid3Key, "")
    val prefPlayerTypeRequest = PreferenceRequest(prefPlayerTypeKey, PlayerType.Media3.ordinal)
    val prefDensityRequest =
        PreferenceRequest(prefDensityKey, BVApp.context.resources.displayMetrics.widthPixels / 960f)
    val prefUseOldPlayerRequest = PreferenceRequest(prefUseOldPlayerKey, false)

    @Suppress("KotlinConstantConditions")
    val prefAlphaRequest = PreferenceRequest(prefAlphaKey, BuildConfig.BUILD_TYPE == "alpha")
    val prefAccessTokenRequest = PreferenceRequest(prefAccessTokenKey, "")
    val prefRefreshTokenRequest = PreferenceRequest(prefRefreshTokenKey, "")
    val prefApiTypeRequest = PreferenceRequest(prefApiTypeKey, 0)
    val prefEnabelProxyRequest = PreferenceRequest(prefEnableProxyKey, false)
    val prefProxyHttpServerRequest = PreferenceRequest(prefProxyHttpServerKey, "")
    val prefProxyGRPCServerRequest = PreferenceRequest(prefProxyGRPCServerKey, "")
    val prefLastVersionCodeRequest = PreferenceRequest(prefLastVersionCodeKey, 0)
    val prefAutoCheckUpdateRequest = PreferenceRequest(prefAutoCheckUpdateKey, true)
    val prefLastUpdateCheckTimeRequest = PreferenceRequest(prefLastUpdateCheckTimeKey, 0L)
    val prefShowedRemoteControllerPanelDemoRequest =
        PreferenceRequest(prefShowedRemoteControllerPanelDemoKey, false)
    val prefPreferOfficialCdnRequest = PreferenceRequest(prefPreferOfficialCdn, false)
    val prefDefaultDanmakuMaskRequest = PreferenceRequest(prefDefaultDanmakuMask, false)
    val prefEnableFfmpegEndererRequest = PreferenceRequest(prefEnableFfmpegAudioRenderer, true)
    val prefDefaultAudioDelayMsRequest = PreferenceRequest(prefDefaultAudioDelayMs, 0L)
    val prefGithubMirrorPrefixRequest = PreferenceRequest(prefGithubMirrorPrefixKey, "https://gh-proxy.com/")
}