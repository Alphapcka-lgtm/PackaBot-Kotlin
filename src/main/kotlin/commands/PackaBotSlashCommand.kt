package commands

import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction

class PackaBotSlashCommand(
    /** the command data */
    val m_CommandData: ExecutableSlashCommand,
    /** the command */
    val m_Command: Command,
    /** the command id */
    val m_CommandId: String,
) {
    fun execute(interaction: SlashCommandInteraction) {
        m_CommandData.execute(interaction)
    }
}