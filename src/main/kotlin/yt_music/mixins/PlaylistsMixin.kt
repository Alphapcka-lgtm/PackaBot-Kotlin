package yt_music.mixins

import com.google.gson.JsonObject
import yt_music.enums.PrivacyStatus
import yt_music.pojos.browse.Track
import yt_music.utils.AuthException
import java.io.IOException

interface PlaylistsMixin {

    /**
     * Returns a list of playlist items
     *
     * @param playlistId: Playlist id
     * @param limit How many songs to return. `None` retrieves them all. Default: 100
     * @param related Whether to fetch 10 related playlists or not. Default: False
     * @param suggestionsLimit How many suggestions to return. The result is a list of
     *     suggested playlist items (videos) contained in a "suggestions" key.
     *     7 items are retrieved in each internal request. Default: 0
     * @return Dictionary with information about the playlist.
     *     The key ``tracks`` contains a List of playlistItem dictionaries
     *
     * Each item is in the following format:
     * ```
     *     {
     *       "id": "PLQwVIlKxHM6qv-o99iX9R85og7IzF9YS_",
     *       "privacy": "PUBLIC",
     *       "title": "New EDM This Week 03/13/2020",
     *       "thumbnails": [...]
     *       "description": "Weekly r/EDM new release roundup. Created with github.com/sigma67/spotifyplaylist_to_gmusic",
     *       "author": "sigmatics",
     *       "year": "2020",
     *       "duration": "6+ hours",
     *       "duration_seconds": 52651,
     *       "trackCount": 237,
     *       "suggestions": [
     *           {
     *             "videoId": "HLCsfOykA94",
     *             "title": "Mambo (GATTÜSO Remix)",
     *             "artists": [{
     *                 "name": "Nikki Vianna",
     *                 "id": "UCMW5eSIO1moVlIBLQzq4PnQ"
     *               }],
     *             "album": {
     *               "name": "Mambo (GATTÜSO Remix)",
     *               "id": "MPREb_jLeQJsd7U9w"
     *             },
     *             "likeStatus": "LIKE",
     *             "thumbnails": [...],
     *             "isAvailable": true,
     *             "isExplicit": false,
     *             "duration": "3:32",
     *             "duration_seconds": 212,
     *             "setVideoId": "to_be_updated_by_client"
     *           }
     *       ],
     *       "related": [
     *           {
     *             "title": "Presenting MYRNE",
     *             "playlistId": "RDCLAK5uy_mbdO3_xdD4NtU1rWI0OmvRSRZ8NH4uJCM",
     *             "thumbnails": [...],
     *             "description": "Playlist • YouTube Music"
     *           }
     *       ],
     *       "tracks": [
     *         {
     *           "videoId": "bjGppZKiuFE",
     *           "title": "Lost",
     *           "artists": [
     *             {
     *               "name": "Guest Who",
     *               "id": "UCkgCRdnnqWnUeIH7EIc3dBg"
     *             },
     *             {
     *               "name": "Kate Wild",
     *               "id": "UCwR2l3JfJbvB6aq0RnnJfWg"
     *             }
     *           ],
     *           "album": {
     *             "name": "Lost",
     *             "id": "MPREb_PxmzvDuqOnC"
     *           },
     *           "duration": "2:58",
     *           "likeStatus": "INDIFFERENT",
     *           "thumbnails": [...],
     *           "isAvailable": True,
     *           "isExplicit": False,
     *           "videoType": "MUSIC_VIDEO_TYPE_OMV",
     *           "feedbackTokens": {
     *             "add": "AB9zfpJxtvrU...",
     *             "remove": "AB9zfpKTyZ..."
     *         }
     *       ]
     *     }
     * ```
     * The setVideoId is the unique id of this playlist item and
     * needed for moving/removing playlist items
     */
    fun getPlaylist(
        playlistId: String,
        limit: Int? = 100,
        related: Boolean = false,
        suggestionsLimit: Int = 0,
    ): JsonObject

    /**
     * Creates a new empty playlist and returns its id.
     *
     * @param title: Playlist title
     * @param description: Playlist description
     * @param privacyStatus: Playlists can be 'PUBLIC', 'PRIVATE', or 'UNLISTED'. Default: 'PRIVATE'
     * @param videoIds: IDs of songs to create the playlist with
     * @param sourcePlaylist: Another playlist whose songs should be added to the new playlist
     * @return ID of the YouTube playlist
     * @throws AuthException when no auth was set.
     * @throws IOException when there was an error other than having no auth.
     */
    @Throws(IllegalStateException::class, IOException::class)
    fun createPlaylist(
        title: String,
        description: String,
        privacyStatus: PrivacyStatus = PrivacyStatus.PRIVATE,
        videoIds: Collection<String>? = null,
        sourcePlaylist: String? = null,
    ): String

    /**
     * Edit title, description or privacyStatus of a playlist.
     * You may also move an item within a playlist or append another playlist to this pla
     * @param playlistId: Playlist id
     * @param title Optional. New title for the playlist
     * @param description Optional. New description for the playlist
     * @param privacyStatus Optional. New privacy status for the playlist
     * @param moveItem Optional. Move one item before another. Items are specified by setVideoId, see [PlaylistsMixin.getPlaylist]
     * @param addPlaylistId Optional. Id of another playlist to add to this playlist
     * @return Status String or full response
     * @throws AuthException when no auth was set.
     * @throws IOException when there was an error other than having no auth.
     */
    @Throws(AuthException::class, IOException::class)
    fun editPlaylist(
        playlistId: String,
        title: String? = null,
        description: String? = null,
        privacyStatus: PrivacyStatus? = null,
        moveItem: Pair<String, String>? = null,
        addPlaylistId: String? = null,
    ): JsonObject

    /**
     * Delete a playlist.
     *
     * @param playlistId Playlist id
     * @return Status String or full response
     * @throws AuthException when no auth was set.
     * @throws IOException when there was an error other than having no auth.
     */
    @Throws(AuthException::class, IOException::class)
    fun deletePlaylist(playlistId: String): String

    /**
     * Add songs to an existing playlist
     *
     * @param playlistId Playlist id
     * @param videoIds List of Video ids
     * @param sourcePlaylist Playlist id of a playlist to add to the current playlist (no duplicate check)
     * @param duplicates If True, duplicates will be added. If False, an error will be returned if there are duplicates (no items are added to the playlist)
     * @return Status String and a dict containing the new setVideoId for each videoId or full response
     * @throws AuthException when no auth was set.
     * @throws IllegalArgumentException If neither videoIds nor a source playlist is provided.
     */
    @Throws(AuthException::class, IllegalArgumentException::class)
    fun addPlaylistItem(
        playlistId: String,
        videoIds: Collection<String>? = null,
        sourcePlaylist: String? = null,
        duplicates: Boolean = false,
    ): JsonObject

    /**
     * Remove songs from an existing playlist
     *
     * @param playlistId Playlist id
     * @param videos List of PlaylistItems, see [PlaylistsMixin.getPlaylist].
     *     Must contain videoId and setVideoId
     * @return Status String or full response
     * @throws IllegalStateException when no auth was set.
     * @throws NoSuchElementException if you no json of the tracks contain the 'setVideoId' key or when [videos] is empty.
     * @throws IOException when there was an error other than having no auth.
     */
    @Throws(AuthException::class, NoSuchElementException::class, IOException::class)
    fun removePlaylistItems(playlistId: String, videos: Collection<Track>): String
}