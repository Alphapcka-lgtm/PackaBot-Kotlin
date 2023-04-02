package music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.internal.utils.JDALogger
import utils.RestActionExecutor
import java.awt.Color
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.annotation.Nonnull

/**
 * Default audio result handler for loading audio items.
 *
 * @param hook   the interaction of a command.
 * @param player the player that will have to work with the track.
 * @author Michael
 * @see AudioLoadResultHandler
 */
open class DefaultPackaBotAudioLoadResultHandler(
    /** the interaction hook of the command that triggered that adding/playing of a track/playlist  */
    @param:Nonnull private val m_Hook: InteractionHook,
    /** the player that is responsible for this track  */
    @param:Nonnull private val m_Player: Player,
) :
    AudioLoadResultHandler {

    /** the audio player of the player  */
    private val m_AudioPlayer: AudioPlayer = m_Player.audioPlayer

    /** the track scheduler for the player  */
    private val m_TrackScheduler: TrackScheduler = m_Player.trackScheduler

    /** the channel to which the player is bound.  */
    private val m_BoundChannel: MessageChannel = m_Player.boundChannel

    override fun trackLoaded(track: AudioTrack) {
        track.userData =
            TrackData(m_Hook.interaction.user, "https://i.ytimg.com/vi/${track.identifier}/maxresdefault.jpg", null)
        m_TrackScheduler.queue(track)

        val restAction = if (m_AudioPlayer.playingTrack == null) {
            m_AudioPlayer.startTrack(track, true)
            m_Hook.sendMessage("**Playing :notes: ** `" + track.info.title + "`")
        } else {
            if (!m_Hook.isExpired) {
                m_Hook.sendMessage("**Enqueued :heavy_plus_sign: ** `" + track.info.title + "`")
            } else {
                m_BoundChannel.sendMessage("**Enqueued :heavy_plus_sign: ** `" + track.info.title + "`")
            }
        }

        restAction.mapToResult().queue { result ->
            result.onFailure { error ->
                LOG.warn("Error when sending playing track message!", error)
            }
        }
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        for (track in playlist.tracks) {
            track.userData =
                TrackData(m_Hook.interaction.user, "https://i.ytimg.com/vi/${track.identifier}/maxresdefault.jpg", null)
            m_TrackScheduler.queue(track)
            if (m_AudioPlayer.playingTrack == null && !m_Player.isShuffled) {
                m_AudioPlayer.playTrack(track)
            }
        }
        if (m_Player.isShuffled) {
            m_TrackScheduler.shuffleQueue()
            val firstTrack = m_TrackScheduler.queue.first!!
            m_AudioPlayer.playTrack(firstTrack)
        }
        val embed = EmbedBuilder()
        embed.setColor(Color.BLACK)
        embed.setTitle("**Playlist added to queue**")
        embed.setThumbnail("https://i.ytimg.com/vi/${m_TrackScheduler.player.audioPlayer.playingTrack.identifier}/maxresdefault.jpg")
        embed.setDescription(playlist.name)
        embed.addField("Enqueued", "`" + playlist.tracks.size + "` songs", true)
        embed.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))
        embed.setFooter("playlist", m_Hook.interaction.user.avatarUrl)

        val restAction = if (!m_Hook.isExpired) {
            m_Hook.sendMessageEmbeds(embed.build())
        } else {
            m_BoundChannel.sendMessageEmbeds(embed.build())
        }.mapToResult()

        RestActionExecutor.queueRestActionResult(restAction, LOG, "Error when sending playlist embed!")
    }

    override fun noMatches() {
        // Notifies the user that nothing got found
        val restAction = if (!m_Hook.isExpired) {
            m_Hook.sendMessage(m_Hook.interaction.user.asMention + " Can not find the song!")
        } else {
            m_BoundChannel.sendMessage(m_Hook.interaction.user.asMention + " Can not find the song!")
        }.mapToResult()

        RestActionExecutor.queueSendMessageRestActionMapping(restAction, LOG)
    }

    override fun loadFailed(exception: FriendlyException) {
        // Notifies the user that everything exploded.
        val restAction = if (!m_Hook.isExpired) {
            m_Hook.sendMessage(
                m_Hook.interaction.user.asMention + " Sorry but everything just exploded..."
            )
        } else {
            m_BoundChannel.sendMessage(
                m_Hook.interaction.user.asMention + " Sorry but everything just exploded..."
            )
        }.mapToResult()

        RestActionExecutor.queueSendMessageRestActionMapping(restAction, LOG)
        LOG.error("Error when loading audio item!", exception)
    }

    companion object {
        /** the logger  */
        private val LOG = JDALogger.getLog(DefaultPackaBotAudioLoadResultHandler::class.java)
    }
}