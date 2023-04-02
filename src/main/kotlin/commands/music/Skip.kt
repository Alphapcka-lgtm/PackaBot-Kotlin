package commands.music

import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import music.Player
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.internal.utils.JDALogger
import utils.RestActionExecutor

@SlashCommandAnnotation
object Skip : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG = JDALogger.getLog(Skip::class.java)
    override val name = "skip"
    override val slashCommandData = createSkipCommand()

    private const val NAME_SUB_CURRENT = "current"
    private const val NAME_SUB_NUMBER = "number"
    private const val NAME_SUB_RANGE = "range"

    private const val NAME_NUMBER_OPTION_NAME = "number"
    private const val NAME_START_OPTION_NAME = "start"
    private const val NAME_END_OPTION_NAME = "end"

    /**
     * Creates the slash command data.
     */
    private fun createSkipCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "To skip one or more tracks.")
        commandData.isGuildOnly = true

        val subSkipCurrent = SubcommandData(NAME_SUB_CURRENT, "Skips the current track.")

        val subSkipNumber = SubcommandData(NAME_SUB_NUMBER, "Skips at a certain position in the queue.")
        subSkipNumber.addOption(OptionType.INTEGER, NAME_NUMBER_OPTION_NAME, "index of the song in the queue.", true)

        val subSkipRange = SubcommandData(NAME_SUB_RANGE, "Skip all tracks in range.")
        subSkipRange.addOption(
            OptionType.INTEGER,
            NAME_START_OPTION_NAME,
            "The start of the range of tracks to skip.",
            true
        )
        subSkipRange.addOption(
            OptionType.INTEGER,
            NAME_END_OPTION_NAME,
            "The end of the range of tracks to skip.",
            true
        )

        commandData.addSubcommands(subSkipCurrent, subSkipNumber, subSkipRange)

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        val _interaction = hook.interaction

        // interaction.guild is never null, because command is guild only
        val player = Player.getAudioPlayerFromGuild(_interaction.guild!!)

        if (player == null || !player.audioManager.isConnected) {
            return notActiveMessage(hook)
        }

        if (!player.isPlaying) {
            return queueSendMessageRestActionMapping(
                hook.sendMessage("Currently not playing anything.").mapToResult(),
                LOG
            )
        }

        when (interaction.subcommandName) {
            NAME_SUB_CURRENT -> executeSubcommandCurrent(interaction, hook, player)
            NAME_SUB_NUMBER -> executeSubcommandNumber(interaction, hook, player)
            NAME_SUB_RANGE -> executeSubcommandRange(interaction, hook, player)
            else -> {
                queueSendMessageRestActionMapping(
                    hook.sendMessage("**Error:** Unexpected subcommand.").mapToResult(),
                    LOG
                )
            }
        }

    }

    /**
     * Execution for hte skip current subcommand.
     *
     * @param interaction the slash command interaction that was triggered by the command call.
     * @param hook the interaction hook of this slash command interaction.
     * @param player the player which is currently handling the music.
     */
    private fun executeSubcommandCurrent(interaction: SlashCommandInteraction, hook: InteractionHook, player: Player) {
        val skippedTrack = player.skipCurrent()
        val restAction = if (skippedTrack.isPresent) {
            hook.sendMessage("Skipped track `${skippedTrack.get().info.title}`")
        } else {
            hook.sendMessage("Currently not playing anything.")
        }.mapToResult()

        queueSendMessageRestActionMapping(restAction, LOG)
    }

    /**
     * Execution for the skip number subcommand.
     *
     * @param interaction the slash command interaction that was triggered by the command call.
     * @param hook the interaction hook of this slash command interaction.
     * @param player the player which is currently handling the music.
     */
    private fun executeSubcommandNumber(interaction: SlashCommandInteraction, hook: InteractionHook, player: Player) {
        val _interaction = hook.interaction
        val number =
            interaction.getOption(NAME_NUMBER_OPTION_NAME, OptionMapping::getAsInt)
                ?: return queueSendMessageRestActionMapping(
                    hook.sendMessage("${_interaction.member!!.asMention} you need to specify the option `number`!")
                        .mapToResult(), LOG
                )

        if (number < 0) {
            return queueSendMessageRestActionMapping(
                hook.sendMessage("Invalid queue index! [$number]").mapToResult(),
                LOG
            )
        }

        val track = player.skipNumber(number)

        val restAction = if (track.isPresent) {
            hook.sendMessage("Skipped track `${track.get().info.title}` at position $number.")
        } else {
            hook.sendMessage("${_interaction.member!!.asMention} given index is out of bounce of the queue indexes!")
        }.mapToResult()

        queueSendMessageEmbedRestActionMapping(restAction, LOG)
    }

    /**
     * Execution for the skip range subcommand.
     *
     * @param interaction the slash command interaction that was triggered by the command call.
     * @param hook the interaction hook of this slash command interaction.
     * @param player the player which is currently handling the music.
     */
    private fun executeSubcommandRange(interaction: SlashCommandInteraction, hook: InteractionHook, player: Player) {
        val _interaction = hook.interaction

        val startIndex = interaction.getOption(NAME_START_OPTION_NAME, OptionMapping::getAsInt)
            ?: return queueSendMessageRestActionMapping(
                hook.sendMessage("${_interaction.member!!.asMention} you need to specify the `start`-option!")
                    .mapToResult(), LOG
            )

        val endIndex = interaction.getOption(NAME_END_OPTION_NAME, OptionMapping::getAsInt)
            ?: return queueSendMessageRestActionMapping(
                hook.sendMessage("${_interaction.member!!.asMention} you need to specify the `start`-option!")
                    .mapToResult(), LOG
            )

        if (startIndex <= 0) {
            return sendInvalidStartIndexMes(hook)
        }

        if (endIndex <= 0 || endIndex <= startIndex) {
            return sendInvalidEndIndexMes(hook)
        }

        when (player.skipFromTo(startIndex, endIndex)) {
            1 -> {
                return queueSendMessageRestActionMapping(
                    hook.sendMessage("Skipped tracks **$startIndex** to **$endIndex**.").mapToResult(), LOG
                )
            }

            -1 -> {
                return sendInvalidStartIndexMes(hook)
            }

            -2 -> {
                return sendInvalidEndIndexMes(hook)
            }

            -3 -> {
                return queueSendMessageRestActionMapping(
                    hook.sendMessage("${_interaction.member!!.asMention} The start index must **not** be `greater` or `equal` than the length of the queue!")
                        .mapToResult(), LOG
                )
            }

            -4 -> {
                return queueSendMessageRestActionMapping(
                    hook.sendMessage("${_interaction.member!!.asMention} The end index must **not** be `greater` than the length of the queue!")
                        .mapToResult(), LOG
                )
            }

            else -> {
                return queueSendMessageRestActionMapping(
                    hook.sendMessage("${_interaction.member!!.asMention} An internal server error occurred! Please try again later.")
                        .mapToResult(), LOG
                )
            }
        }

    }

    /**
     * Sends a message to the user that the value of the start index is invalid.
     */
    private fun sendInvalidStartIndexMes(hook: InteractionHook) {
        queueSendMessageEmbedRestActionMapping(
            hook.sendMessage("${hook.interaction.member!!.asMention} The start index must be greater than 0!")
                .mapToResult(), LOG
        )
    }

    /**
     * Sends a message to the user that the value of the end index is invalid.
     */
    private fun sendInvalidEndIndexMes(hook: InteractionHook) {
        queueSendMessageEmbedRestActionMapping(
            hook.sendMessage("${hook.interaction.member!!.asMention} The end index must be greater than 0 and greater than the start index!")
                .mapToResult(), LOG
        )
    }
}