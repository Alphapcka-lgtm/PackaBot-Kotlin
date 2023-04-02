package commands.music

import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import music.Player
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.JDALogger
import org.slf4j.Logger
import utils.RestActionExecutor
import java.util.concurrent.TimeUnit

@SlashCommandAnnotation
object Seek : ExecutableSlashCommand {

    override val LOG: Logger = JDALogger.getLog(Seek::class.java)
    override val name = "seek"
    override val slashCommandData = createSeekCommand()

    private const val HOUR_OPTION_NAME = "hour"
    private const val MIN_OPTION_NAME = "min"
    private const val SEC_OPTION_NAME = "sec"

    private fun createSeekCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "To seek a position in a track.")
        commandData.isGuildOnly = true

        val hourOption =
            OptionData(
                OptionType.INTEGER,
                HOUR_OPTION_NAME,
                "The hour of the track you want to jump to.",
                true
            ).setMinValue(0)

        val minOption =
            OptionData(
                OptionType.INTEGER,
                MIN_OPTION_NAME,
                "The minute of the track you want to jump to.",
                true
            ).setMinValue(0)

        val secOption = OptionData(
            OptionType.INTEGER,
            SEC_OPTION_NAME,
            "The seconds of the track you want to jump to.",
            true
        ).setMinValue(0)

        commandData.addOptions(hourOption, minOption, secOption)

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        val _interaction = hook.interaction

        val player = Player.getAudioPlayerFromGuild(_interaction.guild!!)
        if (player == null || !player.audioManager.isConnected) {
            return notActiveMessage(hook)
        }

        if (!player.isPlaying) {
            return RestActionExecutor.queueSendMessageRestActionMapping(
                hook.sendMessage("Currently not playing anything.").mapToResult(),
                NowPlaying.LOG
            )
        }

        val hour = interaction.getOption(HOUR_OPTION_NAME, OptionMapping::getAsLong)
            ?: return RestActionExecutor.queueSendMessageEmbedRestActionMapping(
                // interaction.member is never null, because command is guild only
                hook.sendMessage("${_interaction.member!!.asMention} you need to specify the hour!").mapToResult(),
                LOG
            )

        val min = interaction.getOption(MIN_OPTION_NAME, OptionMapping::getAsLong)
            ?: return RestActionExecutor.queueSendMessageEmbedRestActionMapping(
                // interaction.member is never null, because command is guild only
                hook.sendMessage("${_interaction.member!!.asMention} you need to specify the minutes!").mapToResult(),
                LOG
            )

        val sec = interaction.getOption(SEC_OPTION_NAME, OptionMapping::getAsLong)
            ?: return RestActionExecutor.queueSendMessageEmbedRestActionMapping(
                // interaction.member is never null, because command is guild only
                hook.sendMessage("${_interaction.member!!.asMention} you need to specify the seconds!").mapToResult(),
                LOG
            )

        val track = player.audioPlayer.playingTrack!!
        if (!track.isSeekable) {
            return RestActionExecutor.queueSendMessageRestActionMapping(
                hook.sendMessage("${_interaction.member!!.asMention} The currently playing track is not seekable!")
                    .mapToResult(), LOG
            )
        }

        val seekTimeMillis =
            TimeUnit.HOURS.toMillis(hour) + TimeUnit.MINUTES.toMillis(min) + TimeUnit.SECONDS.toMillis(sec)

        if (seekTimeMillis > track.duration) {
            return RestActionExecutor.queueSendMessageRestActionMapping(
                hook.sendMessage("${_interaction.member!!.asMention} The given seek time is greater than the duration of the track!")
                    .mapToResult(), LOG
            )
        }

        track.position = seekTimeMillis

        RestActionExecutor.queueSendMessageRestActionMapping(
            hook.sendMessage("Seeking to `$hour:$min:$sec`.").mapToResult(), LOG
        )
    }
}