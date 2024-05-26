package yt_music.pojos.search

import com.google.gson.JsonObject
import yt_music.pojos.Artist
import yt_music.pojos.ResultTypes
import yt_music.pojos.SearchCategories
import yt_music.pojos.Thumbnail
import yt_music.pojos.search.SearchResult
import yt_music.utils.JsonFormatter

data class AlbumSearchResult(
    override val searchCategory: SearchCategories,
    override val resultType: ResultTypes,
    override val title: String,
    /** if it's a single or an album */
    val type: String,
    val year: Int,
    val artists: Collection<Artist>,
    val browseId: String,
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
