package yt_music.pojos

import com.google.gson.JsonObject

data class Thumbnail(val url: String, val width: Int, val height: Int, val json: JsonObject) {

    companion object {
        fun fromJson(json: JsonObject): Thumbnail {
            val url = json.get("url").asString
            val width = json.get("width").asInt
            val height = json.get("height").asInt

            return Thumbnail(url, width, height, json)
        }
    }

}
