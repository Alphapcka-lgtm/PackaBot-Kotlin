package tune_in_radio

import java.net.URL

data class TuneInAudioOutline(
    /** name of the audio */
    val text: String,
    /** url to the audio stream */
    val url: URL,
    /** the bitrate of the audio stream */
    val bitrate: Int,
    /** reliability of the audio stream */
    val reliability: Int,
    /** the guide id */
    val guideId: String?,
    /** subtext of the audio */
    val subtext: String?,
    /** tune in id of the genre */
    val genreId: String?,
    /** format of the audio stream */
    val formats: String,
    /** the item of the audio */
    val item: String?,
    /** url to the image of the audio source (e.g. the icon of a radio station)*/
    val image: URL?,
    /** id of the currently playing song */
    val nowPlayingId: String?,
    /** id of the preset */
    val presetId: String?,
    /** the category/genre */
    var category: TuneInCategory = TuneInCategory.CATEGORY_NOT_AVAILABLE,
    /** the profile */
    var profile: TuneInProfile = TuneInProfile.PROFILE_NOT_AVAILABLE,
) {
    /** the type (always audio) */
    val type: String = "audio"
}
