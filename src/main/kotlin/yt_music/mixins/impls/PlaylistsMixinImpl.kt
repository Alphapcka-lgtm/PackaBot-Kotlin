package yt_music.mixins.impls

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import yt_music.Continuations
import yt_music.enums.PrivacyStatus
import yt_music.mixins.PlaylistsMixin
import yt_music.parsers.BrowsingParser
import yt_music.parsers.PlaylistsParser
import yt_music.pojos.browse.Track
import yt_music.utils.AuthException
import yt_music.utils.collectionToJsonArray
import yt_music.utils.htmlToText
import yt_music.Navigation
import yt_music.Requester
import yt_music.sumTotalDuration
import yt_music.toInt
import java.io.IOException
import kotlin.math.min

class PlaylistsMixinImpl(private val requester: Requester, private val auth: String?) : PlaylistsMixin, Navigation() {

    private val continuations = Continuations()
    private val playlistsParser = PlaylistsParser()
    private val browsingParser = BrowsingParser()

    override fun getPlaylist(playlistId: String, _limit: Int?, related: Boolean, suggestionsLimit: Int): JsonObject {
        var limit = _limit
        val browseId = if (!playlistId.startsWith("VL")) "VL$playlistId" else playlistId
        val body = JsonObject().also { json -> json.addProperty("browseId", browseId) }
        val endpoint = "browse"
        val response = requester.sendRequest(endpoint, body).body
        val results =
            nav(response, SINGLE_COLUMN_TAB + SECTION_LIST_ITEM + listOf("musicPlaylistShelfRenderer"))!!.asJsonObject

        val playlist = JsonObject()
        playlist.add("id", results["playlistId"])
        val ownPlaylist =
            response.asJsonObject.getAsJsonObject("header").has("musicEditablePlaylistDetailHeaderRenderer")

        var header: JsonObject
        if (!ownPlaylist) {
            header = response.asJsonObject.getAsJsonObject("header").getAsJsonObject("musicDetailHeaderRenderer")
            playlist.addProperty("privacy", PrivacyStatus.PUBLIC.statusStr)
        } else {
            header =
                response.asJsonObject.getAsJsonObject("header")
                    .getAsJsonObject("musicEditablePlaylistDetailHeaderRenderer")
            playlist.add(
                "privacy",
                header.asJsonObject.getAsJsonObject("editHeader").getAsJsonObject("musicPlaylistEditHeaderRenderer")
                    .get("privacy")
            )
            header = header.getAsJsonObject("header").getAsJsonObject("musicDetailHeaderRenderer")
        }

        playlist.add("title", nav(header, TITLE_TEXT))
        playlist.add("thumbnails", nav(header, THUMBNAIL_CROPPED))
        playlist.add("description", nav(header, DESCRIPTION, true))
        val runCount = nav(header, SUBTITLE_RUNS)!!.asJsonArray.size()
        if (runCount > 1) {
            val author = JsonObject()
            author.add("name", nav(header, SUBTITLE2))
            author.add("id", nav(header, SUBTITLE_RUNS + "2" + NAVIGATION_BROWSE_ID, true))
            playlist.add("author", author)

            if (runCount == 5) {
                playlist.add("year", nav(header, SUBTITLE3))
            }
        }

        val secondSubtitleRuns = header.getAsJsonObject("secondSubtitle").getAsJsonArray("runs")
        val ownOffset = (if (ownPlaylist && secondSubtitleRuns.size() > 3) 1 else 0) * 2
        val songCount = toInt(secondSubtitleRuns[0].asJsonObject["text"].asString)
        if (secondSubtitleRuns.size() > 1) {
            playlist.add("duration", secondSubtitleRuns[ownOffset + 2].asJsonObject["text"])
        }

        playlist.addProperty("trackCount", songCount)
        playlist.add("views", null)
        if (ownPlaylist) {
            playlist.addProperty("views", toInt(secondSubtitleRuns[0].asJsonObject["text"].asString))
        }

        fun requestFunc(additionalParams: String) = requester.sendRequest(endpoint, body, additionalParams)

        // suggestions and related are missing e.g. on liked songs
        val sectionList = nav(response, SINGLE_COLUMN_TAB + listOf("sectionListRenderer"))!!.asJsonObject
        if (sectionList.has("continuations")) {
            var additionalParams = continuations.getContinuationParams(sectionList)
            if (ownPlaylist && (suggestionsLimit > 0 || related)) {
                fun parseFunc1(results: JsonElement) = playlistsParser.parsePlaylistItems(results.asJsonArray)
                val suggested = requestFunc(additionalParams).body
                val continuation = nav(suggested, SECTION_LIST_CONTINUATION)!!.asJsonObject
                additionalParams = continuations.getContinuationParams(continuation)
                val suggestionsShelf = nav(continuation, CONTENT + MUSIC_SHELF)!!.asJsonObject
                playlist.add("suggestions", continuations.getContinuationContents(suggestionsShelf, ::parseFunc1))

                fun parseFunc2(results: JsonElement) = playlistsParser.parsePlaylistItems(results.asJsonArray)
                playlist.getAsJsonArray("suggestions").asList().addAll(
                    continuations.getContinuations(
                        suggestionsShelf,
                        "musicShelfContinuation",
                        suggestionsLimit - playlist.getAsJsonArray("suggestions").size(),
                        ::requestFunc,
                        ::parseFunc2,
                        reloadable = true
                    )
                )
            }

            if (related) {
                val response = requestFunc(additionalParams).body
                val continuation = nav(response, SECTION_LIST_CONTINUATION)!!
                fun parseFunc(results: JsonElement) =
                    browsingParser.parseContentList(results.asJsonArray, browsingParser::parsePlaylist)
                playlist.add(
                    "related",
                    continuations.getContinuationContents(
                        nav(continuation, CONTENT + CAROUSEL)!!.asJsonObject,
                        ::parseFunc
                    )
                )
            }
        }

        if (songCount > 0) {
            playlist.add("tracks", playlistsParser.parsePlaylistItems(results.getAsJsonArray("contents")))
            if (limit == null) limit = songCount
            val songsToGet = min(limit, songCount)

            fun parseFunc(contents: JsonElement) = playlistsParser.parsePlaylistItems(contents.asJsonArray)
            if (results.has("continuations")) {
                playlist.getAsJsonArray("tracks").asList().addAll(
                    continuations.getContinuations(
                        results,
                        "musicPlaylistShelfContinuation",
                        songsToGet - playlist.getAsJsonArray("tracks").size(),
                        ::requestFunc,
                        ::parseFunc
                    )
                )
            }
        }

        playlist.addProperty("duration_seconds", sumTotalDuration(playlist))
        return playlist
    }

