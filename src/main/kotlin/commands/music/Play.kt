package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import commands.voice.JoinVoice
import music.Player
import music.TrackData
import music.spotify.SpotifyProvider
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.JDALogger
import utils.RestActionExecutor
import utils.SpotifyToYtMusicResolver
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.function.Function
import java.util.stream.Collectors


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

            val resolver = SpotifyToYtMusicResolver()

            val embedBuilder = EmbedBuilder()
            embedBuilder.setDescription("Found a spotify url. Trying to resolve it with yt-music.\nPlease be patient.")
            embedBuilder.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))
            embedBuilder.setFooter("spotify", hook.interaction.user.avatarUrl)
            val embed = hook.sendMessageEmbeds(embedBuilder.build()).submit()

            val urls = try {
                resolver.resolve(url)
            } catch (e: IllegalStateException) {
                hook.sendMessage("The provided spotify item is invalid.\nWe are only able to try and find spotify albums, tracks and playlists.")
                return
            }

            val futures = ArrayList<Future<Void>>(urls.size)

            for (url in urls) {
                val future = player.loadAndPlay(url, object : AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {

                        track.userData =
                            TrackData(
                                hook.interaction.user,
                                "https://i.ytimg.com/vi/${track.identifier}/maxresdefault.jpg",
                                null
                            )
                        player.trackScheduler.queue(track)


                        // TODO
                        if (player.audioPlayer.playingTrack == null) {
                            player.audioPlayer.startTrack(track, true)
                        }
//                        restAction.mapToResult().queue { result ->
//                            result.onFailure { error ->
//                                DefaultPackaBotAudioLoadResultHandler.LOG.warn(
//                                    "Error when sending playing track message!",
//                                    error
//                                )
//                            }
//                        }
                        // can occur

                    }

                    override fun playlistLoaded(playlist: AudioPlaylist) {
                        for (track in playlist.tracks) {
                            track.userData = TrackData(
                                hook.interaction.user,
                                "https://i.ytimg.com/vi/${track.identifier}/maxresdefault.jpg",
                                null
                            )
                            if (player.audioPlayer.playingTrack == null) {
                                player.audioPlayer.playTrack(track)
                            }
                        }

                        // can occur
                    }

                    override fun noMatches() {
                        TODO("Not yet implemented")
                        // no matches should never occur
                    }

                    override fun loadFailed(exception: FriendlyException) {
                        TODO("Not yet implemented")
                        // well, shit happens
                    }

                })

                futures.add(future)
            }

            val cfs: Array<CompletableFuture<Void>> = futures.toArray(arrayOfNulls(futures.size))

            val fs = CompletableFuture.allOf(*cfs)
                .thenApply(
                    Function<Void, Any> { ignored: Void ->
                        futures.stream()
                            .map { obj -> obj.get() }
                            .collect(Collectors.toList())
                    }
                )

            fs.whenComplete { _, e ->
                if (e != null) {
                    throw e // TODO: what to do when an exception occurred
                }

                // TODO
            }

            return // always return here, [loadAndPlay] will never find something from a spotify url!
        }


        player.loadAndPlay(hook, url)
    }
}