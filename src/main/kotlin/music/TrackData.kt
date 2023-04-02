package music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.User
import tune_in_radio.TuneInAudioOutline

/**
 * Data class to provide more track data to [AudioTrack]
 */
data class TrackData(var user: User, var thumbnailUrl: String?, var tuneInAudio: TuneInAudioOutline?) {
    val isTuneInRadio: Boolean
        get() {
            return tuneInAudio != null
        }
}