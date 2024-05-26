package yt_music.mixins

import com.google.gson.JsonArray
import com.google.gson.JsonObject

interface ExploreMixin {

    /**
     * Fetch "Moods & Genres" categories from YouTube Music.
     * @return Dictionary of sections and categories.
     * Example:
     * ```
     *     {
     *         'For you': [
     *             {
     *                 'params': 'ggMPOg1uX1ZwN0pHT2NBT1Fk',
     *                 'title': '1980s'
     *             },
     *             {
     *                 'params': 'ggMPOg1uXzZQbDB5eThLRTQ3',
     *                 'title': 'Feel Good'
     *             },
     *             ...
     *         ],
     *         'Genres': [
     *             {
     *                 'params': 'ggMPOg1uXzVLbmZnaWI4STNs',
     *                 'title': 'Dance & Electronic'
     *             },
     *             {
     *                 'params': 'ggMPOg1uX3NjZllsNGVEMkZo',
     *                 'title': 'Decades'
     *             },
     *             ...
     *         ],
     *         'Moods & moments': [
     *             {
     *                 'params': 'ggMPOg1uXzVuc0dnZlhpV3Ba',
     *                 'title': 'Chill'
     *             },
     *             {
     *                 'params': 'ggMPOg1uX2ozUHlwbWM3ajNq',
     *                 'title': 'Commute'
     *             },
     *             ...
     *         ],
     *     }
     *     ```
     */
    fun getMoodCategories(): JsonObject

    /**
     * Retrieve a list of playlists for a given "Moods & Genres" category.
     * @param params: params obtained by [getMoodCategories]
     * @return: List of playlists in the format of :py:func:`get_library_playlists` TODO
     */
    fun getMoodPlaylists(params: String): JsonArray

    /**
     * Get latest charts data from YouTube Music: Top songs, top videos, top artists and top trending videos.
     * Global charts have no Trending section, US charts have an extra Genres section with some Genre charts.
     * @param country: ISO 3166-1 Alpha-2 country code. Default: ZZ = Global
     * @return: Dictionary containing chart songs (only if authenticated), chart videos, chart artists and
     * trending videos.
     * Example:
     * ```
     *     {
     *         "countries": {
     *             "selected": {
     *                 "text": "United States"
     *             },
     *             "options": ["DE",
     *                 "ZZ",
     *                 "ZW"]
     *         },
     *         "songs": {
     *             "playlist": "VLPL4fGSI1pDJn6O1LS0XSdF3RyO0Rq_LDeI",
     *             "items": [
     *                 {
     *                     "title": "Outside (Better Days)",
     *                     "videoId": "oT79YlRtXDg",
     *                     "artists": [
     *                         {
     *                             "name": "MO3",
     *                             "id": "UCdFt4Cvhr7Okaxo6hZg5K8g"
     *                         },
     *                         {
     *                             "name": "OG Bobby Billions",
     *                             "id": "UCLusb4T2tW3gOpJS1fJ-A9g"
     *                         }
     *                     ],
     *                     "thumbnails": [...],
     *                     "isExplicit": true,
     *                     "album": {
     *                         "name": "Outside (Better Days)",
     *                         "id": "MPREb_fX4Yv8frUNv"
     *                     },
     *                     "rank": "1",
     *                     "trend": "up"
     *                 }
     *             ]
     *         },
     *         "videos": {
     *             "playlist": "VLPL4fGSI1pDJn69On1f-8NAvX_CYlx7QyZc",
     *             "items": [
     *                 {
     *                     "title": "EVERY CHANCE I GET (Official Music Video) (feat. Lil Baby & Lil Durk)",
     *                     "videoId": "BTivsHlVcGU",
     *                     "playlistId": "PL4fGSI1pDJn69On1f-8NAvX_CYlx7QyZc",
     *                     "thumbnails": [],
     *                     "views": "46M"
     *                 }
     *             ]
     *         },
     *         "artists": {
     *             "playlist": null,
     *             "items": [
     *                 {
     *                     "title": "YoungBoy Never Broke Again",
     *                     "browseId": "UCR28YDxjDE3ogQROaNdnRbQ",
     *                     "subscribers": "9.62M",
     *                     "thumbnails": [],
     *                     "rank": "1",
     *                     "trend": "neutral"
     *                 }
     *             ]
     *         },
     *         "genres": [
     *             {
     *                 "title": "Top 50 Pop Music Videos United States",
     *                 "playlistId": "PL4fGSI1pDJn77aK7sAW2AT0oOzo5inWY8",
     *                 "thumbnails": []
     *             }
     *         ],
     *         "trending": {
     *             "playlist": "VLPLrEnWoR732-DtKgaDdnPkezM_nDidBU9H",
     *             "items": [
     *                 {
     *                     "title": "Permission to Dance",
     *                     "videoId": "CuklIb9d3fI",
     *                     "playlistId": "PLrEnWoR732-DtKgaDdnPkezM_nDidBU9H",
     *                     "artists": [
     *                         {
     *                             "name": "BTS",
     *                             "id": "UC9vrvNSL3xcWGSkV86REBSg"
     *                         }
     *                     ],
     *                     "thumbnails": [],
     *                     "views": "108M"
     *                 }
     *             ]
     *         }
     *     }
     *     ```
     */
    fun getCharts(country: String = "ZZ"): JsonObject

}