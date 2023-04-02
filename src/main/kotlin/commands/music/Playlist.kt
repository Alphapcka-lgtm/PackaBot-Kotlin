package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import commands.SlashCommandController
import commands.voice.JoinVoice
import database.dao.PlaylistDao
import music.Player
import music.TrackScheduler
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.internal.utils.JDALogger
import utils.PythonDiscordColors
import utils.RestActionExecutor
import java.sql.SQLException
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.ExecutionException
import javax.annotation.Nonnull
import javax.annotation.Nullable


@SlashCommandAnnotation
object Playlist : ExecutableSlashCommand, RestActionExecutor() {

    override val LOG = JDALogger.getLog(Playlist::class.java)

    private const val PLAYLIST_OPTION_NAME = "playlist"

    private const val ERROR_LOADING_PLAYLIST_OPTIONS = -1L
    private const val PLAYLIST_WITHOUT_ID = -2L
    private const val NO_DATABASE_CONNECTION = -3L

    override val name = "playlist"

    override val slashCommandData = createPlaylistCommand()

    /**
     * Creates the slash command data.
     */
    private fun createPlaylistCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Select a playlist to play.")
        commandData.isGuildOnly = true

        val optionData = OptionData(OptionType.INTEGER, PLAYLIST_OPTION_NAME, "The playlist to play.")
        optionData.isRequired = true
        optionData.isAutoComplete = true

//        val choicePlaylist9 = Choice("Playlist 9", 3L)
//        optionData.addChoices(choicePlaylist9)
        commandData.addOptions(optionData)

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        val _interaction = hook.interaction

        /** the error mes when responding to the command failed. */
        val errorMes = "Error when responding to command [$name]"

        // interaction.guild is never null, because command is guild only
        var player = Player.getAudioPlayerFromGuild(_interaction.guild!!)

        if (player == null) {
            // can not do it like this, discord throws an error when trying to reply to an slash command interaction that was already replied to.
            JoinVoice.execute(interaction, hook)
            player = Player.getAudioPlayerFromGuild(interaction.guild!!)
        }

        val playlistId = interaction.getOption(PLAYLIST_OPTION_NAME, OptionMapping::getAsLong)
            ?: return hook.sendMessage("${_interaction.member!!.asMention} you need to specify a playlist!")
                .queue()

        if (playlistId == ERROR_LOADING_PLAYLIST_OPTIONS) {
            return queueRestActionResult(
                hook.sendMessage("${_interaction.member!!.asMention} An error occurred when loading all available playlist options! Please try again later.")
                    .mapToResult(), LOG, errorMes
            )
        }

        if (playlistId == PLAYLIST_WITHOUT_ID) {
            return queueRestActionResult(
                hook.sendMessage("${_interaction.member!!.asMention} An error occured when loading the selected playlist! Please try again later.")
                    .mapToResult(), LOG, errorMes
            )
        }

        if (playlistId == NO_DATABASE_CONNECTION) {
            return queueRestActionResult(
                hook.sendMessage("${_interaction.member!!.asMention} The bot currently can not connect to the database! Please try again later.")
                    .mapToResult(), LOG, errorMes
            )
        }

