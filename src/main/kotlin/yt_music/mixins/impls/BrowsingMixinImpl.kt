package yt_music.mixins.impls

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kong.unirest.HttpResponse
import yt_music.Continuations
import yt_music.YTM_DOMAIN
import yt_music.i18n.I18nParser
import yt_music.mixins.BrowsingMixin
import yt_music.parsers.AlbumsParser
import yt_music.parsers.BrowsingParser
import yt_music.parsers.LibraryParser
import yt_music.parsers.PlaylistsParser
import yt_music.pojos.browse.Album
import yt_music.utils.getDatestamp
import yt_music.utils.update
import yt_music.Navigation
import yt_music.Requester
import yt_music.sumTotalDuration


open class BrowsingMixinImpl(private val requester: Requester) : BrowsingMixin, Navigation() {

    private val albumsParser = AlbumsParser()
    private val browsingParser = BrowsingParser()
    private val continuations = Continuations()
    private val playlistsParser = PlaylistsParser()
    private val libraryParser = LibraryParser()
    private val i18nParser = I18nParser()

    private val endpoint = "browse"

    override fun getHome(limit: Int): JsonArray {
        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_home") }
        val response = requester.sendRequest(endpoint, body)
        val json = JsonParser.parseString(response.body.toString())
        val results = nav(json, SINGLE_COLUMN_TAB + SECTION_LIST)!!
        val home = JsonArray()
        home.addAll(browsingParser.parseMixedContent(results.asJsonArray))

        val sectionList = nav(json, SINGLE_COLUMN_TAB + listOf("sectionListRenderer"))!!.asJsonObject
        if (sectionList.has("continuations")) {
            fun requestFunc(additionalParams: String): HttpResponse<JsonElement> {
                return requester.sendRequest(endpoint, body, additionalParams)
            }

            fun parseFunc(contents: JsonElement): JsonElement {
                return browsingParser.parseMixedContent(contents.asJsonArray)
            }

            val conts = continuations.getContinuations(
                sectionList,
                "sectionListContinuation",
                limit - home.size(),
                ::requestFunc,
                ::parseFunc
            )
            home.addAll(conts)
        }

        return home
    }

    override fun getArtist(channelId_: String): JsonObject {
        var channelId = channelId_
        if (channelId.startsWith("MPLA")) {
            channelId = channelId.removeRange(0, 4)
        }

        val body = JsonObject().also { json -> json.addProperty("browseId", channelId) }
        val response = requester.sendRequest(endpoint, body)
        val json = JsonParser.parseString(response.body.toString()).asJsonObject
        val results = nav(json, SINGLE_COLUMN_TAB + SECTION_LIST)!!.asJsonArray

        val artist = JsonObject().also { json ->
            json.add("description", null)
            json.add("views", null)
        }

        val header = json.getAsJsonObject("header").get("musicImmersiveHeaderRenderer").asJsonObject
        artist.add("name", nav(header, TITLE_TEXT))
        val descriptionShelf = findObjectByKey(results, DESCRIPTION_SHELF[0], isKey = true)
        if (descriptionShelf != null) {
            artist.add("description", nav(descriptionShelf, DESCRIPTION)!!)
            if (descriptionShelf.asJsonObject.has("subheader")) {
                artist.add(
                    "views",
                    descriptionShelf.asJsonObject.getAsJsonObject("subheader").getAsJsonArray("runs")
                        .get(0).asJsonObject.get("text")
                )
            } else {
                artist.add("views", null)
            }
        }

        val subscriptionButton = header.getAsJsonObject("subscriptionButton").getAsJsonObject("subscribeButtonRenderer")
        artist.add("channelId", subscriptionButton.get("channelId"))
        artist.add(
            "shuffleId",
            nav(header, listOf("playButton", "buttonRenderer") + NAVIGATION_WATCH_PLAYLIST_ID, true)
        )
        artist.add(
            "radioId",
            nav(header, listOf("startRadioButton", "buttonRenderer") + NAVIGATION_WATCH_PLAYLIST_ID, true)
        )
        artist.add("subscribers", nav(subscriptionButton, listOf("subscriberCountText", "runs", "0", "text"), true))
        artist.add("subscribed", subscriptionButton.get("subscribed"))
        artist.add("songs", JsonObject().also { jsonObject -> jsonObject.add("browseId", null) })
        if (results[0].asJsonObject.has("musicShelfRenderer")) {
            // API sometimes does not return songs
            val musicShelf = nav(results[0], MUSIC_SHELF)!!
            if (nav(musicShelf, TITLE)!!.asJsonObject.has("navigationEndpoint")) {
                artist.getAsJsonObject("songs").add("browseId", nav(musicShelf, TITLE + NAVIGATION_BROWSE_ID)!!)
            }
            artist.getAsJsonObject("songs")
                .add("results", playlistsParser.parsePlaylistItems(musicShelf.asJsonObject.getAsJsonArray("contents")))
        }
        artist.update(i18nParser.parseArtistContents(results))
        return artist
    }

