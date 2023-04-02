package commands.voice

import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import music.Player
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.JDALogger
import org.slf4j.Logger
import utils.RestActionExecutor

@SlashCommandAnnotation
object LeaveVoice : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG: Logger = JDALogger.getLog(LeaveVoice::class.java)
    override val name = "leave"
    override val slashCommandData: SlashCommandData = createLeaveVoiceCommand()

    /**
     * Creates the join voice command data.
     * @return the created slash command data.
     */
    private fun createLeaveVoiceCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Leaves the voice channel.")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        val _interaction = hook.interaction
        val member = hook.interaction.member!!

        // leave command is guild only so it can only be called from a guild.
        if (_interaction.guild!!.audioManager.isConnected) {
            if (member.voiceState!!.inAudioChannel()) {
                val uservoice = member.voiceState!!.channel!!.asVoiceChannel()
                val player = Player.getAudioPlayerFromGuild(_interaction.guild!!)
                val botvoice = player!!.audioManager.connectedChannel!!.asVoiceChannel()

                if (uservoice != botvoice) {
                    queueSendMessageRestActionMapping(
                        hook.sendMessage("${_interaction.user.asMention} you need to be connected to the same voice channel to perform this action!")
                            .mapToResult(), LOG
                    )
                    return
                }

                queueSendMessageRestActionMapping(hook.sendMessage("Leaving `${botvoice.name}`...").mapToResult(), LOG)
                player.audioManager.closeAudioConnection()
                if (player.isShuffled) {
                    queueSendMessageRestActionMapping(hook.sendMessage("Bot is no longer shuffled.").mapToResult(), LOG)
                }
                player.destroy()
            }

            return
        }

        queueSendMessageRestActionMapping(
            hook.sendMessage("${_interaction.user.asMention} you need to be connected to the same voice channel to perform this action!")
                .mapToResult(), LOG
        )

    }

    /**
     * Leaves the voice channel.
     *
     * @param channel the channel the player is bound to
     * @param player the player to leave.
     */
    fun executeWithMessageChannelAndPlayer(channel: MessageChannel, player: Player) {

        queueSendMessageRestActionMapping(
            channel.sendMessage("Leaving `${channel.name}` due to inactivity...").mapToResult(), LOG
        )
        player.audioManager.closeAudioConnection()
        if (player.isShuffled) {
            queueSendMessageRestActionMapping(channel.sendMessage("Bot is no longer shuffled.").mapToResult(), LOG)
        }
        player.destroy()
    }

}