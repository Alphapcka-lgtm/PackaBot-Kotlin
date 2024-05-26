package yt_music.pojos.search

import com.google.gson.JsonObject
import yt_music.pojos.*
import yt_music.utils.JsonFormatter

data class SongSearchResult(
    override val searchCategory: SearchCategories,
    override val resultType: ResultTypes,
    override val title: String,
    // TODO: Feedback tokens. Currently unknown how they look, so not implemented yet
    /** the video id of the song */
    val videoId: String,
    val videoType: VideoType,
    val artists: Collection<Artist>,
    val durationSeconds: Long,
    val isExplicit: Boolean,
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