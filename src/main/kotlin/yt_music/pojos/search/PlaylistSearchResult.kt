package yt_music.pojos.search

import com.google.gson.JsonObject
import yt_music.pojos.ResultTypes
import yt_music.pojos.SearchCategories
import yt_music.pojos.Thumbnail
import yt_music.pojos.search.SearchResult
import yt_music.utils.JsonFormatter

data class PlaylistSearchResult(
    override val searchCategory: SearchCategories,
    override val resultType: ResultTypes,
    override val title: String,
    val itemCount: Int,
    val author: String,
    val browseId: String,
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
