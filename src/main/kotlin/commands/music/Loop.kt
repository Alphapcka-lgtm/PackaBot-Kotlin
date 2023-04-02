package commands.music

import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import music.Player
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.JDALogger
import utils.ExceptionWebhook
import utils.RestActionExecutor

@SlashCommandAnnotation
object Loop : ExecutableSlashCommand, RestActionExecutor() {

    /**
     * Name of the mode option.
     */
    const val OPTION_NAME = "mode"

    override val LOG = JDALogger.getLog(Loop::class.java)
    override val name = "loop"
    override val slashCommandData = createLoopCommand()

    /**
     * Creates the slash command data
     */
    private fun createLoopCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "To set the bot into loop mode or turn loop mode off.")

        val mode = OptionData(OptionType.STRING, "mode", "The loop mode.", true)
        val offChoice = Choice("turn off", 0)
        val currentChoice = Choice("current", 1)
        val queueChoice = Choice("queue", 2)
        mode.addChoices(offChoice, currentChoice, queueChoice)
        commandData.addOptions(mode)

        commandData.isGuildOnly = true
        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {

        val _interaction = hook.interaction

        // command is guild only, so interaction.guild is never null
        val player = Player.getAudioPlayerFromGuild(_interaction.guild!!) ?: return notActiveMessage(hook)

        if (!_interaction.member!!.voiceState!!.inAudioChannel() || _interaction.member!!.voiceState!!.channel != player.channel) {
            queueSendMessageRestActionMapping(
                hook.sendMessage("You need to be in the voice channel with the bot.").mapToResult(), LOG
            )
            return
        }

        val mode = interaction.getOption(OPTION_NAME, OptionMapping::getAsInt)
        if (mode == null) {
            queueSendMessageRestActionMapping(hook.sendMessage("You need to provide a loop mode.").mapToResult(), LOG)
            return
        }

        when (mode) {
            0 -> {
                player.turnOffLoop()
                queueSendMessageRestActionMapping(
                    hook.sendMessage("The player is no longer in loop mode.").mapToResult(), LOG
                )
            }

            1 -> {
                player.loop()
                queueSendMessageRestActionMapping(
                    hook.sendMessage("The player will now repeat the current song.").mapToResult(), LOG
                )
            }

            2 -> {
                player.loopQueue()
                queueSendMessageRestActionMapping(
                    hook.sendMessage("The player will now repeat the queue.").mapToResult(), LOG
                )
            }

            else -> {
                queueSendMessageRestActionMapping(hook.sendMessage("Unknown loop mode: $mode!").mapToResult(), LOG)
                val errorMes = "Unknown loop mode: $mode!"
                LOG.error(errorMes)
                ExceptionWebhook.sendException(Exception(errorMes), _interaction.jda)
            }
        }
    }
}