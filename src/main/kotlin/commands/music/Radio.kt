package commands.music

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import commands.voice.JoinVoice
import music.Player
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.Result
import net.dv8tion.jda.internal.utils.JDALogger
import utils.PythonDiscordColors
import utils.RestActionExecutor
import java.io.InputStreamReader
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.annotation.Nonnull

@SlashCommandAnnotation
object Radio : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG = JDALogger.getLog(Radio::class.java)

    private const val RADIO_OPTION_NAME: String = "radio"

    /** name of the one choice to be shown, when the available radios could not be loaded from the csv resource file.*/
    private const val UNABLE_TO_LOAD_RADIOS_CHOICE = "n/A"
    private var radiosLoadingError = false

    private val radioLines = ArrayList<Array<String>>()

    override val name = "radio"
    override val slashCommandData = createRadioCommand()

    init {
        radioLines.addAll(readRadioCsv())
    }

    /**
     * Creates the slash command data
     */
    private fun createRadioCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Plays one of the radios.")
        commandData.isGuildOnly = true

        val radioOption = OptionData(OptionType.STRING, RADIO_OPTION_NAME, "the radio channel", false, true)
        commandData.addOptions(radioOption)

        return commandData
    }

    /**
     * Reads the lines of the radio csv file.
     * @throws IllegalStateException if the resource csv file could not be found.
     */
    @Throws(IllegalStateException::class)
    private fun readRadioCsv(): MutableList<Array<String>> {
        val resourceString = "data/radio.csv"
        val cvsFileStream = this.javaClass.classLoader.getResourceAsStream(resourceString)
            ?: throw IllegalStateException("Resource $resourceString not found!")
        val parser = CSVParserBuilder().withSeparator(';').build()
        val reader = CSVReaderBuilder(InputStreamReader(cvsFileStream)).withSkipLines(1).withCSVParser(parser).build()

        return reader.readAll()
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        val _interaction = hook.interaction

        if (radiosLoadingError) {
            return queueSendMessageRestActionMapping(
                hook.sendMessage("We where unable to load the available radios when starting the bot. We are sorry for the inconvenience. :pleading_face:")
                    .mapToResult(), LOG
            )
        }

        // if no radioOption was set, it returns the method and replies with an error mes.
        val radioOption = interaction.getOption(RADIO_OPTION_NAME, OptionMapping::getAsString)
            ?: return queueSendMessageEmbedRestActionMapping(
                hook.sendMessageEmbeds(radioListEmbed(_interaction.user)).mapToResult(), LOG
            )

        // guild is never null, because command is guild only
        var player = Player.getAudioPlayerFromGuild(_interaction.guild!!)

        if (player == null || !player.audioManager.isConnected) {
            // can not do it like this, discord throws an error when trying to reply to an slash command interaction that was already replied to.
            JoinVoice.execute(interaction, hook)
            player = Player.getAudioPlayerFromGuild(interaction.guild!!)
        }

        player?.loadAndPlay(hook, radioOption)
    }

    /**
     * Creates a message embed with a table of all available radio channels.<br>
     * The given Collection needs to be String-Arrays with (at least) 3 Array-Entries for the columns in the sort of 'name'    'description'   'url'.
     * The url-column contains the url (link, path, etc.) to the audio source.
     *
     * @param user       the user that send the command. (never <code>null</code>)
     * @return a message embed with the table of all available radio channels.
     */
    private fun radioListEmbed(@Nonnull user: User): MessageEmbed {
        val names = StringBuilder()
        val descr = StringBuilder()

        val embed = EmbedBuilder()
        embed.setColor(PythonDiscordColors.BLUE.color)
        embed.setFooter("Radios", user.effectiveAvatarUrl)
        embed.setTitle("Radio channels:")

        val iter = radioLines.iterator()
        while (iter.hasNext()) {
            val str = iter.next()
            names.append(str[0])
            descr.append(str[1])
            if (iter.hasNext()) {
                names.appendLine()
                descr.appendLine()
            }
        }

        embed.addField("Name", names.toString(), true)
        embed.addField("Description", descr.toString(), true)
        embed.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))

        return embed.build()
    }

    /**
     * Method for the autocompletion event of the Radio command.
     */
    fun commandAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        if (event.isAcknowledged) {
            LOG.error("Command auto complete interaction event already acknowledged! $event")
            return
        }

        if (event.focusedOption.name == RADIO_OPTION_NAME) {

            var replyMapRestAction: RestAction<Result<Void>>

            try {
                val choices = ArrayList<Command.Choice>(radioLines.size)

                for (line in radioLines) {
                    val radioName = line[0]
                    val radioDesc = line[1]
                    val url = line[2]
                    choices.add(Command.Choice(radioName, url))
                }
                replyMapRestAction = event.replyChoices(choices).mapToResult()
            } catch (e: IllegalStateException) {
                LOG.warn("Unable to load radio csv file!", e)
                replyMapRestAction = event.replyChoice("n/a", UNABLE_TO_LOAD_RADIOS_CHOICE).mapToResult()
                radiosLoadingError = true
            }

            replyMapRestAction.queue { result ->
                result.onFailure { error ->
                    LOG.error("Error when reply choices to command auto complete interaction event!", error)
                }
            }
            return
        }

        LOG.warn("Unknown option in command auto completion event in Radio command! ${event.focusedOption.name}")
    }
}