    override fun createPlaylist(
        title: String,
        description: String,
        privacyStatus: PrivacyStatus,
        videoIds: Collection<String>?,
        sourcePlaylist: String?,
    ): String {
        checkAuth()
        val body = JsonObject().also { json ->
            json.addProperty("title", title)
            json.addProperty("description", htmlToText(description)) // YT does not allow html tags
            json.addProperty("privacyStatus", privacyStatus.statusStr)
        }

        if (!videoIds.isNullOrEmpty()) {
            body.add("videoIds", collectionToJsonArray(videoIds))
        }

        if (sourcePlaylist != null) {
            body.addProperty("sourcePlaylistId", sourcePlaylist)
        }

        val endpoint = "playlist/create"
        val response = requester.sendRequest(endpoint, body).body.asJsonObject

        if (!response.has("playlistId")) {
            throw IOException("Error when creating playlist!\nError response: $response")
        }

        return response["playlistId"].asString
    }

    override fun editPlaylist(
        playlistId: String,
        title: String?,
        description: String?,
        privacyStatus: PrivacyStatus?,
        moveItem: Pair<String, String>?,
        addPlaylistId: String?,
    ): JsonObject {
        checkAuth()

        val body = JsonObject().also { json ->
            json.addProperty("playlistId", playlistsParser.validatePlaylistId(playlistId))
        }
        val actions = JsonArray()

        if (title != null) {
            actions.add(JsonObject().also { json ->
                json.addProperty("action", "ACTION_SET_PLAYLIST_NAME")
                json.addProperty("playlistName", title)
            })
        }
        if (description != null) {
            actions.add(JsonObject().also { json ->
                json.addProperty("action", "ACTION_SET_PLAYLIST_DESCRIPTION")
                json.addProperty("playlistDescription", description)
            })
        }
        if (privacyStatus != null) {
            actions.add(JsonObject().also { json ->
                json.addProperty("action", "ACTION_SET_PLAYLIST_PRIVACY")
                json.addProperty("playlistPrivacy", privacyStatus.statusStr)
            })
        }
        if (moveItem != null) {
            actions.add(JsonObject().also { json ->
                json.addProperty("action", "ACTION_MOVE_VIDEO_BEFORE")
                json.addProperty("setVideoId", moveItem.first)
                json.addProperty("movedSetVideoIdSuccessor", moveItem.second)
            })
        }
        if (addPlaylistId != null) {
            actions.add(JsonObject().also { json ->
                json.addProperty("action", "ACTION_ADD_PLAYLIST")
                json.addProperty("addedFullListId", addPlaylistId)
            })
        }

        body.add("actions", actions)
        val endpoint = "browse/edit_playlist"
        val response = requester.sendRequest(endpoint, body).body

        if (!response.asJsonObject.has("status")) {
            throw IOException("Error when editing playlist!\nError: $response")
        }

        return response.asJsonObject.getAsJsonObject("status")
    }

