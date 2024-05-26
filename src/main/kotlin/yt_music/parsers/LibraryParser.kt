package yt_music.parsers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kong.unirest.HttpResponse
import yt_music.Continuations
import yt_music.utils.getItemText
import yt_music.utils.parseMenuPlaylists
import yt_music.utils.update
import yt_music.Navigation
import kotlin.reflect.KFunction1

class LibraryParser : Navigation() {

    private val songsParser = SongsParser()
    private val continuations = Continuations()
    private val playlistsParser = PlaylistsParser()

    fun parseArtists(results: JsonArray, uploaded: Boolean = false): JsonArray {
        val artists = JsonArray()
        for (_result in results) {
            val result = _result.asJsonObject
            val data = result.getAsJsonObject(MRLIR)
            val artist = JsonObject()
            artist.add("browseId", nav(data, NAVIGATION_BROWSE_ID))
            artist.addProperty("artist", getItemText(data, 0))
            parseMenuPlaylists(data, artist)
            if (uploaded) {
                artist.addProperty("songs", getItemText(data, 1)!!.split(' ')[0])
            } else {
                val subtitle = getItemText(data, 1)
                if (subtitle != null) {
                    artist.addProperty("subscribers", subtitle.split(' ')[0])
                }
            }
            artist.add("thumbnails", nav(data, THUMBNAILS, true))
            artists.add(artist)
        }

        return artists
    }

    fun parseLibraryAlbums(
        response: JsonObject,
        requestFunc: KFunction1<String, HttpResponse<JsonElement>>,
        limit: Int?,
    ): JsonArray {
        val results = getLibraryContents(response, GRID) ?: return JsonArray()

        val albums = parseAlbums(results.asJsonObject.getAsJsonArray("items"))
        if (results.asJsonObject.has("continuations")) {
            fun parseFunc(contents: JsonElement) = parseAlbums(contents.asJsonArray)
            val remainingLimit = if (limit == null) null else limit - albums.size()
            albums.addAll(
                continuations.getContinuations(
                    response.asJsonObject,
                    "gridContinuation",
                    remainingLimit,
                    requestFunc,
                    ::parseFunc
                )
            )
        }

        return albums
    }

    fun parseAlbums(results: JsonArray): JsonArray {
        val albums = JsonArray()
        for (_result in results) {
            val result = _result.asJsonObject
            val data = result.getAsJsonObject(MTRIR)
            val album = JsonObject()
            album.add("browseId", nav(data, TITLE + NAVIGATION_BROWSE_ID))
            album.add("playlistId", nav(data, MENU_PLAYLIST_ID, true))
            album.add("title", nav(data, TITLE_TEXT))
            album.add("thumbnails", nav(data, THUMBNAIL_RENDERER))

            if (data.getAsJsonObject("subtitle").has("runs")) {
                album.add("type", nav(data, SUBTITLE))
                val runsList = data.getAsJsonObject("subtitle").getAsJsonArray("runs").asList()
                val runs = JsonParser.parseString(
                    runsList.subList(2, runsList.size - 1).joinToString(prefix = "[", postfix = "]")
                ).asJsonArray
                album.update(songsParser.parseSongRuns(runs))
            }

            albums.add(album)
        }

        return albums
    }

    fun parseLibraryArtists(
        response: JsonObject,
        requestFunc: KFunction1<String, HttpResponse<JsonElement>>,
        limit: Int?,
    ): JsonArray {
        val results = getLibraryContents(response, MUSIC_SHELF) ?: return JsonArray()

        val artists = parseArtists(results.asJsonObject.getAsJsonArray("contents"))

        if (results.asJsonObject.has("continuations")) {
            fun parseFunc(contents: JsonElement): JsonElement {
                return parseArtists(contents.asJsonArray)
            }

            val remainingLimit = if (limit == null) null else limit - artists.size()
            artists.addAll(
                continuations.getContinuations(
                    results.asJsonObject,
                    "musicShelfContinuation",
                    remainingLimit,
                    requestFunc,
                    ::parseFunc
                )
            )
        }

        return artists
    }

    fun parseLibrarySongs(response: JsonObject): JsonObject {
        val results = getLibraryContents(response, MUSIC_SHELF)
        return JsonObject().also { json ->
            json.add("results", results)
            if (results != null) {
                val contentsList = results.asJsonObject.getAsJsonArray("contents").asList()
                val res = JsonParser.parseString(
                    contentsList.subList(1, contentsList.size - 1).joinToString(prefix = "[", postfix = "]")
                ).asJsonArray
                json.add("parsed", playlistsParser.parsePlaylistItems(res))
            } else {
                json.add("parsed", results)
            }
        }
    }

    /**
     * Find library contents. This function is a bit messy now
     * as it is supporting two different response types. Can be
     * cleaned up once all users are migrated to the new responses.
     * @param response ytmusicapi response
     * @param renderer GRID or MUSIC_SHELF
     * @return: library contents or None
     */
    fun getLibraryContents(response: JsonObject, renderer: Collection<String>): JsonElement? {
        val section = nav(response, SINGLE_COLUMN_TAB + SECTION_LIST, true)
        val contents: JsonElement? = if (section == null) {
            // empty library
            nav(response, SINGLE_COLUMN + TAB_1_CONTENT + SECTION_LIST_ITEM + renderer, true)
        } else {
            val results = findObjectByKey(section.asJsonArray, "itemSectionRenderer")
            if (results == null) {
                nav(response, SINGLE_COLUMN_TAB + SECTION_LIST_ITEM + renderer, true)
            } else {
                nav(results, ITEM_SECTION + renderer, true)
            }
        }

        return contents
    }
}