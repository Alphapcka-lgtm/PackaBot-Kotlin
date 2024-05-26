package yt_music.mixins

import com.google.gson.JsonObject
import yt_music.utils.AuthException
import yt_music.utils.OrderParameters

interface UploadsMixin {

    /**
     * Returns a list of uploaded songs
     *
     * @param limit How many songs to return. `None` retrieves them all. Default: 25
     * @param order Order of songs to return.
     * @return List of uploaded songs.
     * @throws AuthException when no auth was set.
     *
     * Each item is in the following format:
     * ```
     *     {
     *       "entityId": "t_po_CICr2crg7OWpchDpjPjrBA",
     *       "videoId": "Uise6RPKoek",
     *       "artists": [{
     *         'name': 'Coldplay',
     *         'id': 'FEmusic_library_privately_owned_artist_detaila_po_CICr2crg7OWpchIIY29sZHBsYXk',
     *       }],
     *       "title": "A Sky Full Of Stars",
     *       "album": "Ghost Stories",
     *       "likeStatus": "LIKE",
     *       "thumbnails": [...]
     *     }
     *     ```
     */
    @Throws(AuthException::class)
    fun getLibraryUploadSongs(limit: Int? = 25, order: OrderParameters? = null): Collection<JsonObject>

    /**
     * Gets the albums of uploaded songs in the user's library.
     *
     * @param limit Number of albums to return. `None` retrives them all. Default: 25
     * @param order Order of albums to return.
     * @return List of albums as returned by [getLibraryUploadAlbums]
     * @throws AuthException when no auth was set.
     */
    @Throws(AuthException::class)
    fun getLibraryUploadAlbums(limit: Int? = 25, order: OrderParameters? = null): Collection<JsonObject>

    /**
     * Gets the artists of uploaded songs in the user's library.
     *
     * @param limit Number of artists to return. `None` retrieves them all. Default: 25
     * @param order Order of artists to return.
     * @return List of artists as returned by [LibraryMixin.getLibraryArtists]
     */
    fun getLibraryUploadArtists(limit: Int? = 25, order: OrderParameters? = null): Collection<JsonObject>

    // TODO with getLibraryUploadArtist

}