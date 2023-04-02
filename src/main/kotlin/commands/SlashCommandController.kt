package commands

import PackaBot
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.internal.utils.JDALogger
import org.slf4j.Logger
import java.sql.Connection
import javax.annotation.Nonnull

object SlashCommandController {

    /** the logger */
    val LOG: Logger = JDALogger.getLog(SlashCommandRegister::class.java)

    /**
     * All registered commands.
     * The keys are the command ids of the commands.
     */
    val m_Commands: HashMap<String, PackaBotSlashCommand> = HashMap()

    /**
     * If the slash commands are registered
     */
    var isRegistered = false
        private set

    var databaseConnection: Connection? = null
    lateinit var packaBot: PackaBot
        private set

    /**
     * Registers all available slash commands.
     * @param packaBot the packa bot instance.
     * @param path to the slash command update script. If null, the default update script will be used.
     */
    fun registerSlashCommands(packaBot: PackaBot, path: String?) {
        this.packaBot = packaBot
        m_Commands.clear()

        val slashCommandRegister = SlashCommandRegister(packaBot)
        slashCommandRegister.setPathUpdateScript(path)
        val dataMap = slashCommandRegister.slashCommandData
        var unaddedCommandData = slashCommandRegister.loadCommandsWithDataMap(dataMap)
        unaddedCommandData = slashCommandRegister.addCommandsWithDataMap(unaddedCommandData)

        if (!unaddedCommandData.isEmpty()) {
            LOG.warn("Slash commands are not added: $unaddedCommandData")
        } else {
            LOG.info("All slash commands are added.")
        }

        m_Commands.putAll(slashCommandRegister.commands)
        isRegistered = true
    }

    /**
     * Checks if the slash command event has a CommandData class and executes the command, if the command has.<br>
     * <b>The slash commands first need to be registered with the method [SlashCommandController.registerSlashCommands]!</b>
     *
     * @param interaction the slash command event that was triggered (never `null`)
     * @return `true` if the event has data class with the execute method, otherwise `false`.
     * @throws IllegalStateException when the slash commands are not yet registered via [SlashCommandController.registerSlashCommands].
     */
    @Throws(IllegalStateException::class)
    fun checkAndRunSlashCommand(@Nonnull interaction: SlashCommandInteraction): Boolean {
        if (!isRegistered) {
            throw IllegalStateException("Slash commands are not registered!")
        }

        val commandId: String = interaction.commandId
        if (m_Commands.containsKey(commandId)) {
            val cmd: PackaBotSlashCommand = m_Commands[commandId]!!
            cmd.execute(interaction)
            return true
        }
        return false
    }
}