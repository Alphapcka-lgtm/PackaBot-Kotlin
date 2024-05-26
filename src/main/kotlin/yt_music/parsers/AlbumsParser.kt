package yt_music.parsers

import com.google.gson.JsonObject
import yt_music.utils.update
import yt_music.Navigation
import yt_music.toInt

class AlbumsParser : Navigation() {

    private val songsParser = SongsParser()

    fun parseAlbumHeader(response: JsonObject): JsonObject {
        val header = nav(response, HEADER_DETAIL)!!.asJsonObject
        val album: JsonObject = JsonObject().also { json ->
            json.add("title", nav(header, TITLE_TEXT))
            json.add("type", nav(header, SUBTITLE))
            json.add("thumbnails", nav(header, THUMBNAIL_CROPPED))
        }

        if (header.has("description")) {
            album.add(
                "description",
                header.getAsJsonObject("description").getAsJsonArray("runs")
                    .get(0).asJsonObject.getAsJsonPrimitive("text")
            )
        }

        val runs = header.getAsJsonObject("subtitle").getAsJsonArray("runs")
            .also { runs -> /* remove the first two elements */runs.remove(0); runs.remove(0) }
        val albumInfo = songsParser.parseSongRuns(runs)
        album.update(albumInfo)

        if (header.getAsJsonObject("secondSubtitle").getAsJsonArray("runs").size() > 1) {
            val string = header.getAsJsonObject("secondSubtitle").getAsJsonArray("runs")
                .get(0).asJsonObject.getAsJsonPrimitive("text").asString
            album.addProperty(
                "trackCount", toInt(string)// TODO: remove all non number characters from the string
            )

            album.add(
                "duration",
                header.getAsJsonObject("secondSubtitle").getAsJsonArray("runs")
                    .get(2).asJsonObject.getAsJsonPrimitive("text")
            )
        } else {
            album.add(
                "duration",
                header.getAsJsonObject("secondSubtitle").getAsJsonArray("runs")
                    .get(0).asJsonObject.getAsJsonPrimitive("text")
            )
        }

        // add to library/uploaded
        val menu = nav(header, MENU)!!.asJsonObject
        val toplevel = menu.getAsJsonArray("topLevelButtons")
        album.add("audioPlaylistId", nav(toplevel, listOf("0", "buttonRenderer") + NAVIGATION_WATCH_PLAYLIST_ID, true))

        if (album.get("audioPlaylistId") == null) {
            album.add(
                "audioPlaylistId",
                nav(toplevel, listOf("0", "buttonRenderer") + NAVIGATION_PLAYLIST_ID, true)
            )
        }

        val service = nav(toplevel, listOf("1", "buttonRenderer", "defaultServiceEndpoint"), true)
        if (service != null) {
            album.add("likeStatus", songsParser.parseLikeStatus(service.asJsonObject))
        }

        return album
    }
}