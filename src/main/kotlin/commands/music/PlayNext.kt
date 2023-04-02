package commands.music

import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import commands.voice.JoinVoice
import music.Player
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.JDALogger
import org.slf4j.Logger
import utils.RestActionExecutor

@SlashCommandAnnotation
object PlayNext : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG: Logger = JDALogger.getLog(PlayNext::class.java)
    override val name = "play_next"
    override val slashCommandData = createPlayNextCommand()

    private const val OPTION_NAME = "url"

    /**
     * Creates the slash command data.
     */
    private fun createPlayNextCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Adds the given song on top of the queue.")
        commandData.isGuildOnly = true

        commandData.addOption(OptionType.STRING, OPTION_NAME, "The music url.", true)
        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        val _interaction = hook.interaction

        val url = interaction.getOption(OPTION_NAME, OptionMapping::getAsString)
            ?: return queueSendMessageRestActionMapping(
                hook.sendMessage("${hook.interaction.user.asMention} you need to specify an url!").mapToResult(), LOG
            )

        //checks if the users is connected to an audio channel
        if (!_interaction.member!!.voiceState!!.inAudioChannel()) {
            return userNotConnectedToAudioChannelMessage(hook)
        }

        // interaction.guild is never null because command is guild only
        var player = Player.getAudioPlayerFromGuild(_interaction.guild!!)
        if (player == null || !player.audioManager.isConnected) {
            JoinVoice.execute(interaction, hook)
            player = Player.getAudioPlayerFromGuild(_interaction.guild!!)
        }

        // if the bot was not able to connect to the voice channel
        if (player == null || !player.audioManager.isConnected) {
            return
        }

        //if the play is not in the same vc as the user, it joins their channel
        if (player.audioManager.connectedChannel != _interaction.member!!.voiceState!!.channel) {
            JoinVoice.channelConnect(_interaction.member!!.voiceState!!.channel!!, player.audioManager, hook)
            player.channel = _interaction.member!!.voiceState!!.channel!!.asVoiceChannel()
        }


        // if the player is not playing anything, it uses the normal play command.
        if (!player.isPlaying) {
            return Play.execute(interaction, hook)
        }

        player.playNext(hook, url)
    }
}