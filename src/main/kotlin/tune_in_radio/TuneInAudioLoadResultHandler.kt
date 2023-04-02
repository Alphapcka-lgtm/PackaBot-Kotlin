package tune_in_radio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import music.DefaultPackaBotAudioLoadResultHandler
import music.Player
import music.TrackData
import music.TrackScheduler
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.internal.utils.JDALogger
import utils.PythonDiscordColors
import utils.RestActionExecutor
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.annotation.Nonnull

class TuneInAudioLoadResultHandler(
    /** the interaction hook of the command that triggered that adding/playing of a track/playlist  */
    @param:Nonnull private val m_Hook: InteractionHook,
    /** the player that is responsible for this track  */
    @param:Nonnull private val m_Player: Player,
    /** the audio outline that should be loaded. */
    private val m_AudioOutline: TuneInAudioOutline,
) :
    DefaultPackaBotAudioLoadResultHandler(m_Hook, m_Player) {
    /** the audio player of the player  */
    private val m_AudioPlayer: AudioPlayer = m_Player.audioPlayer

    /** the track scheduler for the player  */
    private val m_TrackScheduler: TrackScheduler = m_Player.trackScheduler

    /** the channel to which the player is bound.  */
    private val m_BoundChannel: MessageChannel = m_Player.boundChannel

    override fun trackLoaded(track: AudioTrack) {
        track.userData = TrackData(m_Hook.interaction.user, m_AudioOutline.image?.toString(), m_AudioOutline)
        m_TrackScheduler.queue(track)
        if (m_AudioPlayer.playingTrack == null) {
            m_AudioPlayer.startTrack(track, true)
        }

        // build the embed for the loaded radio.
        val builder = EmbedBuilder()
        builder.setColor(PythonDiscordColors.DARK_BLUE.color)
        builder.setTitle("Loaded Radio _${m_AudioOutline.text}_")
        builder.setDescription(m_AudioOutline.profile.description)
        builder.setThumbnail(m_AudioOutline.image.toString())
        builder.addField("genre", m_AudioOutline.category.title, false)
        builder.addField("country", m_AudioOutline.profile.country, false)
        builder.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))

        val restAction = if (!m_Hook.isExpired) {
            m_Hook.sendMessageEmbeds(builder.build()).mapToResult()
        } else {
            m_BoundChannel.sendMessageEmbeds(builder.build()).mapToResult()
        }

        RestActionExecutor.queueSendMessageEmbedRestActionMapping(restAction, LOG)
    }

    companion object {
        /** the logger  */
        private val LOG = JDALogger.getLog(TuneInAudioLoadResultHandler::class.java)
    }
}