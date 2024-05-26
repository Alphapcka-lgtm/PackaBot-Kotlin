package yt_music.mixins.impls

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import yt_music.Continuations
import yt_music.mixins.UploadsMixin
import yt_music.parsers.LibraryParser
import yt_music.parsers.UploadsParser
import yt_music.utils.AuthException
import yt_music.utils.OrderParameters
import yt_music.Navigation
import yt_music.Requester

class UploadsMixinImpl(private val requester: Requester, private val auth: String?) : UploadsMixin, Navigation() {

    private val libraryParser = LibraryParser()
    private val uploadsParser = UploadsParser()
    private val continuations = Continuations()

    private fun checkAuth() {
        if (auth == null) {
            throw AuthException("Please provide authentication before using this function.")
        }
    }

    override fun getLibraryUploadSongs(limit: Int?, order: OrderParameters?): Collection<JsonObject> {
        checkAuth()
        val endpoint = "browse"
        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_library_privately_owned_tracks") }
        if (order != null) {
            body.addProperty("params", order.orderParam)
        }

        val response = requester.sendRequest(endpoint, body).body.asJsonObject
        val results = libraryParser.getLibraryContents(response, MUSIC_SHELF)?.asJsonObject ?: return emptyList()
        val contents = results.getAsJsonArray("contents")
        contents.asList().removeFirst()
        val songs = uploadsParser.parseUploadedItems(contents)

        if (results.has("continuations")) {
            fun requestFunc(additionalParams: String) = requester.sendRequest(endpoint, body, additionalParams)
            fun parseFunc(results: JsonElement): JsonElement = uploadsParser.parseUploadedItems(results.asJsonArray)
            val remainingLimit = if (limit == null) null else (limit - songs.size())
            continuations.getContinuations(
                results,
                "musicShelfContinuation",
                remainingLimit,
                ::requestFunc,
                ::parseFunc
            )
        }

        return songs.asList().stream().map { el -> el.asJsonObject }.toList()
    }

    override fun getLibraryUploadAlbums(limit: Int?, order: OrderParameters?): Collection<JsonObject> {
        checkAuth()
        val body =
            JsonObject().also { json -> json.addProperty("browseId", "FEmusic_library_privately_owned_releases") }
        if (order != null) {
            body.addProperty("params", order.orderParam)
        }
        val endpoint = "browse"
        val response = requester.sendRequest(endpoint, body).body.asJsonObject
        fun requestFunc(additionalParams: String) = requester.sendRequest(endpoint, body, additionalParams)
        return libraryParser.parseLibraryAlbums(response, ::requestFunc, limit).asList().stream()
            .map { el -> el.asJsonObject }.toList()
    }

    override fun getLibraryUploadArtists(limit: Int?, order: OrderParameters?): Collection<JsonObject> {
        checkAuth()
        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_library_privately_owned_artists") }
        if (order != null) {
            body.addProperty("params", order.orderParam)
        }
        val endpoint = "browse"
        val response = requester.sendRequest(endpoint, body).body.asJsonObject
        fun requestFunc(additionalParams: String) = requester.sendRequest(endpoint, body, additionalParams)
        return libraryParser.parseLibraryArtists(response, ::requestFunc, limit).asList().stream()
            .map { el -> el.asJsonObject }.toList()
    }

    // TODO with getLibraryUploadArtist

}