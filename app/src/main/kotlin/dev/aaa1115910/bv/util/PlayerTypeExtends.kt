package dev.aaa1115910.bv.util

import android.content.Context
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.PlayerType

fun PlayerType.getDisplayName(context: Context) = when (this) {
    PlayerType.Media3 -> context.getString(R.string.player_type_media3)
    PlayerType.VLC -> context.getString(R.string.player_type_vlc)
}
