package yt_music.mixins

import com.google.gson.JsonObject

interface WatchMixin {

    /**
     * Get a watch list of tracks. This watch playlist appears when you press
     * play on a track in YouTube
     * Please note that the `INDIFFERENT` likeStatus of tracks returned by this
     * endpoint may be either `INDIFFERENT` or `DISLIKE`, due to ambiguous data
     * returned by YouTube
     * @param videoId videoId of the played video
     * @param playlistId playlistId of the played playlist or album
     * @param limit minimum number of watch playlist items to return
     * @param radio get a radio playlist (changes each time)
     * @param shuffle shuffle the input playlist. only works when the playlistId parameter
     *     is set at the same time. does not work if radio=True
     * @return List of watch playlist items. The counterpart key is optional and only
     *     appears if a song has a corresponding video counterpart (UI song/video
     *     switch
     * Example:
     * ```
     *     {
     *         "tracks": [
     *             {
     *               "videoId": "9mWr4c_ig54",
     *               "title": "Foolish Of Me (feat. Jonathan Mendelsohn)",
     *               "length": "3:07",
     *               "thumbnail": [
     *                 {
     *                   "url": "https://lh3.googleusercontent.com/ulK2YaLtOW0PzcN7ufltG6e4ae3WZ9Bvg8CCwhe6LOccu1lCKxJy2r5AsYrsHeMBSLrGJCNpJqXgwczk=w60-h60-l90-rj",
     *                   "width": 60,
     *                   "height": 60
     *                 }...
     *               ],
     *               "feedbackTokens": {
     *                 "add": "AB9zfpIGg9XN4u2iJ...",
     *                 "remove": "AB9zfpJdzWLcdZtC..."
     *               },
     *               "likeStatus": "INDIFFERENT",
     *               "videoType": "MUSIC_VIDEO_TYPE_ATV",
     *               "artists": [
     *                 {
     *                   "name": "Seven Lions",
     *                   "id": "UCYd2yzYRx7b9FYnBSlbnknA"
     *                 },
     *                 {
     *                   "name": "Jason Ross",
     *                   "id": "UCVCD9Iwnqn2ipN9JIF6B-nA"
     *                 },
     *                 {
     *                   "name": "Crystal Skies",
     *                   "id": "UCTJZESxeZ0J_M7JXyFUVmvA"
     *                 }
     *               ],
     *               "album": {
     *                 "name": "Foolish Of Me",
     *                 "id": "MPREb_C8aRK1qmsDJ"
     *               },
     *               "year": "2020",
     *               "counterpart": {
     *                 "videoId": "E0S4W34zFMA",
     *                 "title": "Foolish Of Me [ABGT404] (feat. Jonathan Mendelsohn)",
     *                 "length": "3:07",
     *                 "thumbnail": [...],
     *                 "feedbackTokens": null,
     *                 "likeStatus": "LIKE",
     *                 "artists": [
     *                   {
     *                     "name": "Jason Ross",
     *                     "id": null
     *                   },
     *                   {
     *                     "name": "Seven Lions",
     *                     "id": null
     *                   },
     *                   {
     *                     "name": "Crystal Skies",
     *                     "id": null
     *                   }
     *                 ],
     *                 "views": "6.6K"
     *               }
     *             },...
     *         ],
     *         "playlistId": "RDAMVM4y33h81phKU",
     *         "lyrics": "MPLYt_HNNclO0Ddoc-17"
     *     }
     * ```
     * @throws IllegalArgumentException when neither the videoId nor a playlistId is provided.
     */
    @Throws(IllegalArgumentException::class)
    fun getWatchPlaylist(
        videoId: String? = null,
        playlistId: String? = null,
        limit: Int = 25,
        radio: Boolean = false,
        shuffle: Boolean = false,
    ): JsonObject


}