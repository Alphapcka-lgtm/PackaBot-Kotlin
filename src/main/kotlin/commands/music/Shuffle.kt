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
object Shuffle : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG: Logger = JDALogger.getLog(Shuffle::class.java)
    override val name = "shuffle"
    override val slashCommandData = createShuffleCommand()

    /**
     * Creates the slash command data
     */
    private fun createShuffleCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Shuffles the bot or unshuffles it, if already shuffled.")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        val _interaction = hook.interaction

        // interaction.guild is never null, because command is guild only
        var player = Player.getAudioPlayerFromGuild(_interaction.guild!!)

        var shuffled = false
        if (player == null) {
            val audioManager = _interaction.guild!!.audioManager
            audioManager.isAutoReconnect = true
            val channel = _interaction.member!!.voiceState!!.channel
                ?: return userNotConnectedToAudioChannelMessage(hook)
            val boundChannel = _interaction.messageChannel
            player = Player.createPlayer(audioManager, channel.asVoiceChannel(), boundChannel)
            shuffled = player.shuffle()

        } else {
            shuffled = player.shuffle()
        }

        if (!shuffled) {
            queueSendMessageRestActionMapping(hook.sendMessage("Bot is no longer shuffled.").mapToResult(), LOG)
            return
        }

        queueSendMessageRestActionMapping(hook.sendMessage("Bot is now shuffled.").mapToResult(), LOG)
    }
}