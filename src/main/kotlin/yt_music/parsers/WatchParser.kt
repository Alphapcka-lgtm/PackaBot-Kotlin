package yt_music.parsers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import yt_music.utils.update
import yt_music.Navigation

class WatchParser : Navigation() {

    private val songsParser = SongsParser()

    fun parseWatchPlaylist(results: JsonArray): JsonArray {
        val tracks = JsonArray()
        val ppvwr = "playlistPanelVideoWrapperRenderer"
        val ppvr = "playlistPanelVideoRenderer"
        for (result in results) {
            var _result = result.asJsonObject
            var counterpart: JsonObject? = null
            if (_result.has(ppvr)) {
                counterpart =
                    _result[ppvr].asJsonObject["counterpart"].asJsonArray[0].asJsonObject["counterpartRenderer"].asJsonObject[ppvr].asJsonObject
                _result = _result[ppvwr].asJsonObject["primaryReader"].asJsonObject
            }
            if (!_result.has(ppvr)) {
                continue
            }
            val data = _result[ppvr].asJsonObject
            val track = parseWatchTrack(data)
            if (counterpart != null) {
                track.add("counterpart", parseWatchTrack(counterpart))
            }
            tracks.add(track)
        }

        return tracks
    }

    fun parseWatchTrack(data: JsonObject): JsonObject {
        var feedbackTokens: JsonObject? = null
        var likeStatus: JsonElement? = null
        for (item in nav(data, MENU_ITEMS)!!.asJsonArray) {
            if (item.asJsonObject.has(TOGGLE_MENU)) {
                val service = item.asJsonObject[TOGGLE_MENU].asJsonObject["defaultServiceEndpoint"].asJsonObject
                if (service.has("feedbackEndpoint")) {
                    feedbackTokens = songsParser.parseSongMenuTokens(item.asJsonObject)
                }
                if (service.has("likeEndpoint")) {
                    likeStatus = songsParser.parseLikeStatus(service)
                }
            }
        }

        val songInfo = songsParser.parseSongRuns(data["longByLineText"].asJsonObject["runs"].asJsonArray)

        val track = JsonObject().also { json ->
            json.add("videoId", data["videoId"])
            json.add("title", nav(data, TITLE_TEXT))
            json.add("length", nav(data, listOf("lengthText", "runs", "0", "text"), true))
            json.add("thumbnail", nav(data, THUMBNAIL))
            json.add("feedbackTokens", feedbackTokens)
            json.add("likeStatus", likeStatus)
            json.add("videoType", nav(data, listOf("navigationEndpoint") + NAVIGATION_VIDEO_TYPE, true))
        }
        track.update(songInfo)
        return track
    }

    fun getTabBrowseId(watchNextRenderer: JsonObject, tabId: Int): JsonElement? {
        if (!watchNextRenderer.getAsJsonArray("tabs")[tabId].asJsonObject.getAsJsonObject("tabRenderer")
                .has("unselectable")
        ) {
            return watchNextRenderer.getAsJsonArray("tabs")[tabId].asJsonObject.getAsJsonObject("tabRenderer")
                .getAsJsonObject("endpoint").getAsJsonObject("browseEndpoint")["browseId"]
        }

        return null
    }
}