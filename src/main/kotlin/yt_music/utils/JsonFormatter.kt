package yt_music.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement

object JsonFormatter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun formatToPrettyString(json: JsonElement): String {
        return gson.toJson(json)
    }
}