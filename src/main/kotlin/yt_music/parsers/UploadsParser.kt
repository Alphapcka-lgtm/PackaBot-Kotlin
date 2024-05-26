package yt_music.parsers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import yt_music.utils.getFixedColumnItem
import yt_music.utils.getItemText
import yt_music.utils.parseDuration
import yt_music.Navigation

class UploadsParser : Navigation() {

    private val songsParser = SongsParser()

    fun parseUploadedItems(results: JsonArray): JsonArray {
        val songs = JsonArray()
        for (result in results) {
            val data = result.asJsonObject.getAsJsonObject(MRLIR)
            if (!data.has("menu")) continue
            val entityId =
                nav(data, MENU_ITEMS)!!.asJsonArray.last().asJsonObject.getAsJsonObject("menuNavigationItemRenderer")
                    .getAsJsonObject("navigationEndpoint").getAsJsonObject("confirmDialogEndpoint")
                    .getAsJsonObject("content").getAsJsonObject("confirmDialogRenderer")
                    .getAsJsonObject("confirmButton").getAsJsonObject("buttonRenderer").getAsJsonObject("command")
                    .getAsJsonObject("musicDeletePrivatelyOwnedEntityCommand")["entityId"].asString
            val videoId =
                nav(data, MENU_ITEMS + listOf("0") + MENU_SERVICE)!!.asJsonObject.getAsJsonObject("queueAddEndpoint")
                    .getAsJsonObject("queueTarget")["videoId"].asString

            val title = getItemText(data, 0)
            val like = nav(data, MENU_LIKE_STATUS)
            val thumbnails = if (data.has("thumbnail")) nav(data, THUMBNAILS) else null
            val duration = getFixedColumnItem(data, 0)!!.getAsJsonObject("text")
                .getAsJsonArray("runs")[0].asJsonObject["text"].asString

            val song = JsonObject()
            song.addProperty("entityId", entityId)
            song.addProperty("videoId", videoId)
            song.addProperty("title", title)
            song.addProperty("duration", duration)
            song.addProperty("duration_seconds", parseDuration(duration))
            song.add("artists", songsParser.parseSongArtists(data, 1))
            song.add("album", songsParser.parseSongAlbum(data, 2))
            song.add("likeStatus", like)
            song.add("thumbnails", thumbnails)

            songs.add(song)
        }
        return songs
    }

}