    override fun deletePlaylist(playlistId: String): String {
        checkAuth()
        val body = JsonObject().also { json ->
            json.addProperty("playlistId", playlistsParser.validatePlaylistId(playlistId))
        }
        val endpoint = "playlist/delete"
        val response = requester.sendRequest(endpoint, body).body
        if (!response.asJsonObject.has("status")) {
            throw IOException("Error when deleting playlist!\nError: $response")
        }

        return response.asJsonObject.get("status").asString
    }

    override fun addPlaylistItem(
        playlistId: String,
        videoIds: Collection<String>?,
        sourcePlaylist: String?,
        duplicates: Boolean,
    ): JsonObject {
        checkAuth()
        val body = JsonObject().also { json ->
            json.addProperty("playlistId", playlistsParser.validatePlaylistId(playlistId))
            json.add("actions", JsonArray())
        }
        if (videoIds == null && sourcePlaylist == null) {
            throw IllegalArgumentException("You must provide either videoIds or a source_playlist to add to the playlist")
        }

        if (videoIds != null) {
            for (videoId in videoIds) {
                val action = JsonObject().also { json ->
                    json.addProperty("action", "ACTION_ADD_VIDEO")
                    json.addProperty("addedVideoId", videoId)
                }
                if (duplicates) {
                    action.addProperty("dedupeOption", "DEDUPE_OPTION_SKIP")
                }
                body.getAsJsonArray("actions").add(action)
            }
        }

        if (sourcePlaylist != null) {
            var action = JsonObject().also { json ->
                json.addProperty("action", "ACTION_ADD_PLAYLIST")
                json.addProperty("addedFullListId", sourcePlaylist)
            }
            body.getAsJsonArray("actions").add(action)

            /*
             * add an empty ACTION_ADD_VIDEO because otherwise YTM doesn't
             * return the dict that maps videoIds to their new setVideoIds
             */
            if (videoIds == null) {
                action = JsonObject().also { json ->
                    json.addProperty("action", "ACTION_ADD_VIDEO")
                    json.add("addedVideoId", null)
                }
                body.getAsJsonArray("actions").add(action)
            }
        }

        val endpoint = "browse/edit_playlist"
        val response = requester.sendRequest(endpoint, body).body.asJsonObject
        if (response.has("status") && response.getAsJsonObject("status").has("SUCCEEDED")) {
            val resultJson = JsonArray()
            val editResults =
                if (response.has("playlistEditResults")) response.getAsJsonArray("playlistEditResults") else JsonArray()
            for (resultData in editResults) {
                resultJson.add(resultData.asJsonObject.get("playlistEditVideoAddedResultData"))
            }

            return JsonObject().also { json ->
                json.add("status", response.get("status"))
                json.add("playlistEditResults", resultJson)
            }
        }

        return response
    }

    override fun removePlaylistItems(playlistId: String, videos: Collection<Track>): String {
        checkAuth()
        val filteredVideos =
            videos.stream().filter { track -> track.json.has("videoId") && track.json.has("setVideoId") }.toList()

        if (filteredVideos.isEmpty()) {
            throw NoSuchElementException("Cannot remove songs, because setVideoId is missing. Do you own this playlist?")
        }

        val body = JsonObject().also { json ->
            json.addProperty("playlistId", playlistsParser.validatePlaylistId(playlistId))
            json.add("actions", JsonArray())
        }
        for (video in filteredVideos) {
            val action = JsonObject()
            action.addProperty("setVideoId", video.setVideoId)
            action.addProperty("removedVideoId", video.videoId)
            action.addProperty("action", "ACTION_REMOVE_VIDEO")
            body.getAsJsonArray("actions").add(action)
        }

        val endpoint = "browse/edit_playlist"
        val response = requester.sendRequest(endpoint, body).body.asJsonObject
        if (!response.has("status")) {
            throw IOException("Error when removing items from playlist!\nError: $response")
        }

        return response["status"].asString
    }

    private fun checkAuth() {
        if (auth == null) {
            throw AuthException("Please provide authentication before using this function.")
        }
    }
}