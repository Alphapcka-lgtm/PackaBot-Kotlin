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
object Pause : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG: Logger = JDALogger.getLog(Pause::class.java)
    override val name = "pause"
    override val slashCommandData = createPauseCommand()

    /**
     * Creates the slash command data
     */
    private fun createPauseCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Pause the music.")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {

        val _interaction = hook.interaction

        // command is guild only
        val player = Player.getAudioPlayerFromGuild(_interaction.guild!!)
        val userChannel = _interaction.member!!.voiceState!!.channel
        val channel = _interaction.guild!!.audioManager.connectedChannel

        if (channel == null || player == null) {
            return notActiveMessage(hook)
        }

        if (userChannel == null || userChannel != channel) {
            return queueSendMessageRestActionMapping(
                hook.sendMessage("You need to be in the voice channel with the bot.").mapToResult(), LOG
            )
        }

        if (player.audioPlayer.isPaused) {
            return queueSendMessageRestActionMapping(hook.sendMessage("Bot is already pause.").mapToResult(), LOG)
        }

        if (player.pause()) {
            return queueSendMessageRestActionMapping(hook.sendMessage("Bot is now paused.").mapToResult(), LOG)
        }

        queueSendMessageRestActionMapping(hook.sendMessage("Currently not playing anything").mapToResult(), LOG)
    }
}