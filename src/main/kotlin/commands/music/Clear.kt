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
object Clear : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG: Logger = JDALogger.getLog(Clear::class.java)
    override val name = "clear"

    override val slashCommandData: SlashCommandData = createClearCommand()

    /**
     * Creates the slash command data
     */
    private fun createClearCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Clears the queue and stops the current song")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {

        // command is guild only, so guild property can never be null!
        val player = Player.getAudioPlayerFromGuild(hook.interaction.guild!!) ?: return notActiveMessage(hook)

        player.clearQueue()
        queueSendMessageRestActionMapping(hook.sendMessage("Cleared the queue.").mapToResult(), LOG)
    }

}