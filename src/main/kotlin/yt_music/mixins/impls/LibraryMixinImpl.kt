package yt_music.mixins.impls

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import yt_music.Continuations
import yt_music.enums.Order
import yt_music.enums.Rating
import yt_music.mixins.LibraryMixin
import yt_music.parsers.BrowsingParser
import yt_music.parsers.LibraryParser
import yt_music.parsers.PlaylistsParser
import yt_music.Navigation
import yt_music.Requester

class LibraryMixinImpl(private val requester: Requester) : LibraryMixin, Navigation() {

    private val browsingParser = BrowsingParser()
    private val libraryParser = LibraryParser()
    private val playlistsParser = PlaylistsParser()
    private val continuations = Continuations()

    override fun getLibraryPlaylists(limit: Int?): JsonArray {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_liked_playlists") }
//        val endpoint = "browse"
//        val response = requester.sendRequest(endpoint, body).body.asJsonObject
//
//        val results = libraryParser.getLibraryContents(response, GRID)!!.asJsonObject
//        val items = results.getAsJsonArray("items")
//        items.remove(0)
//        val playlists =
//            browsingParser.parseContentList(items, browsingParser::parsePlaylist)
//
//        if (results.has("continuations")) {
//            fun requestFunc(additionalParams: String) = requester.sendRequest(endpoint, body, additionalParams)
//            fun parseFunc(contents: JsonElement): JsonElement =
//                browsingParser.parseContentList(contents.asJsonArray, browsingParser::parsePlaylist)
//
//            val remainingLimit = if (limit == null) {
//                null
//            } else {
//                limit - playlists.size()
//            }
//
//            playlists.addAll(
//                continuations.getContinuations(
//                    results,
//                    "gridContinuation",
//                    remainingLimit,
//                    ::requestFunc,
//                    ::parseFunc
//                )
//            )
//        }
//
//        return playlists
    }

    override fun getLibrarySongs(limit: Int?, validateResponses: Boolean, order: Order?): JsonArray {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_liked_videos") }
//        if (order != null) {
//            body.addProperty("params", order.orderParam)
//        }
//        val endpoint = "browse"
//        val perPage = 25
//
//        fun requestFunc(additionalParams: String) = requester.sendRequest(endpoint, body, additionalParams)
//        fun parseFunc(rawResponse: JsonObject) = libraryParser.parseLibrarySongs(rawResponse)
//
//        if (validateResponses && limit == null) {
//            throw IllegalArgumentException("Validation is not supported without a limit parameter.")
//        }
//
//        val response = if (validateResponses) {
//            fun validateFunc(parsed: JsonObject) = continuations.validateResponse(parsed, perPage, limit ?: 0, 0)
//            continuations.resendRequestUntilParsedResponseIsValid(::requestFunc, "", ::parseFunc, ::validateFunc, 3)
//        } else {
//            parseFunc(requestFunc("").body.asJsonObject)
//        }
//
//        val results = response.getAsJsonObject("results")
//        val songs = response.getAsJsonArray("parsed")
//        if (songs != null) {
//            return JsonArray()
//        }
//
//        if (results.has("continuations")) {
//            fun requestContinuationsFunc(additionalParams: String) =
//                requester.sendRequest(endpoint, body, additionalParams)
//
//            fun parseContinuationsFunc(contents: JsonArray) = playlistsParser.parsePlaylistItems(contents)
//
//            if (validateResponses) {
//                continuations.getValidatedContinuations(
//                    results,
//                    "musicShelfContinuation",
//                    limit - songs.size(),
//                    perPage,
//                    ::requestContinuationsFunc,
//                    ::parseContinuationsFunc //TODO
//                )
//            }
//        }
    }

    override fun getLibraryAlbums(limit: Int, order: Order?): JsonArray {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_liked_albums") }
//        if (order != null) {
//            body.addProperty("params", order.orderParam)
//        }
//        if (order != null) {
//            body.addProperty("params", order.orderParam)
//        }
//        val endpoint = "browse"
//        val response = requester.sendRequest(endpoint, body).body
//        fun requestFunc(additionalParams: String) = requester.sendRequest(endpoint, body, additionalParams)
//        return libraryParser.parseLibraryAlbums(response.asJsonObject, ::requestFunc, limit)
    }

    override fun getLibraryArtists(limit: Int, order: Order?): JsonArray {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_library_corpus_track_artists") }
//        if (order != null) {
//            body.addProperty("params", order.orderParam)
//        }
//        val endpoint = "browse"
//        val response = requester.sendRequest(endpoint, body).body.asJsonObject
//        fun requestFunc(additionalParams: String): HttpResponse<JsonElement> {
//            return requester.sendRequest(endpoint, body, additionalParams)
//        }
//        return libraryParser.parseLibraryArtists(response, ::requestFunc, limit)
    }

