package commands.music

import PROPERTIES
import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import commands.voice.JoinVoice
import music.Player
import music.spotify.SpotifyProvider
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.JDALogger
import org.alphapacka.com.YTMusic
import org.alphapacka.com.enums.SearchFilters
import org.alphapacka.com.pojos.search.SearchResult
import se.michaelthelin.spotify.enums.ModelObjectType
import utils.RestActionExecutor

@SlashCommandAnnotation
object Play : ExecutableSlashCommand, RestActionExecutor() {

    /** the logger */
    override val LOG = JDALogger.getLog(Play::class.java)

    override val name = "play"
    private const val URL_OPTION_NAME = "url"

    private val spotifyProvider =
        SpotifyProvider(PROPERTIES.getProperty("spotify.clientId"), PROPERTIES.getProperty("spotify.clientSecret"))
    private val ytMusic = YTMusic(null, null, null)

    /**
     * Creates the slash command data.
     */
    private fun createPlayCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Play music from the given URL or search for a track on YouTube.")
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

        // check if the given url is from spotify
        if (SpotifyProvider.isSpotifyUrl(url)) {
            // search for the given item(s) on yt music
            val res = try {
                spotifyProvider.retrieve(url)
            } catch (e: IllegalStateException) {
                hook.sendMessage("The provided spotify item is invalid.\nWe are only able to try and find spotify albums, tracks and playlists.")
                return
            }

            when (res.type) {
                ModelObjectType.TRACK -> {
                    val track = res.objectAsTrack()
                    val songsResult =
                        ytMusic.search("${track.artists.joinToString()} - ${track.name}", SearchFilters.SONGS).stream()
                            .map(SearchResult::asSong).toList()
                    for (song in songsResult) {
                        // TODO: Algorithm for finding a suitable object.
                        // songs results don't have a `type`
                        val spotifyArtistNames = track.artists.asList().map { el -> el.name }
                        val ytArtistNames = song.artists.map { el -> el.name }
                        var hasAllArtists = true
                        for (ytArtist in ytArtistNames) {
                            if (!spotifyArtistNames.contains(ytArtist)) {
                                hasAllArtists = false
                            }
                        }

                        if (song.title == track.name && hasAllArtists) {
                            
                        }
                    }
                }

                ModelObjectType.ALBUM -> {

                }

                ModelObjectType.PLAYLIST -> {

                }

                else -> {
                    hook.sendMessage("The provided spotify item is invalid.\nWe are only able to try and find spotify albums, tracks and playlists.")
                    return
                }
            }

            return // always return here, [loadAndPlay] will never find something from a spotify url!
        }


        player.loadAndPlay(hook, url)
    }
}