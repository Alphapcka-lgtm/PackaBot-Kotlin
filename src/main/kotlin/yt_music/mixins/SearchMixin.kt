package yt_music.mixins

import com.google.gson.JsonArray
import yt_music.enums.SearchFilters
import yt_music.enums.SearchScopes
import yt_music.pojos.search.SearchResult

interface SearchMixin {
    /**
     * Search YouTube music
     * Returns results within the provided category.
     * @param query Query string, i.e. 'Oasis Wonderwall'
     * @param filter Filter for item types. Allowed values: ``songs``, ``videos``, ``albums``, ``artists``, ``playlists``, ``community_playlists``, ``featured_playlists``, ``uploads``.
     *   Default: Default search, including all types of items.
     * @param scope: Search scope. Allowed values: ``library``, ``uploads``.
     *     For uploads, no filter can be set! An exception will be thrown if you attempt to do so.
     *     Default: Search the public YouTube Music catalogue.
     * @param limit Number of search results to return
     *   Default: 20
     * @param ignoreSpelling Whether to ignore YTM spelling suggestions.
     *   If True, the exact search term will be searched for, and will not be corrected.
     *   This does not have any effect when the filter is set to ``uploads``.
     *   Default: False, will use YTM's default behavior of autocorrecting the search.
     * @return List of results depending on filter.
     *   resultType specifies the type of item (important for default search).
     *   albums, artists and playlists additionally contain a browseId, corresponding to
     *   albumId, channelId and playlistId (browseId=``VL``+playlistId)
     *   Example list for default search with one result per resultType for brevity. Normally
     *   there are 3 results per resultType and an additional ``thumbnails`` key:
     *   ```
     *     [
     *       {
     *         "category": "Top result",
     *         "resultType": "video",
     *         "videoId": "vU05Eksc_iM",
     *         "title": "Wonderwall",
     *         "artists": [
     *           {
     *             "name": "Oasis",
     *             "id": "UCmMUZbaYdNH0bEd1PAlAqsA"
     *           }
     *         ],
     *         "views": "1.4M",
     *         "videoType": "MUSIC_VIDEO_TYPE_OMV",
     *         "duration": "4:38",
     *         "duration_seconds": 278
     *       },
     *       {
     *         "category": "Songs",
     *         "resultType": "song",
     *         "videoId": "ZrOKjDZOtkA",
     *         "title": "Wonderwall",
     *         "artists": [
     *           {
     *             "name": "Oasis",
     *             "id": "UCmMUZbaYdNH0bEd1PAlAqsA"
     *           }
     *         ],
     *         "album": {
     *           "name": "(What's The Story) Morning Glory? (Remastered)",
     *           "id": "MPREb_9nqEki4ZDpp"
     *         },
     *         "duration": "4:19",
     *         "duration_seconds": 259
     *         "isExplicit": false,
     *         "feedbackTokens": {
     *           "add": null,
     *           "remove": null
     *         }
     *       },
     *       {
     *         "category": "Albums",
     *         "resultType": "album",
     *         "browseId": "MPREb_9nqEki4ZDpp",
     *         "title": "(What's The Story) Morning Glory? (Remastered)",
     *         "type": "Album",
     *         "artist": "Oasis",
     *         "year": "1995",
     *         "isExplicit": false
     *       },
     *       {
     *         "category": "Community playlists",
     *         "resultType": "playlist",
     *         "browseId": "VLPLK1PkWQlWtnNfovRdGWpKffO1Wdi2kvDx",
     *         "title": "Wonderwall - Oasis",
     *         "author": "Tate Henderson",
     *         "itemCount": "174"
     *       },
     *       {
     *         "category": "Videos",
     *         "resultType": "video",
     *         "videoId": "bx1Bh8ZvH84",
     *         "title": "Wonderwall",
     *         "artists": [
     *           {
     *             "name": "Oasis",
     *             "id": "UCmMUZbaYdNH0bEd1PAlAqsA"
     *           }
     *         ],
     *         "views": "386M",
     *         "duration": "4:38",
     *         "duration_seconds": 278
     *       },
     *       {
     *         "category": "Artists",
     *         "resultType": "artist",
     *         "browseId": "UCmMUZbaYdNH0bEd1PAlAqsA",
     *         "artist": "Oasis",
     *         "shuffleId": "RDAOkjHYJjL1a3xspEyVkhHAsg",
     *         "radioId": "RDEMkjHYJjL1a3xspEyVkhHAsg"
     *       }
     *     ]
     * ```
     */
    fun search(
        query: String,
        filter: SearchFilters? = null,
        scope: SearchScopes? = null,
        limit: Int = 20,
        ignoreSpelling: Boolean = false,
    ): Collection<SearchResult>

    /**
     * Get Search Suggestions
     * @param query Query string, i.e. 'faded'
     * @param detailedRuns Whether to return detailed runs of each suggestion.
     *     If True, it returns the query that the user typed and the remaining
     *     suggestion along with the complete text (like many search services
     *     usually bold the text typed by the user).
     *     Default: False, returns the list of search suggestions in plain text.
     * @return List of search suggestion results depending on ``detailed_runs`` param.
     * Example response when ``query`` is 'fade' and ``detailed_runs`` is set to ``False``:
     * ```
     *       [
     *         "faded",
     *         "faded alan walker lyrics",
     *         "faded alan walker",
     *         "faded remix",
     *         "faded song",
     *         "faded lyrics",
     *         "faded instrumental"
     *       ]
     *   Example response when ``detailed_runs`` is set to ``True``::
     *       [
     *         {
     *           "text": "faded",
     *           "runs": [
     *             {
     *               "text": "fade",
     *               "bold": true
     *             },
     *             {
     *               "text": "d"
     *             }
     *           ]
     *         },
     *         {
     *           "text": "faded alan walker lyrics",
     *           "runs": [
     *             {
     *               "text": "fade",
     *               "bold": true
     *             },
     *             {
     *               "text": "d alan walker lyrics"
     *             }
     *           ]
     *         },
     *         {
     *           "text": "faded alan walker",
     *           "runs": [
     *             {
     *               "text": "fade",
     *               "bold": true
     *             },
     *             {
     *               "text": "d alan walker"
     *             }
     *           ]
     *         },
     *         ...
     *       ]
     * ```
     */
    fun getSearchSuggestion(query: String, detailedRuns: Boolean = false): JsonArray
}