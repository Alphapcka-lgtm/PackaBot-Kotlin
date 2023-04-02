package commands.music

import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import music.Player
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.JDALogger
import org.slf4j.Logger
import utils.RestActionExecutor

@SlashCommandAnnotation
object NowPlaying : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG: Logger = JDALogger.getLog(NowPlaying::class.java)
    override val name = "nowplaying"
    override val slashCommandData = createNowPlayingCommand()

    /**
     * Creates the slash command data
     */
    private fun createNowPlayingCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Shows the currently playing song.")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {

        // interaction.guild is never null because the command is guild only
        val player = Player.getAudioPlayerFromGuild(hook.interaction.guild!!) ?: return notActiveMessage(hook)

        if (!player.isPlaying) {
            return queueSendMessageRestActionMapping(
                hook.sendMessage("Currently not playing anything.").mapToResult(),
                LOG
            )
        }

        hook.sendMessageEmbeds(player.nowPlayingEmbed).queue()
    }
}