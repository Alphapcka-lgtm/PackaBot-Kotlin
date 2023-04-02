package commands.music

import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import commands.voice.JoinVoice
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import music.Player
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.internal.utils.JDALogger
import org.slf4j.Logger
import selection_menus.SelectionMenuController
import tune_in_radio.TuneIn
import tune_in_radio.TuneInAudioLoadResultHandler
import tune_in_radio.TuneInAudioOutline
import utils.RestActionExecutor
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.function.BiConsumer

@SlashCommandAnnotation
object TuneInRadio : ExecutableSlashCommand {
    override val LOG: Logger = JDALogger.getLog(TuneInRadio::class.java)

    override val name = "tune_in"

    const val nameSearchSubcommand = "search"
    const val searchOptionName = "query"

    /**
     * Hash map to store the Tune In Audio Outlines after searching by guild.
     * This is used to ensure that on multiple calls from multiple guilds no audio outlines will get mixed up.
     */
    private val SEARCH_OUTLINES = HashMap<String, List<TuneInAudioOutline>>()

    /**
     * Execution when an option of the selection menu has been chosen.
     */
    private val selectionEventExecution = ExecutionConsumer().biConsumer

    override val slashCommandData: SlashCommandData = createTuneInRadioCommand()

    private fun createTuneInRadioCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Play a radio from tune in radio.")
        commandData.isGuildOnly = true
        commandData.addSubcommands(
            SubcommandData(
                nameSearchSubcommand,
                "Search for radios."
            ).addOption(OptionType.STRING, searchOptionName, "The query to search for.", true)
        )

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        // currently no check for which sub command was called, since there is only one (yet).

        val query = interaction.getOption(searchOptionName, OptionMapping::getAsString)
            ?: return RestActionExecutor.queueSendMessageRestActionMapping(
                hook.sendMessage("${hook.interaction.user.asMention} you need to specify a query!").mapToResult(), LOG
            )

        val tuneIn = TuneIn()
        val result = tuneIn.search(query)

        if (result.isError()) {
            LOG.warn("Error when requesting search from TuneIn! ${result.errorResponse.toString()}")
            return RestActionExecutor.queueSendMessageRestActionMapping(
                hook.sendMessage("${hook.interaction.user.asMention} Error when searching query _${query}_: **${result.errorResponse!!.fault}**")
                    .mapToResult(), LOG
            )
        }

        val outlines = result.audioOutlines!!

        SEARCH_OUTLINES[interaction.guild!!.id] = outlines

        val outlinesSubs = listToSublistsOf25(outlines)
        var menus = createSelectMenus(outlinesSubs)
        val embed = createSearchResultsEmbed(outlines, query, interaction.jda)

        /*
         * A message can contain at most 5 action rows. To make sure that there are not more than 5 action,
         * this statement as implemented.
         */
        if (menus.size > 5) {
            menus = menus.subList(0, 5)
            LOG.warn("TuneIn Results fill more the 5 Action Rows! $menus")
        }

