package yt_music.pojos.search

import com.google.gson.JsonObject
import yt_music.pojos.*
import yt_music.utils.JsonFormatter

data class VideoSearchResult(
    override val searchCategory: SearchCategories,
    override val resultType: ResultTypes,
    override val title: String,
    /** the video id of the video */
    val videoId: String,
    val videoType: VideoType,
    val artists: Collection<Artist>,
    val views: Long,
    val durationSeconds: Long,
    val thumbnails: Collection<Thumbnail>,
    override val json: JsonObject,
) : SearchResult {

    override fun toString(): String {
        return json.toString()
    }

    override fun toPrettyString(): String {
        return JsonFormatter.formatToPrettyString(json)
    }
}
