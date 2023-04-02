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
object Resume : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG: Logger = JDALogger.getLog(Resume::class.java)
    override val name = "resume"
    override val slashCommandData = createResumeCommand()

    /**
     * Creates the slash command data.
     */
    private fun createResumeCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Resumes the bot when it is paused.")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {

        val _interaction = hook.interaction
        // interaction.guild can never be null, because command is guild only
        val player = Player.getAudioPlayerFromGuild(_interaction.guild!!)

        val userChannel = _interaction.member!!.voiceState!!.channel
        val channel = _interaction.guild!!.audioManager.connectedChannel

        if (player == null || channel == null) {
            return notActiveMessage(hook)
        }

        if (userChannel == null || userChannel != channel) {
            return userNotInVcOfBot(hook)
        }

        if (!player.audioPlayer.isPaused) {
            return queueSendMessageRestActionMapping(hook.sendMessage("Bot is not paused.").mapToResult(), LOG)
        }

        if (player.resume()) {
            return queueSendMessageRestActionMapping(hook.sendMessage("Bot will resume playing").mapToResult(), LOG)
        }

        queueSendMessageRestActionMapping(hook.sendMessage("Currently not playing anything.").mapToResult(), LOG)
    }
}