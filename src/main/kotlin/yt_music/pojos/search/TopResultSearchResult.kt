package yt_music.pojos.search

import com.google.gson.JsonObject
import yt_music.pojos.Artist
import yt_music.pojos.ResultTypes
import yt_music.pojos.SearchCategories
import yt_music.pojos.Thumbnail
import yt_music.utils.JsonFormatter

data class TopResultSearchResult(
    override val searchCategory: SearchCategories,
    override val resultType: ResultTypes,
    override val title: String,
    /**
     * if the top search result is from the result type [ResultTypes.ALBUM], this field will contain a string 'album' or 'single', depending on weather it's an album or a single
     */
    val type: String?,
    val artists: Collection<Artist>,
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
