package yt_music.mixins.impls

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import yt_music.Continuations
import yt_music.mixins.WatchMixin
import yt_music.parsers.PlaylistsParser
import yt_music.parsers.WatchParser
import yt_music.Navigation
import yt_music.Requester

class WatchMixinImpl(private val requester: Requester) : WatchMixin, Navigation() {

    private val playlistsParser = PlaylistsParser()
    private val continuations = Continuations()
    private val watchParser = WatchParser()

    override fun getWatchPlaylist(
        videoId: String?,
        playlistId: String?,
        limit: Int,
        radio: Boolean,
        shuffle: Boolean,
    ): JsonObject {
        var _playlistId = playlistId
        val body = JsonObject().also { json ->
            json.addProperty("enablePersistentPlaylistPanel", true)
            json.addProperty("isAudioOnly", true)
            json.addProperty("tunerSettingValue", "AUTOMIX_SETTING_NORMAL")
        }

        if (videoId == null && playlistId == null) {
            throw IllegalArgumentException("You must provide either a video id, a playlist id or both.")
        }

        if (videoId != null) {
            body.addProperty("videoId", videoId)
            if (_playlistId == null) {
                _playlistId = "RDAMVM$videoId"
            }
            if (!radio || !shuffle) {
                val watchEndpointMusicSupportedConfig = JsonObject()
                watchEndpointMusicSupportedConfig.addProperty("hasPersistentPlaylistPanel", true)
                watchEndpointMusicSupportedConfig.addProperty("musicVideoType", "MUSIC_VIDEO_TYPE_ATV")
                body.add(
                    "watchEndpointMusicSupportedConfigs",
                    JsonObject().also { json ->
                        json.add(
                            "watchEndpointMusicConfig",
                            watchEndpointMusicSupportedConfig
                        )
                    })
            }
        }
        body.addProperty("playlistId", _playlistId?.let { playlistsParser.validatePlaylistId(it) })
        val isPlaylist =
            body["playlistId"].asString.startsWith("PL") or body["playlistId"].asString.startsWith("OLA")

        if (shuffle && playlistId != null) {
            body.addProperty("params", "wAEB8gECKAE%3D")
        }
        if (radio) {
            body.addProperty("params", "wAEB")
        }

        val endpoint = "next"
        val response = requester.sendRequest(endpoint, body).body
        val watchNextRenderer = nav(
            response, listOf(
                "contents", "singleColumnMusicWatchNextResultsRenderer",
                "tabbedRenderer",
                "watchNextTabbedResultsRenderer"
            )
        )!!.asJsonObject

        val lyricsBrowseId = watchParser.getTabBrowseId(watchNextRenderer, 1)
        val relatedBrowseId = watchParser.getTabBrowseId(watchNextRenderer, 2)

        val results = nav(
            watchNextRenderer, TAB_CONTENT + listOf(
                "musicQueueRenderer", "content",
                "playlistPanelRenderer"
            )
        )!!.asJsonObject

        val playlist = results.getAsJsonArray("contents").asList().stream()
            .map { el -> nav(el, listOf("playlistPanelVideoRenderer") + NAVIGATION_PLAYLIST_ID, true) }
            .toList().filterNotNull().first()

        val tracks = watchParser.parseWatchPlaylist(results.getAsJsonArray("contents"))

        if (results.has("continuations")) {
            fun requestFunc(additionalParams: String) = requester.sendRequest(endpoint, body, additionalParams)
            fun parseFunc(contents: JsonElement): JsonElement = watchParser.parseWatchPlaylist(contents.asJsonArray)
            tracks.addAll(
                continuations.getContinuations(
                    results,
                    "playlistContinuation",
                    limit - tracks.size(),
                    ::requestFunc,
                    ::parseFunc,
                    if (isPlaylist) "" else "Radio"
                )
            )
        }

        return JsonObject().also { ret ->
            ret.add("tracks", tracks)
            ret.addProperty("playlistId", _playlistId)
            ret.add("lyrics", lyricsBrowseId)
            ret.add("related", relatedBrowseId)
        }

    }
}