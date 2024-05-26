package yt_music.pojos.browse

import com.google.gson.JsonObject
import yt_music.pojos.Artist
import yt_music.pojos.LikeStatus
import yt_music.pojos.VideoType
import yt_music.utils.JsonFormatter

data class Track(
    val videoId: String,
    /** The setVideoId is the unique id of this playlist item and
    needed for moving/removing playlist items. It is only shown when you own the playlist and have provided an auth */
    val setVideoId: String?,
    val title: String,
    val artists: Collection<Artist>,
    val albumTitle: String,
    val likeStatus: LikeStatus,
    val isAvailable: Boolean,
    val isExplicit: Boolean,
    val videoType: VideoType,
    val duration: String,
    val durationSeconds: Long,
    val json: JsonObject,
) {
    companion object {
        fun fromJson(json: JsonObject): Track {
            val videoId = json.get("videoId").asString
            val setVideoId = if (json.has("setVideoId")) json["setVideoId"].asString else null
            val title = json.get("title").asString

            val artistsJson = json.getAsJsonArray("artists")
            val artists = artistsJson.asList().stream().map { el -> Artist.fromJson(el.asJsonObject) }.toList()

            val album = json.get("album").asString
            val likeStatus = LikeStatus.valueOf(json.get("likeStatus").asString)
            val isAvailable = json.get("isAvailable").asBoolean
            val isExplicit = json.get("isExplicit").asBoolean
            val videoType = VideoType.valueOf(json.get("videoType").asString)
            val duration = json.get("duration").asString
            val durationSeconds = json.get("duration_seconds").asLong

            return Track(
                videoId,
                setVideoId,
                title,
                artists,
                album,
                likeStatus,
                isAvailable,
                isExplicit,
                videoType,
                duration,
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