        hook.setEphemeral(true).sendMessageEmbeds(embed).setEphemeral(true).addActionRow(menus).setEphemeral(true)
            .mapToResult()
            .queue { result ->
                result.onSuccess { message ->
                    for (menu in menus) {
                        SelectionMenuController.addSelectionMenuExecution(
                            menu.id!!,
                            selectionEventExecution,
                            menu,
                            message
                        )
                    }
                }.onFailure { error ->
                    LOG.error("Error when sending embed with search results from tune in!", error)
                }
            }

    }

    private fun createSearchResultsEmbed(outlines: List<TuneInAudioOutline>, query: String, jda: JDA): MessageEmbed {
        val builder = EmbedBuilder()
        builder.setAuthor(jda.selfUser.name, null, jda.selfUser.effectiveAvatarUrl)
        builder.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))

        if (outlines.isEmpty()) {
            builder.setTitle("No search results found for query: _${query}_")
            return builder.build()
        }

        builder.setTitle("TuneIn results for query: _${query}_")
        builder.setDescription("Please select a result with the Select Menus(s) below.")
        builder.addField("Found:", outlines.size.toString(), false)

        return builder.build()
    }

    private fun createSelectMenus(outlinesSubs: List<List<TuneInAudioOutline>>): MutableList<SelectMenu> {

        val menus = ArrayList<SelectMenu>()

        for (i in outlinesSubs.indices) {
            val outlines = outlinesSubs[i]
            val menu = StringSelectMenu.create("TuneInRadio#$i")

            menu.placeholder = "Choose the radio station."
            menu.setRequiredRange(1, 1)

            for (d in outlines.indices) {
                val radio = outlines[d]
                menu.addOption(radio.text, (i * 25 + d).toString(), radio.subtext ?: "")
            }

            menus.add(menu.build())
        }

        return menus
    }

    /**
     *  Splits the given list into sublists with up max 25 entries.
     */
    private fun listToSublistsOf25(lst: List<TuneInAudioOutline>): List<List<TuneInAudioOutline>> {
        val maxSize = 25

        val full = lst.size / maxSize
        val rest = lst.size % maxSize

        val sublists = ArrayList<List<TuneInAudioOutline>>()

        var index = 0
        for (i in 0 until full) {
            sublists.add(lst.subList(index, index + maxSize))
            index += maxSize
        }

        sublists.add(lst.subList(index, index + rest))
        return sublists
    }

    /**
     * Class to better organize the code and make it less confusing.
     */
    private class ExecutionConsumer {
        val biConsumer = BiConsumer<StringSelectInteractionEvent, InteractionHook> { event, hook ->
            runBlocking {
                val option = event.selectedOptions[0]

                if (!SEARCH_OUTLINES.containsKey(event.guild!!.id)) {
                    return@runBlocking RestActionExecutor.queueSendMessageRestActionMapping(
                        hook.sendMessage("No data found in your Guild! Please try using the _/${name}_-command again later.")
                            .setEphemeral(true).mapToResult(), LOG
                    )
                }

                val outlines = SEARCH_OUTLINES.remove(event.guild!!.id)!!

                try {
                    val position = option.value.toInt()
                    val outline = outlines[position]
                    val url = outline.url.toString()

                    val tuneIn = TuneIn()
                    val futureGenre = retrieveGenre(tuneIn, outline.genreId)
                    val futureProfile = retrieveProfile(tuneIn, outline.guideId)

                    val genre = futureGenre.await()
                    val profile = futureProfile.await()

                    outline.category = genre
                    outline.profile = profile

                    // guild is never null, because command is guild only
                    var player = Player.getAudioPlayerFromGuild(event.guild!!)

                    if (player == null || !player.audioManager.isConnected) {
                        JoinVoice.executeWithoutSlashCommand(event.guild!!, event.messageChannel, hook)
                        player = Player.getAudioPlayerFromGuild(event.guild!!)
                    }

                    player?.loadAndPlay(url, TuneInAudioLoadResultHandler(hook, player, outline))

                } catch (e: NumberFormatException) {
                    LOG.error("Error when parsing value of ${SelectOption::class.java}!", e)
                    RestActionExecutor.queueSendMessageRestActionMapping(
                        hook.sendMessage("${event.member!!.asMention} Error when parsing selection! You can thank to programmer for that one...")
                            .setEphemeral(true)
                            .mapToResult(), LOG
                    )
                }
            }
        }

        private suspend fun retrieveGenre(tuneIn: TuneIn, genreId: String?) = coroutineScope {
            return@coroutineScope async {
                return@async tuneIn.genre(genreId)
            }
        }

        private suspend fun retrieveProfile(tuneIn: TuneIn, guidId: String?) = coroutineScope {
            return@coroutineScope async {
                return@async tuneIn.profile(guidId)
            }
        }
    }

}