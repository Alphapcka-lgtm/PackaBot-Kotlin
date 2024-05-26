package yt_music

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kong.unirest.Proxy
import kong.unirest.Unirest
import yt_music.enums.*
import yt_music.mixins.*
import yt_music.mixins.impls.*
import yt_music.pojos.browse.Album
import yt_music.pojos.browse.Track
import yt_music.pojos.search.SearchResult


class YTMusic(
    private val auth: String?,
    private val user: String?,
    private val proxy: Proxy? = null,
) : SearchMixin, BrowsingMixin, LibraryMixin, WatchMixin, PlaylistsMixin {

    private val searchMixin: SearchMixin
    private val browsingMixin: BrowsingMixin
    private val libraryMixin: LibraryMixin
    private val playlistsMixin: PlaylistsMixin
    private val watchMixin: WatchMixin

    private val context: JsonObject = initializeContext()
    private val headers: MutableMap<String, String>
    private val language: String = "en"

    private val requester: Requester

    init {
        val unirest = Unirest.spawnInstance()
        headers = prepareHeaders(unirest, proxy, auth)

        if (user != null) {
            context.getAsJsonObject("context").getAsJsonObject("user").addProperty("onBehalfOfUser", user)
        }
        context.getAsJsonObject("context").getAsJsonObject("client").addProperty("h1", language)
        requester = Requester(unirest, headers, context)

        // check if map contains a key like this, to make the check case-insensitive
        if (!headers.keys.stream().anyMatch { key -> key.lowercase() == "x-goog-visitor-id" }) {
            val pair = getVisitorId(requester::sendGetRequest)
            headers[pair.first] = pair.second
        }

        searchMixin = SearchMixinImpl(requester)
        browsingMixin = BrowsingMixinImpl(requester)
        libraryMixin = LibraryMixinImpl(requester)
        playlistsMixin = PlaylistsMixinImpl(requester, auth)
        watchMixin = WatchMixinImpl(requester)
        //possible to later add auth header
    }

    override fun search(
        query: String,
        filter: SearchFilters?,
        scope: SearchScopes?,
        limit: Int,
        ignoreSpelling: Boolean,
    ): Collection<SearchResult> = searchMixin.search(query, filter, scope, limit, ignoreSpelling)

    override fun getSearchSuggestion(query: String, detailedRuns: Boolean): JsonArray =
        searchMixin.getSearchSuggestion(query, detailedRuns)

    override fun getHome(limit: Int): JsonArray = browsingMixin.getHome(limit)

    override fun getArtist(channelId_: String): JsonObject = browsingMixin.getArtist(channelId_)

    override fun getArtistAlbums(channelId: String, params: String): JsonArray =
        browsingMixin.getArtistAlbums(channelId, params)

    override fun getUser(channelId: String): JsonObject = browsingMixin.getUser(channelId)

    override fun getUserPlaylists(channelId: String, params: String): JsonArray =
        browsingMixin.getUserPlaylists(channelId, params)

    override fun getAlbumBrowseId(audioPlaylistId: String): String? = browsingMixin.getAlbumBrowseId(audioPlaylistId)

    override fun getAlbum(browseId: String): Album = browsingMixin.getAlbum(browseId)

    override fun getSong(videoId: String, signatureTimestamp_: Long?): JsonObject =
        browsingMixin.getSong(videoId, signatureTimestamp_)

    override fun getSongRelated(browseId: String): JsonArray = browsingMixin.getSongRelated(browseId)

    override fun getLyrics(browseId: String): JsonObject = browsingMixin.getLyrics(browseId)

    override fun getBaseJsUrl(): String = browsingMixin.getBaseJsUrl()

    override fun getSignatureTimestamp(url: String?): Int = browsingMixin.getSignatureTimestamp(url)

    override fun getTasteprofile(): JsonObject = browsingMixin.getTasteprofile()

    override fun setTasteprofile(artists: Collection<String>, tasteProfile: JsonObject?) =
        browsingMixin.setTasteprofile(artists, tasteProfile)

    override fun getLibraryPlaylists(limit: Int?): JsonArray = libraryMixin.getLibraryPlaylists(limit)

    override fun getLibrarySongs(limit: Int?, validateResponse: Boolean, order: Order?): JsonArray =
        libraryMixin.getLibrarySongs(limit)

    override fun getLibraryAlbums(limit: Int, order: Order?): JsonArray = libraryMixin.getLibraryAlbums(limit, order)

    override fun getLibraryArtists(limit: Int, order: Order?): JsonArray = libraryMixin.getLibraryArtists(limit, order)

    override fun getLibrarySubscriptions(limit: Int, order: Order?): JsonArray =
        libraryMixin.getLibrarySubscriptions(limit, order)

    override fun getLikedSongs(limit: Int): JsonObject = libraryMixin.getLikedSongs(limit)

    override fun getHistory(): JsonArray = libraryMixin.getHistory()

    override fun addHistoryItem(song: JsonObject): String = libraryMixin.addHistoryItem(song)

    override fun removeHistoryItems(feedbackTokens: Collection<String>): JsonObject =
        libraryMixin.removeHistoryItems(feedbackTokens)

    override fun rateSong(videoId: String, rating: Rating): JsonObject = libraryMixin.rateSong(videoId, rating)

    override fun editSongLibraryStatus(feedbackTokens: Collection<String>?): JsonObject =
        libraryMixin.editSongLibraryStatus(feedbackTokens)

    override fun ratePlaylist(playlistId: String, rating: Rating): JsonObject =
        libraryMixin.ratePlaylist(playlistId, rating)

    override fun subscribeArtists(channelIds: Collection<String>): JsonObject =
        libraryMixin.subscribeArtists(channelIds)

    override fun unsubscribeArtists(channelIds: Collection<String>): JsonObject =
        libraryMixin.unsubscribeArtists(channelIds)

    override fun getWatchPlaylist(
        videoId: String?,
        playlistId: String?,
        limit: Int,
        radio: Boolean,
        shuffle: Boolean,
    ): JsonObject = watchMixin.getWatchPlaylist(videoId, playlistId, limit, radio, shuffle)

    override fun getPlaylist(playlistId: String, limit: Int?, related: Boolean, suggestionsLimit: Int): JsonObject =
        playlistsMixin.getPlaylist(playlistId, limit, related, suggestionsLimit)

    override fun createPlaylist(
        title: String,
        description: String,
        privacyStatus: PrivacyStatus,
        videoIds: Collection<String>?,
        sourcePlaylist: String?,
    ): String = playlistsMixin.createPlaylist(title, description, privacyStatus, videoIds, sourcePlaylist)

    override fun editPlaylist(
        playlistId: String,
        title: String?,
        description: String?,
        privacyStatus: PrivacyStatus?,
        moveItem: Pair<String, String>?,
        addPlaylistId: String?,
    ): JsonObject = playlistsMixin.editPlaylist(playlistId, title, description, privacyStatus, moveItem, addPlaylistId)

    override fun deletePlaylist(playlistId: String): String = playlistsMixin.deletePlaylist(playlistId)

    override fun addPlaylistItem(
        playlistId: String,
        videoIds: Collection<String>?,
        sourcePlaylist: String?,
        duplicates: Boolean,
    ): JsonObject = addPlaylistItem(playlistId, videoIds, sourcePlaylist, duplicates)

    override fun removePlaylistItems(playlistId: String, videos: Collection<Track>): String =
        playlistsMixin.removePlaylistItems(playlistId, videos)

}