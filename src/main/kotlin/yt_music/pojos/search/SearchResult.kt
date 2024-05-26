package yt_music.pojos.search

import com.google.gson.JsonObject
import yt_music.pojos.*
import yt_music.utils.convertSearchDuration

interface SearchResult {

    companion object {
        fun fromJson(json: JsonObject): SearchResult {

            val resultTypeStr = json.getAsJsonPrimitive("resultType").asString
            val resultType = ResultTypes.ofString(resultTypeStr)
                ?: throw IllegalArgumentException("Unknown result type: $resultTypeStr")

            val categoryStr = json.get("category").asString
            val category = SearchCategories.ofCategoryString(categoryStr)
                ?: throw Exception("Unknown SearchCategory: $categoryStr")

            if (category == SearchCategories.TOP_RESULT) {
                val title = json.get("title").asString
                val type = if (json.has("type")) json.get("type").asString else null

                val artistsJson = json.getAsJsonArray("artists")
                val artists = artistsJson.asList().stream().map { el -> Artist.fromJson(el.asJsonObject) }.toList()

                val thumbnailsJson = json.getAsJsonArray("thumbnails")
                val thumbnails =
                    thumbnailsJson.asList().stream().map { el -> Thumbnail.fromJson(el.asJsonObject) }.toList()

                return TopResultSearchResult(category, resultType, title, type, artists, thumbnails, json)
            }

            when (resultType) {
                ResultTypes.SONG -> {

                    val title = json.get("title").asString

                    val video = json.get("video").asString

                    val videoType = VideoType.valueOf(json.get("videoType").asString)

                    val artistsJson = json.getAsJsonArray("artists")
                    val artists =
                        artistsJson.asList().stream().map { element -> Artist.fromJson(element.asJsonObject) }.toList()

                    val durationSeconds = json.get("duration_seconds").asLong

                    val isExplicit = json.get("isExplicit").asBoolean

                    val thumbnailsJson = json.getAsJsonArray("thumbnails")
                    val thumbnails =
                        thumbnailsJson.asList().stream().map { el -> Thumbnail.fromJson(el.asJsonObject) }.toList()

                    return SongSearchResult(
                        category,
                        resultType,
                        title,
                        video,
                        videoType,
                        artists,
                        durationSeconds,
                        isExplicit,
                        thumbnails,
                        json
                    )
                }

                ResultTypes.VIDEO -> {

                    val title = json.get("title").asString

                    val video = json.get("video").asString

                    val videoType = VideoType.valueOf(json.get("videoType").asString)


                    val artistsJson = json.getAsJsonArray("artists")
                    val artists = artistsJson.asList().stream().map { el -> Artist.fromJson(el.asJsonObject) }.toList()

                    val views = convertSearchDuration(json.get("views").asString)

                    val durationSecond = json.get("duration_seconds").asLong

                    val thumbnailsJson = json.getAsJsonArray("thumbnails")
                    val thumbnails =
                        thumbnailsJson.asList().stream().map { el -> Thumbnail.fromJson(el.asJsonObject) }.toList()

                    return VideoSearchResult(
                        category,
                        resultType,
                        title,
                        video,
                        videoType,
                        artists,
                        views,
                        durationSecond,
                        thumbnails,
                        json
                    )
                }

                ResultTypes.ALBUM -> {
                    val title = json.get("title").asString
                    val type = json.get("type").asString
                    val year = json.get("year").asInt

                    val artistsJson = json.getAsJsonArray("artists")
                    val artists = artistsJson.asList().stream().map { el -> Artist.fromJson(el.asJsonObject) }.toList()

                    val browseId = json.get("browseId").asString
                    val isExplicit = json.get("isExplicit").asBoolean

                    val thumbnailsJson = json.getAsJsonArray("thumbnails")
                    val thumbnails =
                        thumbnailsJson.asList().stream().map { el -> Thumbnail.fromJson(el.asJsonObject) }.toList()

                    return AlbumSearchResult(
                        category,
                        resultType,
                        title,
                        type,
                        year,
                        artists,
                        browseId,
                        isExplicit,
                        thumbnails,
                        json
                    )
                }

                ResultTypes.ARTIST -> {
                    val artist = json.get("artist").asString
                    val shuffleId = json.get("shuffleId").asString
                    val radioId = json.get("radioId").asString
                    val browseId = json.get("browseId").asString

                    val thumbnailsJson = json.getAsJsonArray("thumbnails")
                    val thumbnails =
                        thumbnailsJson.asList().stream().map { el -> Thumbnail.fromJson(el.asJsonObject) }.toList()

                    return ArtistSearchResult(
                        category,
                        resultType,
                        artist,
                        shuffleId,
                        radioId,
                        browseId,
                        thumbnails,
                        json
                    )
                }

                ResultTypes.PLAYLIST -> {
                    val title = json.get("title").asString
                    val itemCount = json.get("itemCount").asInt
                    val author = json.get("author").asString
                    val browseId = json.get("browseId").asString

                    val thumbnailsJson = json.getAsJsonArray("thumbnails")
                    val thumbnails =
                        thumbnailsJson.asList().stream().map { el -> Thumbnail.fromJson(el.asJsonObject) }.toList()

                    return PlaylistSearchResult(
                        category,
                        resultType,
                        title,
                        itemCount,
                        author,
                        browseId,
                        thumbnails,
                        json
                    )
                }

                ResultTypes.COMMUNITY_PLAYLIST -> TODO()
                ResultTypes.FEATURED_PLAYLIST -> TODO()
            }

        }
    }

    val searchCategory: SearchCategories
    val resultType: ResultTypes
    val title: String
    val json: JsonObject

    val asTopResult: TopResultSearchResult
        get() = this as TopResultSearchResult

    val asSong: SongSearchResult
        get() = this as SongSearchResult

    val asVideo: VideoSearchResult
        get() = this as VideoSearchResult

    val asAlbum: AlbumSearchResult
        get() = this as AlbumSearchResult

    val asArtist: ArtistSearchResult
        get() = this as ArtistSearchResult

    val asPlaylist: PlaylistSearchResult
        get() = this as PlaylistSearchResult


    /**
     * Returns the json of this search result object as string.
     *
     * @return the json string.
     */
    override fun toString(): String

    fun toPrettyString(): String
}