    override fun getArtistAlbums(channelId: String, params: String): JsonArray {
        val body = JsonObject().also { json ->
            json.addProperty("browseId", channelId)
            json.addProperty("params", params)
        }
        val response = requester.sendRequest(endpoint, body)
        val json = JsonParser.parseString(response.body.toString())
        val results = nav(json, SINGLE_COLUMN_TAB + SECTION_LIST_ITEM + GRID_ITEMS)!!.asJsonArray
        return libraryParser.parseAlbums(results)
    }

    override fun getUser(channelId: String): JsonObject {
        val body = JsonObject().also { json -> json.addProperty("browseId", channelId) }
        val response = JsonParser.parseString(requester.sendRequest(endpoint, body).body.toString())
        val user = JsonObject().also { json ->
            json.add(
                "name",
                nav(response, listOf("header", "musicVisualHeaderRenderer") + TITLE_TEXT)
            )
        }
        val results = nav(response, SINGLE_COLUMN_TAB + SECTION_LIST)!!
        user.update(i18nParser.parseArtistContents(results.asJsonArray))
        return user
    }

    override fun getUserPlaylists(channelId: String, params: String): JsonArray {
        val body = JsonObject().also { json ->
            json.addProperty("browseId", channelId)
            json.addProperty("params", params)
        }

        val response = requester.sendRequest(endpoint, body).body
        val results = nav(response, SINGLE_COLUMN_TAB + SECTION_LIST_ITEM + GRID_ITEMS)!!.asJsonArray

        return browsingParser.parseContentList(results, browsingParser::parsePlaylist)
    }

