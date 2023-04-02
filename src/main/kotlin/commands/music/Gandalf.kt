package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import commands.voice.JoinVoice
import music.Player
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.Result
import net.dv8tion.jda.internal.utils.JDALogger
import utils.RestActionExecutor

@SlashCommandAnnotation
object Gandalf : ExecutableSlashCommand, RestActionExecutor() {

    /** the logger */
    override val LOG = JDALogger.getLog(Gandalf::class.java)

    /** url of the sax guy gandalf 10h video. */
    private const val SAX_GUY_GANDALF_URL = "https://youtu.be/G1IbRujko-A"

    /** url to an awesome gandalf gif ;) */
    private const val GANDALF_GIF = "https://media.giphy.com/media/TcdpZwYDPlWXC/giphy.gif"

    override val name = "gandalf"
    override val slashCommandData = createGandalfCommand()

    /**
     * Creates the slash command data.
     */
    private fun createGandalfCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Gandalf :smirk: ")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        // interaction.guild is never null, because command is guild only.
        var player = Player.getAudioPlayerFromGuild(interaction.guild!!)

        if (player == null || !player.audioManager.isConnected) {
            JoinVoice.execute(interaction, hook)
            player = Player.getAudioPlayerFromGuild(interaction.guild!!)
        }

        if (player == null || !player.audioManager.isConnected) {
            LOG.info("Bot not connected!")
            return
        }

        player.loadAndPlay(SAX_GUY_GANDALF_URL, object : AudioLoadResultHandler {

            override fun trackLoaded(track: AudioTrack) {
                track.userData = interaction.user
                player.trackScheduler.queue(track)
                if (!player.isPlaying) {
                    player.audioPlayer.startTrack(track, true)
                }

                sendGandalfGif(hook, player)
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                for (track in playlist.tracks) {
                    track.userData = interaction.user
                    player.trackScheduler.queue(track)
                    if (!player.isPlaying && !player.isShuffled) {
                        player.audioPlayer.playTrack(track)
                    }
                }

                if (player.isShuffled) {
                    player.trackScheduler.shuffleQueue()
                    val firstTrack = player.trackScheduler.queue.first!!
                    player.audioPlayer.startTrack(firstTrack, true)
                }

                sendGandalfGif(hook, player)
            }

            override fun noMatches() {
                LOG.warn("Sax guy gandalf could not be loaded! [No matches]")

                val messageText = "Unable to load music, here's a gif: \n$GANDALF_GIF"
                val errorLogMes = "Error when sending [Unable to load music] message in command [$name]!"

                val restAction: RestAction<Result<Message>> = if (hook.isExpired) {
                    player.boundChannel.sendMessage(messageText)
                } else {
                    hook.sendMessage(messageText)
                }.mapToResult()

                queueRestAction(restAction, LOG, errorLogMes)

            }

            override fun loadFailed(exception: FriendlyException) {
                LOG.warn("Sax guy gandalf could not be loaded! [exception]", exception)

                val messageText = "${hook.interaction.member!!.asMention} Error when executing gandalf command."

                val restAction: RestAction<Result<Message>> = if (hook.isExpired) {
                    player.boundChannel.sendMessage(messageText)
                } else {
                    hook.sendMessage(messageText)
                }.mapToResult()

                queueSendMessageRestActionMapping(restAction, LOG)
            }

        })
    }

    /**
     * Sends the gandalf gif to the hook or the bound channel of player, if the hook is expired.
     *
     * @param hook the hook
     * @param player the player
     */
    private fun sendGandalfGif(hook: InteractionHook, player: Player) {

        val restAction = if (hook.isExpired) {
            player.boundChannel.sendMessage(GANDALF_GIF).mapToResult()
        } else {
            hook.sendMessage(GANDALF_GIF).mapToResult()
        }

        queueRestActionResult(restAction, LOG, "Error when sending awesome gandalf gif!")
    }
}