        try {
            val connection = SlashCommandController.databaseConnection
                ?: return queueSendMessageRestActionMapping(
                    hook.sendMessage("${_interaction.member!!.asMention} unable to load the playlist. Bot does not have a database connection.")
                        .mapToResult(), LOG
                ); LOG.warn("Unable to execute playlist command: No database connection!")

            val playlistDao = PlaylistDao()
            val playlistOpt = playlistDao.get(connection, playlistId)

            if (playlistOpt.isEmpty) {
                val notFoundEmbed = EmbedBuilder()
                notFoundEmbed.setColor(PythonDiscordColors.BLURPLE.color)
                notFoundEmbed.setDescription("Playlist not found.")
                queueSendMessageRestActionMapping(hook.sendMessageEmbeds(notFoundEmbed.build()).mapToResult(), LOG)
                return
            }

            val playlist = playlistOpt.get()

            val message = hook.sendMessageEmbeds(
                createLoadingPlaylistEmbed(
                    playlist.name,
                    _interaction.user.effectiveAvatarUrl
                )
            ).submit()

            val unableToLoad = HashSet<String>()
            val loadedTracks = HashSet<AudioTrack>()

            for (track in playlist.tracks) {
                //at this point player can not be null!
                val future = player!!.loadAndPlay(track.audioUrl, object : AudioLoadResultHandler {
                    override fun trackLoaded(track: AudioTrack) {
                        LOG.info("DB playlist track $track loaded.")
                        loadedTracks.add(track)
                    }

                    override fun playlistLoaded(playlist: AudioPlaylist) {
                        LOG.info("DP playlist loaded playlist $playlist.")
                        loadedTracks.addAll(playlist.tracks)
                    }

                    override fun noMatches() {
                        LOG.warn("DP playlist track $track not found!")
                        unableToLoad.add("${track.name} - ${track.author} [$track]: {not found}")
                    }

                    override fun loadFailed(exception: FriendlyException) {
                        LOG.warn("Loading DB track $track failed!", exception)
                        unableToLoad.add("${track.name} - ${track.author} [$track] {exception}")
                    }

                })

                try {
                    future.get()
                } catch (e: InterruptedException) {
                    LOG.error("Error while waiting for the loading of the track!", e)
                    queueSendMessageRestActionMapping(
                        hook.sendMessage("Unexpected error while loading playlist!").mapToResult(), LOG
                    )
                    return
                } catch (e: ExecutionException) {
                    LOG.error("Error while waiting for the loading of the track!", e)
                    queueSendMessageRestActionMapping(
                        hook.sendMessage("Unexpected error while loading playlist!").mapToResult(), LOG
                    )
                    return
                }
            }

            if (unableToLoad.size > 0) {
                LOG.warn("${unableToLoad.size} Tracks could not be loaded! Tracks: {$unableToLoad}")
            }

            val audioPlayer = player!!.audioPlayer

            //adds all tracks to the queue
            for (track in loadedTracks) {
                track.userData = _interaction.user

                /**
                 * We need to queue the track before playing, to avoid an exception in the method [TrackScheduler.onTrackStart]
                 */
                player.trackScheduler.queue(track)
                //If the player is shuffled, see next if-statement.
                //If the player is not playing anything and is not shuffled, the player should start playing the first track.
                if (audioPlayer.playingTrack == null && !player.isShuffled) {
                    audioPlayer.playTrack(track)
                }
            }

            if (player.isShuffled) {
                player.trackScheduler.shuffleQueue()
                val firstTrack = player.trackScheduler.queue.first
                audioPlayer.playTrack(firstTrack)
            }

            try {
                queueEditMessageRestActionMapping(
                    message.get().editMessageEmbeds(
                        createPlaylistEmbed(
                            loadedTracks,
                            playlist.name,
                            _interaction.user.effectiveAvatarUrl
                        )
                    ).mapToResult(), LOG
                )
            } catch (e: ExecutionException) {
                LOG.error("Error when updating loading embed message!", e)
                queueSendMessageEmbedRestActionMapping(
                    player.boundChannel.sendMessageEmbeds(
                        createPlaylistEmbed(
                            loadedTracks, playlist.name,
                            _interaction.user.effectiveAvatarUrl
                        )
                    ).mapToResult(), LOG
                )
            } catch (e: InterruptedException) {
                LOG.error("Error when updating loading embed message!", e)
                queueSendMessageEmbedRestActionMapping(
                    player.boundChannel.sendMessageEmbeds(
                        createPlaylistEmbed(
                            loadedTracks, playlist.name,
                            _interaction.user.effectiveAvatarUrl
                        )
                    ).mapToResult(), LOG
                )
            }
        } catch (e: SQLException) {
            LOG.error("Unable to get database connection!", e)
            queueSendMessageRestActionMapping(hook.sendMessage("Unable loading playlist.").mapToResult(), LOG)
        }
    }

    /**
     * Creates the message embed that the playlist has been loaded.
     *
     * @param loadedTracks  the loaded tracks. (never `null`)
     * @param playlistName  the name of the playlist.
     * @param userAvatarUrl the avatar url of the user that sent the command.
     * @return the message embed that the playlist has been loaded.
     */
    private fun createPlaylistEmbed(
        @Nonnull loadedTracks: Set<AudioTrack>, @Nullable playlistName: String,
        @Nullable userAvatarUrl: String,
    ): MessageEmbed {
        val update = EmbedBuilder()
        update.setColor(PythonDiscordColors.BLURPLE.color)
        update.setTitle("**Playlist added to queue**")
        update.setDescription(playlistName)
        update.addField("Enqueued", "`" + loadedTracks.size + "` songs", true)
        update.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))
        update.setFooter("playlist", userAvatarUrl)
        return update.build()
    }

    /**
     * Creates the loading playlist embed.
     * @param playlistName name of the playlist
     * @param effectiveAvatarUrl the url of the effective avatar of the user that send the command.
     */
    private fun createLoadingPlaylistEmbed(playlistName: String, effectiveAvatarUrl: String): MessageEmbed {
        val loadingEmbed = EmbedBuilder()
        loadingEmbed.setColor(PythonDiscordColors.BLURPLE.color)
        loadingEmbed.setDescription("Loading playlist `${playlistName}`...")
        loadingEmbed.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))
        loadingEmbed.setFooter("", effectiveAvatarUrl)

        return loadingEmbed.build()
    }

    /**
     * Method for the autocompletion event of the Radio command.
     */
    fun commandOptionAutoComplete(event: CommandAutoCompleteInteractionEvent) {

        if (event.isAcknowledged) {
            LOG.error("Command auto complete interaction event already acknowledged! $event")
            return
        }

//        val connection = SlashCommandController.databaseConnection
//            ?: return hook.sendMessage("${_interaction.member!!.asMention} unable to load the playlist. Bot does not have a database connection.")
//                .queue(); LOG.warn("Unable to execute playlist command: No database connection!")
        try {
            val connection =
                SlashCommandController.databaseConnection
                    ?: return event.replyChoice("Unable to load playlist choices!", ERROR_LOADING_PLAYLIST_OPTIONS)
                        .mapToResult()
                        .queue { result ->
                            result.onFailure { error ->
                                LOG.warn("Unable to reply choice!", error)
                            }
                        }

            val playlistDao = PlaylistDao()
            val playlists = playlistDao.getAll(connection)

            if (playlists.isEmpty()) {
                return event.replyChoice("Unable to load playlist choices!", ERROR_LOADING_PLAYLIST_OPTIONS)
                    .mapToResult().queue { result ->
                        result.onFailure { error ->
                            LOG.warn("Unable to reply choice!", error)
                        }
                    }
            }

            val choices = ArrayList<Choice>(playlists.size)
            for (playlist in playlists) {
                choices.add(Choice(playlist.name, playlist.id ?: PLAYLIST_WITHOUT_ID))
            }

            event.replyChoices(choices).mapToResult().queue { result ->
                result.onFailure { error ->
                    LOG.warn("Error when reply choices!", error)
                }
            }

        } catch (e: SQLException) {
            LOG.warn("Error when retrieving playlists from DB!", e)

            return event.replyChoice("Unable to load playlist choices!", ERROR_LOADING_PLAYLIST_OPTIONS).mapToResult()
                .queue { result ->
                    result.onFailure { error ->
                        LOG.warn("Unable to reply choice!", error)
                    }
                }
        }
    }
}