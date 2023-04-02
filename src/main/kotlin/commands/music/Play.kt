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
import utils.RestActionExecutor

@SlashCommandAnnotation
object Play : ExecutableSlashCommand, RestActionExecutor() {

    /** the logger */
    override val LOG = JDALogger.getLog(Play::class.java)

    override val name = "play"
    private const val URL_OPTION_NAME = "url"

    /**
     * Creates the slash command data.
     */
    private fun createPlayCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Play music from the given URL or search for a track.")
        commandData.isGuildOnly = true
        commandData.addOption(OptionType.STRING, URL_OPTION_NAME, "The music url.", true)

        return commandData
    }

    override val slashCommandData: SlashCommandData = createPlayCommand()

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        val url = interaction.getOption(URL_OPTION_NAME, OptionMapping::getAsString)
            ?: return queueSendMessageRestActionMapping(
                hook.sendMessage("${hook.interaction.user.asMention} you need to specify an url!").mapToResult(), LOG
            )

        // command is guild only
        var player = Player.getAudioPlayerFromGuild(interaction.guild!!)

        if (player == null || !player.audioManager.isConnected) {
            JoinVoice.execute(interaction, hook)
            player = Player.getAudioPlayerFromGuild(interaction.guild!!)
        }

        // if the bot was not able to connect to the voice channel
        if (player == null || !player.audioManager.isConnected) {
            LOG.info("Bot not connected!")
            return
        }

        player.loadAndPlay(hook, url)
    }
}