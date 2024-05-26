package yt_music.pojos

import com.google.gson.JsonObject

data class Artist(val name: String, val id: String?, val json: JsonObject) {

    companion object {
        fun fromJson(json: JsonObject): Artist {
            val name = json.get("name").asString
            val id = if (json.get("id").isJsonNull) null else json.get("id").asString

            return Artist(name, id, json)
        }
    }

}
