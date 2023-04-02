package commands

import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.Logger
import utils.RestActionExecutor

/**
 * An interface for all slash commands that have the [SlashCommandAnnotation] Annotation in order to be executed during runtime by the [SlashCommandRegister]!
 */
interface ExecutableSlashCommand {

    /** the logger */
    val LOG: Logger

    /**
     * Property with the name of the slash command.
     */
    val name: String

    /**
     * Property with the slash command data
     */
    val slashCommandData: SlashCommandData

    /**
     * This method will be called during runtime.
     *
     * @param interaction the slash command interaction that was triggered by the command call.
     */
    fun execute(interaction: SlashCommandInteraction) {
        if (interaction.isAcknowledged) {
            return execute(interaction, interaction.hook)
        }

        interaction.deferReply().mapToResult().queue { result ->
            result.onSuccess { hook -> execute(interaction, hook) }.onFailure { error ->
                LOG.error("Error when acknowledging slash command interaction!", error)
            }
        }
    }

    /**
     * Method to be implemented in the commands. **The slash command interaction never needs to be acknowledged since it is expected that it already is!**
     *
     * @param interaction the slash command interaction that was triggered by the command call.
     * @param hook the interaction hook of this slash command interaction.
     */
    fun execute(interaction: SlashCommandInteraction, hook: InteractionHook)

    /**
     * Sends a message to the interaction hook that the player is not active. (not connected to a vc)
     */
    fun notActiveMessage(hook: InteractionHook) {
        RestActionExecutor.queueRestActionResult(
            hook.sendMessage("Currently not active.").mapToResult(),
            LOG,
            "Error when sending [not active] message!"
        )
    }

    /**
     * Sends a message to the interaction hook that the user needs to be connected to a voice channel.
     */
    fun userNotConnectedToAudioChannelMessage(hook: InteractionHook) {
        RestActionExecutor.queueRestActionResult(
            hook.sendMessage("${hook.interaction.user.asMention} you need to be connected to a voice channel!")
                .mapToResult(), LOG, "Error when sending [user not connected to audio channel] message!"
        )
    }

    /**
     * Sends a message to teh interaction hook that the user is not in the same vc as the bot.
     */
    fun userNotInVcOfBot(hook: InteractionHook) {
        RestActionExecutor.queueRestActionResult(
            hook.sendMessage("You need to be in the voice channel with the bot.").mapToResult(),
            LOG,
            "Error when sending [user not in vc of bot] message!"
        )
    }
}