package yt_music.pojos.browse

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import yt_music.pojos.Artist
import yt_music.pojos.Thumbnail
import yt_music.pojos.Types
import yt_music.utils.JsonFormatter

data class Album(
    val title: String,
    val type: Types,
    val thumbnails: Collection<Thumbnail>,
    val artists: Collection<Artist>,
    val year: Int,
    val trackCount: Int,
    val duration: String,
    val audioPlaylistId: String,
    val tracks: Collection<Track>,
    val otherVersions: JsonArray?,
    val durationSeconds: Long,
    val json: JsonObject,
) {
    companion object {
        fun fromJson(json: JsonObject): Album {
            val title = json.get("title").asString
            val type = Types.ofString(json.get("type").asString)
                ?: throw IllegalArgumentException("Unknown type: ${json.get("type")}")

            val thumbnailsJson = json.getAsJsonArray("thumbnails")
            val thumbnails = thumbnailsJson.asList().stream().map { el -> Thumbnail.fromJson(el.asJsonObject) }.toList()

            val artistsJson = json.getAsJsonArray("artists")
            val artists = artistsJson.asList().stream().map { el -> Artist.fromJson(el.asJsonObject) }.toList()

            val year = json.get("year").asInt
            val trackCount = json.get("trackCount").asInt
            val duration = json.get("duration").asString
            val audioPlaylistId = json.get("audioPlaylistId").asString

            val tracksJson = json.getAsJsonArray("tracks")
            val tracks = tracksJson.asList().stream().map { el -> Track.fromJson(el.asJsonObject) }.toList()

            val otherVersions = json.getAsJsonArray("other_versions")
            val durationSeconds = json.get("duration_seconds").asLong

            return Album(
                title,
                type,
                thumbnails,
                artists,
                year,
                trackCount,
                duration,
                audioPlaylistId,
                tracks,
                otherVersions,
                durationSeconds,
                json
            )
        }
    }

    override fun toString(): String {
        return json.toString()
    }

    fun toPrettyString(): String {
        return JsonFormatter.formatToPrettyString(json)
    }
}
