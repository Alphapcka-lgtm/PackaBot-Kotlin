package yt_music.mixins

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import yt_music.enums.Order
import yt_music.enums.Rating

interface LibraryMixin {

    /**
     * Retrieves the playlists in the user's library.
     * @param limit Number of playlists to retrieve. `None` retrieves them all.
     * @return List of owned playlists.
     * Each item is in the following format:
     * ```
     *     {
     *         'playlistId': 'PLQwVIlKxHM6rz0fDJVv_0UlXGEWf-bFys',
     *         'title': 'Playlist title',
     *         'thumbnails: [...],
     *         'count': 5
     *     }
     *     ```
     */
    fun getLibraryPlaylists(limit: Int? = 25): JsonArray

    /**
     * Gets the songs in the user's library (liked videos are not included).
     * To get liked songs and videos, use :py:func:`get_liked_songs`
     * @param limit: Number of songs to retrieve
     * @param validateResponse Flag indicating if responses from YTM should be validated and retried in case
     *     when some songs are missing. Default: False
     * @param order Order of songs to return. Allowed values: 'a_to_z', 'z_to_a', 'recently_added'. Default: Default order.
     * @return List of songs. Same format as [PlaylistMixin.getPlaylist]
     */
    fun getLibrarySongs(limit: Int? = 25, validateResponse: Boolean = false, order: Order? = null): JsonArray

    /**
     * Gets the albums in the user's library.
     * @param limit Number of albums to return
     * @param order Order of albums to return. Allowed values: 'a_to_z', 'z_to_a', 'recently_added'. Default: Default order.
     * @return List of albums.
     * Each item is in the following format:
     * ```
     *     {
     *       "browseId": "MPREb_G8AiyN7RvFg",
     *       "playlistId": "OLAK5uy_lKgoGvlrWhX0EIPavQUXxyPed8Cj38AWc",
     *       "title": "Beautiful",
     *       "type": "Album",
     *       "thumbnails": [...],
     *       "artists": [{
     *         "name": "Project 46",
     *         "id": "UCXFv36m62USAN5rnVct9B4g"
     *       }],
     *       "year": "2015"
     *     }
     * ```
     */
    fun getLibraryAlbums(limit: Int = 25, order: Order? = null): JsonArray

    /**
     * Gets the artists of the songs in the user's library.
     * @param limit Number of artists to return
     * @param order Order of artists to return. Allowed values: 'a_to_z', 'z_to_a', 'recently_added'. Default: Default order.
     * @return List of artists.
     * Each item is in the following format:
     * ```
     *      {
     *        "browseId": "UCxEqaQWosMHaTih-tgzDqug",
     *        "artist": "WildVibes",
     *        "subscribers": "2.91K",
     *        "thumbnails": [...]
     *      }
     * ```
     */
    fun getLibraryArtists(limit: Int = 25, order: Order? = null): JsonArray

    /**
     * Gets the artists the user has subscribed to.
     * @param limit Number of artists to return
     * @param order Order of artists to return. Allowed values: 'a_to_z', 'z_to_a', 'recently_added'. Default: Default order.
     * @return List of artists. Same format as [getLibraryArtists]
     */
    fun getLibrarySubscriptions(limit: Int = 25, order: Order? = null): JsonArray

    /**
     * Gets your play history in reverse chronological order
     * @return List of playlistItems, see [PlaylistMixin.getPlaylist]
     * The additional property ``played`` indicates when the playlistItem was played
     * The additional property ``feedbackToken`` can be used to remove items with :py:func:`remove_history_items`
     */
    fun getLikedSongs(limit: Int = 100): JsonObject

    /**
     * Gets your play history in reverse chronological order
     * @return List of playlistItems, see [PlaylistMixin.getPlaylist]
     * The additional property ``played`` indicates when the playlistItem was played
     * The additional property ``feedbackToken`` can be used to remove items with [removeHistoryItems]
     */
    fun getHistory(): JsonArray

    /**
     * Add an item to the account's history using the playbackTracking URI
     * obtained from [BrowsingMixin.getSong].
     * @param song Dictionary as returned by :py:func:`get_song`
     * @return Full response. response.status_code is 204 if successful
     */
    fun addHistoryItem(song: JsonObject): String

    /**
     * Remove an item from the account's history. This method does currently not work with brand accounts
     * @param feedbackTokens Token to identify the item to remove, obtained from [getHistory]
     * @return Full response
     */
    fun removeHistoryItems(feedbackTokens: Collection<String>): JsonObject // pragma: no cover

    /**
     * Rates a song ("thumbs up"/"thumbs down" interactions on YouTube Music)
     * @param videoId Video id
     * @param rating One of 'LIKE', 'DISLIKE', 'INDIFFERENT'
     *
     *   | 'INDIFFERENT' removes the previous rating and assigns no rating
     * @return Full response
     */
    fun rateSong(videoId: String, rating: Rating = Rating.INDIFFERENT): JsonObject

    /**
     * Adds or removes a song from your library depending on the token provided.
     * @param feedbackTokens List of feedbackTokens obtained from authenticated requests
     * to endpoints that return songs (i.e. :py:func:`get_album`)
     * @return: Full response
     */
    fun editSongLibraryStatus(feedbackTokens: Collection<String>? = null): JsonObject

    /**
     * Rates a playlist/album ("Add to library"/"Remove from library" interactions on YouTube Music)
     * You can also dislike a playlist/album, which has an effect on your recommendations
     * @param playlistId Playlist id
     * @param rating One of 'LIKE', 'DISLIKE', 'INDIFFERENT'
     *
     *   | 'INDIFFERENT' removes the playlist/album from the library
     * @return Full response
     */
    fun ratePlaylist(playlistId: String, rating: Rating = Rating.INDIFFERENT): JsonObject

    /**
     * Subscribe to artists. Adds the artists to your library
     * @param channelIds Artist channel ids
     * @return Full response
     */
    fun subscribeArtists(channelIds: Collection<String>): JsonObject

    /**
     * Unsubscribe from artists. Removes the artists from your library
     * @param channelIds Artist channel ids
     * @return Full response
     */
    fun unsubscribeArtists(channelIds: Collection<String>): JsonObject

}