    override fun getAlbumBrowseId(audioPlaylistId: String): String? {
        val params = JsonObject().also { json -> json.addProperty("list", audioPlaylistId) }
        val response = requester.sendGetRequest(YTM_DOMAIN + "/playlist", params.asMap())
        val matches = "\"MPRE.+?\"".toRegex().findAll(response.body).toList()
        var browseId: String? = null
        if (matches.size > 0) {
            browseId = matches[0].value.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8)
        }
        return browseId
    }

    override fun getAlbum(browseId: String): Album {
        val body = JsonObject().also { json -> json.addProperty("browseId", browseId) }
        val response = requester.sendRequest(endpoint, body).body.asJsonObject
        val album = albumsParser.parseAlbumHeader(response)
        var results = nav(response, SINGLE_COLUMN_TAB + SECTION_LIST_ITEM + MUSIC_SHELF)
        album.add("tracks", playlistsParser.parsePlaylistItems(results!!.asJsonObject.getAsJsonArray("contents")))
        results = nav(response, SINGLE_COLUMN_TAB + SECTION_LIST + listOf("1") + CAROUSEL, true)
        if (results != null) {
            album.add(
                "other_versions",
                browsingParser.parseContentList(
                    results.asJsonObject.getAsJsonArray(
                        "contents"
                    ),
                    browsingParser::parseAlbum
                )
            )
        }
        album.addProperty("duration_seconds", sumTotalDuration(album))
        album.getAsJsonArray("tracks").asList().forEachIndexed { i, track ->
            album.getAsJsonArray("tracks")[i].asJsonObject.add("album", album.get("title"))
            album.getAsJsonArray("tracks")[i].asJsonObject.add("artists", album.get("artists"))
        }

        return Album.fromJson(album)
    }

    override fun getSong(videoId: String, signatureTimestamp_: Long?): JsonObject {
        var signatureTimestamp = signatureTimestamp_
        // this request uses another endpoint
        val endpoint = "player"
        if (signatureTimestamp_ == null) {
            signatureTimestamp = getDatestamp()
        }

        val params = JsonObject().also { json ->
            json.add("playbackContext", JsonObject().also { playbackContext ->
                playbackContext.addProperty("signatureTimestamp", signatureTimestamp)
            })
            json.addProperty("video_id", videoId)
        }

        val response = requester.sendRequest(endpoint, params).body.asJsonObject
        val keys = arrayOf("videoDetails", "playabilityStatus", "streamingData", "microformat", "playbackTracking")
        for (k in response.keySet()) {
            if (!keys.contains(k)) {
                response.remove(k)
            }
        }
        return response
    }

    override fun getSongRelated(browseId: String): JsonArray {
        val body = JsonObject().also { json -> json.addProperty("browseId", browseId) }
        val response = requester.sendRequest(endpoint, body).body
        val sections = nav(response, listOf("contents") + SECTION_LIST)!!.asJsonArray
        return browsingParser.parseMixedContent(sections)
    }

    override fun getLyrics(browseId: String): JsonObject {
        val lyrics = JsonObject()
        val body = JsonObject().also { json -> json.addProperty("browseId", browseId) }
        val response = requester.sendRequest(endpoint, body).body
        lyrics.add(
            "lyrics",
            nav(response, listOf("contents") + SECTION_LIST_ITEM + DESCRIPTION_SHELF + DESCRIPTION, true)
        )
        lyrics.add(
            "source",
            nav(
                response,
                listOf("contents") + SECTION_LIST_ITEM + DESCRIPTION_SHELF + listOf("footer") + RUN_TEXT,
                true
            )
        )

        return lyrics
    }

    override fun getBaseJsUrl(): String {
        val response = requester.sendGetRequest(YTM_DOMAIN)
        val match = "jsUrl\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(response.body)
            ?: throw NoSuchElementException("Could not identify the URL for base.js player.")

        return YTM_DOMAIN + match.groups[1]
    }

    override fun getSignatureTimestamp(url: String?): Int {
        val _url = url ?: getBaseJsUrl()
        val response = requester.sendGetRequest(_url)
        val match = "signatureTimestamp[:=](\\d+)".toRegex().find(response.body)
            ?: throw NoSuchElementException("Unable to identify the signatureTimestamp.")

        return match.groups[1]!!.value.toInt()
    }

    override fun getTasteprofile(): JsonObject {
        val body = JsonObject().also { json -> json.addProperty("browseId", "FEmusic_tastebuilder") }
        val response = requester.sendRequest(endpoint, body).body
        val profiles = nav(response, TASTE_PROFILE_ITEMS)!!.asJsonArray

        val tasteProfiles = JsonObject()
        for (itemList in profiles) {
            for (item in itemList.asJsonObject.getAsJsonObject("tastebuilderItemListRenderer")
                .getAsJsonArray("contents")) {
                val artist = nav(
                    item.asJsonObject.get("tastebuilderItemRenderer"),
                    TASTE_PROFILE_ARTIST
                )!!.asJsonArray[0].asJsonObject.getAsJsonPrimitive("text").asString
                tasteProfiles.add(artist, JsonObject().also { artist ->
                    artist.add(
                        "selectionValue",
                        item.asJsonObject.getAsJsonObject("tastebuilderItemRenderer").get("selectionFormValue")
                    )
                    artist.add(
                        "impressionValue",
                        item.asJsonObject.getAsJsonObject("tastebuilderItemRenderer").get("impressionFormValue")
                    )
                })
            }
        }
        return tasteProfiles
    }

    override fun setTasteprofile(artists: Collection<String>, tasteProfile: JsonObject?) {
        val profile = tasteProfile ?: getTasteprofile()
        val formData = JsonObject().also { json ->
            val impressionValues = JsonArray()
            impressionValues.asList()
                .addAll(profile.keySet().stream().map { p -> profile.getAsJsonObject(p).get("impressionValue") }
                    .toList())
            json.add("impressionValues", impressionValues)
            json.add("selectedValues", JsonArray())
        }

        for (artist in artists) {
            if (!profile.has(artist)) {
                throw NoSuchElementException("The artist \"$artist\" was not present in taste!")
            }
            formData.getAsJsonArray("selectedValues").add(profile.getAsJsonObject(artist).get("selectionValue"))
        }

        val body = JsonObject().also { json ->
            json.addProperty("browseId", "FEmusic_home")
            json.add("formData", formData)
        }
        requester.sendRequest(endpoint, body)
    }

}