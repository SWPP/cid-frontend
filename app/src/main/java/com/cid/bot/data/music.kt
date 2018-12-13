package com.cid.bot.data

import com.cid.bot.ellipsize
import com.google.gson.annotations.SerializedName

data class Music(
        var title: String? = null,
        var album: String? = null,
        @SerializedName("album_id")
        var albumId: Int? = null,
        var artists: List<String> = listOf(),
        var length: Int? = null
) {
    val artistsRep: String
        get() = artists.ellipsize()
}
