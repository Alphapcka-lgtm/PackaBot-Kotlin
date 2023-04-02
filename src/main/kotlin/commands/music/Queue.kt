package commands.music

import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import music.Player
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.JDALogger

@SlashCommandAnnotation
object Queue : ExecutableSlashCommand {

    /** the logger */
    override val LOG = JDALogger.getLog(Queue::class.java)

    override val name = "queue"
    override val slashCommandData = createQueueCommand()

    /**
     * Creates the slash command data.
     */
    private fun createQueueCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Shows the queue of the music bot.")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        // interaction.guild is never null, because command is guild only
        val player = Player.getAudioPlayerFromGuild(interaction.guild!!) ?: return notActiveMessage(hook)
        player.queue(hook)
    }
}