    override fun getLibrarySubscriptions(limit: Int, order: Order?): JsonArray {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_library_corpus_track_artists") }
//        if (order != null) {
//            body.addProperty("params", order.orderParam)
//        }
//        val endpoint = "browse"
//        val response = requester.sendRequest(endpoint, body).body.asJsonObject
//        fun requestFunc(additionalParams: String): HttpResponse<JsonElement> {
//            return requester.sendRequest(endpoint, body, additionalParams)
//        }
//        return libraryParser.parseLibraryArtists(response, ::requestFunc, limit)
    }

    override fun getLikedSongs(limit: Int): JsonObject {
        TODO("Needs PlaylistMixin which is not yet implemented.")
//        return getPlaylist("LM", limit)
    }

    override fun getHistory(): JsonArray {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_history") }
//        val endpoint = "browse"
//        val response = requester.sendRequest(endpoint, body).body
//        val results = nav(response, SINGLE_COLUMN_TAB + SECTION_LIST)!!.asJsonArray
//        val songs = JsonArray()
//        for (content in results) {
//            val data = nav(content, MUSIC_SHELF + listOf("contents"), true)
//            if (data == null) {
//                val error = nav(content, MUSIC_SHELF + listOf("musicNotifierShelfRenderer") + TITLE, true)
//                throw Exception(error?.asString ?: null)
//            }
//            val menuEntries = listOf(listOf("-1") + MENU_SERVICE + FEEDBACK_TOKEN)
//            val songList = playlistsParser.parsePlaylistItems(data.asJsonArray, menuEntries)
//            for (song in songList) {
//                song.asJsonObject.add("played", nav(content.asJsonObject.get("musicShelfRenderer"), TITLE_TEXT))
//            }
//            songs.addAll(songList)
//        }
//
//        return songs
    }

    override fun addHistoryItem(song: JsonObject): String {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val url =
//            song.getAsJsonObject("playbackTracking").getAsJsonObject("videostatsPlaybackUrl")
//                .getAsJsonPrimitive("baseUrl").asString
//        val cpna = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
//        var cpn = ""
//        for (r in 0 until 16) {
//            val i = Random.nextInt(0..256)
//            cpn += cpna[i and 63]
//        }
//        val params = JsonObject().also { params ->
//            params.addProperty("ver", 2)
//            params.addProperty("c", "WEB_REMIX")
//            params.addProperty("cpn", cpn)
//        }
//        return requester.sendGetRequest(url, params.asMap()).body
    }

    override fun removeHistoryItems(feedbackTokens: Collection<String>): JsonObject {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json -> json.add("feedbackTokens", collectionToJsonArray(feedbackTokens)) }
//        val endpoint = "feedback"
//        return requester.sendRequest(endpoint, body).body.asJsonObject
    }

    override fun rateSong(videoId: String, rating: Rating): JsonObject {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json ->
//            val target = JsonObject().also { target -> target.addProperty("videoId", videoId) }
//            json.add("target", target)
//        }
//        val endpoint = rating.endpoint
//        return requester.sendRequest(endpoint, body).body.asJsonObject
    }

    override fun editSongLibraryStatus(feedbackTokens: Collection<String>?): JsonObject {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json ->
//            if (feedbackTokens == null) {
//                json.add("feedbackTokens", null)
//            } else {
//                json.add("feedbackTokens", collectionToJsonArray(feedbackTokens))
//            }
//        }
//        val endpoint = "feedback"
//        return requester.sendRequest(endpoint, body).body.asJsonObject
    }

    override fun ratePlaylist(playlistId: String, rating: Rating): JsonObject {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json ->
//            val target = JsonObject()
//            target.addProperty("playlistId", playlistId)
//            json.add("target", target)
//        }
//        val endpoint = rating.endpoint
//        return requester.sendRequest(endpoint, body).body.asJsonObject
    }

    override fun subscribeArtists(channelIds: Collection<String>): JsonObject {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json -> json.add("channelIds", collectionToJsonArray(channelIds)) }
//        val endpoint = "subscription/subscribe"
//        return requester.sendRequest(endpoint, body).body.asJsonObject
    }

    override fun unsubscribeArtists(channelIds: Collection<String>): JsonObject {
        TODO("Currently not yet functional! Requires Authentication which is not yet implemented!")
//        val body = JsonObject().also { json -> json.add("channelIds", collectionToJsonArray(channelIds)) }
//        val endpoint = "subscription/unsubscribe"
//        return requester.sendRequest(endpoint, body).body.asJsonObject
    }
}