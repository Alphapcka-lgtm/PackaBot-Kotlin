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
object Reshuffle : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG: Logger = JDALogger.getLog(Reshuffle::class.java)
    override val name = "reshuffle"
    override val slashCommandData = createReshuffleCommands()

    /**
     * Creates the slash command data
     */
    private fun createReshuffleCommands(): SlashCommandData {
        val commandData = Commands.slash(name, "Reshuffles the queue of the bot.")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        // interaction.guild is never null, because command is guild only
        val player =
            Player.getAudioPlayerFromGuild(hook.interaction.guild!!) ?: return Shuffle.execute(interaction, hook)

        player.reshuffle()
        queueSendMessageRestActionMapping(hook.sendMessage("Reshuffled the queue.").mapToResult(), LOG)
    }
}