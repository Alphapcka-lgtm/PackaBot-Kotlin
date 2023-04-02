package music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import commands.voice.LeaveVoice
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import music.queue_embed.QueueEmbedController
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.managers.AudioManager
import net.dv8tion.jda.internal.utils.JDALogger
import okhttp3.internal.wait
import tune_in_radio.TuneInAudioOutline
import utils.RestActionExecutor
import java.awt.Color
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.annotation.Nonnull
import kotlin.concurrent.schedule

/**
 * The player of the music bot.
 *
 * @param audioManager The [AudioManager] of the [Guild] the [Player] got created for. (never
 * `null`)
 * @param channel      The [VoiceChannel] the [Player] is connected to. (never `null`)
 * @param boundChannel The [MessageChannel] the player is bound to. All future Messages will be sent to the
 * bounded channel.
 * @author Michael
 * @version 0.1.0
 * @since 0.0.1
 */
class Player private constructor(
    @Nonnull audioManager: AudioManager, @Nonnull channel: VoiceChannel,
    @Nonnull boundChannel: MessageChannel,
) {
    /** the logger */
    private val LOG = JDALogger.getLog(Player::class.java)

    /**
     * the size of the duration slide for the now-playing-embed
     */
    private val durationSliderSize = 30

    /**
     * Gets the [AudioManager] of the [Player].
     *
     * @return The [AudioManager].
     */
    /**
     * the audio manager of the player
     */
    val audioManager: AudioManager

    /**
     * the voice channel the player is connected to
     */
    private val m_Channel: VoiceChannel
    /**
     * Gets the audio player.
     *
     * @return the audio player
     */
    /**
     * the audio player of the player
     */
    var audioPlayer: AudioPlayer
    /**
     * Gets the [AudioPlayerManager] of the [Player].
     *
     * @return The [AudioPlayerManager].
     */
    /**
     * the player manager of the player
     */
    val playerManager: AudioPlayerManager
    /**
     * Gets the [TrackScheduler] of the [Player].
     *
     * @return The [TrackScheduler].
     */
    /**
     * the track scheduler of the player
     */
    val trackScheduler: TrackScheduler
    /**
     * Gets the channel the bot is currently bound to.
     *
     * @return the bound channel.
     */
    /**
     * the message channel that the bot is bound to.<br></br>
     * When a message can not be send of an interaction hook, it will fall back to this channel, no matter in what
     * channel the original message of the interaction hook was sent in.
     */
    val boundChannel: MessageChannel

    /**
     * Task to be executed onece the bot is inactive. This task gets cancled by [.loadAndPlay],
     * [.playNext], [.resume]
     */
    private val inactiveTask: TimerTask

    /**
     * Timer of the player to execute timer tasks on.
     */
    private val timer: Timer

    /**
     * Interaction hook of a slash command event. Used for the [DefaultPackaBotAudioLoadResultHandler] to send messages to.
     * <br></br>
     * If the hook has expired, the message will be sent to the bound channel.
     */
    private var m_Hook: InteractionHook? = null
    /**
     * Indicates if the player is shuffled.
     *
     * @return `true` when the player is shuffled.
     */
    /**
     * `true` if the player is shuffled
     */
    var isShuffled = false
        private set

    /**
     * The [MessageEmbed] of the currently playing [AudioTrack]
     */
    val nowPlayingEmbed: MessageEmbed
        get() {
            val currentTrack = audioPlayer.playingTrack
            val data = currentTrack.userData as TrackData
            if (data.isTuneInRadio) {
                return createTuneInRadioAudioTrackEmbed(data.tuneInAudio!!, data.user)
            }
            return createDefaultAudioTrackEmbed(currentTrack, data.user, data.thumbnailUrl)
        }

    /**
     * Constructor to create a [Player].
     */
    init {
        Objects.requireNonNull(audioManager, "audioManager must not be null!")
        Objects.requireNonNull<Any>(channel, "channel must not be null!")
        Objects.requireNonNull(boundChannel, "boundChannel must not be null!")
        this.audioManager = audioManager
        m_Channel = channel
        this.boundChannel = boundChannel
        playerManager = DefaultAudioPlayerManager()
        AudioSourceManagers.registerRemoteSources(playerManager)
        audioPlayer = playerManager.createPlayer()
        audioPlayer.volume = 50
        trackScheduler = TrackScheduler(this)
        audioPlayer.addListener(trackScheduler)
        val audioSendHandler = AudioPlayerSendHandler(audioPlayer)
        audioManager.sendingHandler = audioSendHandler

        inactiveTask = object : TimerTask() {
            override fun run() {
                println("Inactive Task execution!")
                // normal 'this' is a TimerTask Object, Player.this is the player object.
                LeaveVoice.executeWithMessageChannelAndPlayer(boundChannel, this@Player)
            }
        }
        timer = Timer("TimerInactive#" + this.audioManager.guild)
        timer.schedule(TimeUnit.HOURS.toMillis(3)) { inactiveTask.run() }
    }

    companion object {
        /**
         * [List] with all active [Player]. (List elements never `null`)
         */
        @Nonnull
        private val PLAYER_LIST: MutableList<Player> = LinkedList()

        /**
         * Creates a [Player].
         * @param audioManager The [AudioManager] of the [Guild] the [Player] got created for. (never
         * `null`)
         * @param channel      The [VoiceChannel] the [Player] is connected to. (never `null`)
         * @param boundChannel The [MessageChannel] the player is bound to. All future Messages will be sent to the
         * bounded channel.
         */
        fun createPlayer(
            @Nonnull audioManager: AudioManager, @Nonnull channel: VoiceChannel,
            @Nonnull boundChannel: MessageChannel,
        ): Player {
            val player = Player(audioManager, channel, boundChannel)
            PLAYER_LIST.add(player)
            return player
        }

        /**
         * Gets the Player of the guild.
         *
         * @param guild The [Guild] to get the player from. (never `null`)
         * @return The Player or `null` if no one exists yet.
         */
        fun getAudioPlayerFromGuild(guild: Guild): Player? {
            Objects.requireNonNull(guild, "guild must not be null!")
            for (player in PLAYER_LIST) {
                if (player.audioManager.guild == guild) {
                    return player
                }
            }
            return null
        }

        /**
         * Removes the [Player] of the given [Guild] from the player list.
         *
         * @param guild The [Guild] who's [Player] should be removed. (never `null`)
         * @return `true` if this list contained the [Player].
         */
        fun removePlayerFromGuild(@Nonnull guild: Guild): Boolean {
            Objects.requireNonNull(guild, "guild must not be null!")
            val player = getAudioPlayerFromGuild(guild) ?: return true
            return PLAYER_LIST.remove(player)
        }

        /**
         * Destroys all players and stops playing tracks. Removes the players from the list.
         */
        fun destroyAll() {
            // Creates for each player object a own ThreadPool to execute async
            PLAYER_LIST.forEach { player: Player ->
                runBlocking {
                    async {
                        while (player.audioPlayer.playingTrack != null) {
                            if (player.audioPlayer.playingTrack.info.isStream) {
                                player.audioPlayer.destroy()
                                break
                            } else {
                                val duration = player.audioPlayer.playingTrack.duration
                                try {
                                    delay(duration)
                                    player.wait()
                                } catch (e: InterruptedException) {
                                    player.audioPlayer.destroy()
                                    e.printStackTrace()
                                    break
                                }
                            }
                        }
                        player.timer.cancel()
                    }
                }
            }

            PLAYER_LIST.clear()
        }

        /**
         * Removes the given [Player] from the player list.
         *
         * @param player The [Player] to remove. (never `null`)
         * @return `true` if this list contained the [Player].
         */
        fun removePlayer(@Nonnull player: Player): Boolean {
            Objects.requireNonNull(player)
            return PLAYER_LIST.remove(player)
        }
    }

    /**
     * Destroy the player and stop playing track. Removes it self from the player list.
     */
    fun destroy() {
        trackScheduler.queue.clear()
        audioPlayer.destroy()
        timer.cancel()
        removePlayer(this)
    }

    /**
     * Skips the current track and returns an Optional with the skipped track.
     *
     * @return an optional with the skipped track. If no value is present, the player wasn't playing anything.
     */
    fun skipCurrent(): Optional<AudioTrack> {
        inactiveTask.cancel()
        val track = audioPlayer.playingTrack
        if (track != null) {
            audioPlayer.stopTrack()
        }
        return Optional.ofNullable(track)
    }

    /**
     * Skips the track at the index and returns an Optional with the skipped track.
     *
     * @return an optional with the skipped track. If no value is present, the index was out of bounce of the queue.
     */
    fun skipNumber(index: Int): Optional<AudioTrack> {
        inactiveTask.cancel()
        return Optional.ofNullable(trackScheduler.skipTrack(index))
    }

    /**
     * Skips the tracks from x to y (including x and y).
     *
     * @param x The start point to skip from.
     * @param y The end point to skip to.
     * @return an int value which indicates the current response: <br></br>
     *
     *  * -1 when x < 0
     *  * -2 when y < 0
     *  * -3 when x >= the length of the queue
     *  * -4 when y > the length of the queue
     *  * 1 when the tracks from x to y where skipped
     *
     */
    fun skipFromTo(x: Int, y: Int): Int {
        var x = x
        inactiveTask.cancel()
        if (x < 0) {
            return -1
        }
        if (y < 1) {
            return -2
        }
        val queueLength = trackScheduler.queueLength
        if (x >= queueLength) {
            return -3
        }
        if (y > queueLength) {
            return -4
        }
        trackScheduler.skipTracks(x, y)
        if (x == 0) {
            x = 1
        }
        return 1
    }
    /**
     * Pauses the playing player.
     *
     * @param millis the milliseconds until the bot should leave the channel because in inactivity. The milliseconds
     * must be greater or equal than 0.
     * @return `true` if the bot is now paused. `false` if the bot is not playing anything
     * currently.
     * @throws IllegalArgumentException When millis are < 0.
     */
    /**
     * Pauses the playing player.
     *
     * @return `true` if the bot is now paused. `false` if the bot is not playing anything
     * currently.
     */
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun pause(millis: Long = TimeUnit.HOURS.toMillis(3)): Boolean {
        require(millis >= 0) { "Delay for the inactive task time must be >= 0!" }

        timer.schedule(millis) {
            inactiveTask.run()
        }
        val track = audioPlayer.playingTrack
        return if (track == null) {
            false
        } else {
            audioPlayer.isPaused = true
            true
        }
    }

    /**
     * Resumes the paused player.
     *
     * @return `true` if the bot now resumes playing. `false` if the bot has nothing to play.
     */
    fun resume(): Boolean {
        timer.cancel()
        inactiveTask.cancel()
        val track = audioPlayer.playingTrack
        return if (track == null) {
            false
        } else {
            audioPlayer.isPaused = false
            true
        }
    }

    /**
     * Loads the specified identifier and adds it to the queue.<br></br>
     * The identifier is set in the option "url" of the [SlashCommandEvent]
     *
     * @param hook the hook of the slash command interaction. (never `null`)
     * @param url  the url of the song to add. (never `null`)
     * @return the future operation of the audio item loading. If you need for what ever reason to wait for it, you can do it over this object.
     */
    fun loadAndPlay(@Nonnull hook: InteractionHook, @Nonnull url: String): Future<Void> {
        m_Hook = Objects.requireNonNull(hook, "hook must not be null!")
        Objects.requireNonNull(url, "url must not be null!")
        return playerManager.loadItem(url, DefaultPackaBotAudioLoadResultHandler(hook, this))
    }

    /**
     * Loads the specified identifier and adds it to the queue with a custom [AudioLoadResultHandler].<br></br>
     * The identifier may be set in the option "url" of the [SlashCommandEvent]
     *
     * @param url           the url of the song to add. (never `null`)
     * @param resultHandler the custom results handler. (never `null`)
     * @return the future operation of the audio item loading. If you need for what ever reason to wait for it, you can do it over this object.
     */
    fun loadAndPlay(@Nonnull url: String, @Nonnull resultHandler: AudioLoadResultHandler): Future<Void> {
        Objects.requireNonNull(url, "url must not be null!")
        Objects.requireNonNull(resultHandler, "resultHandler must not be null")
        inactiveTask.cancel()
        return playerManager.loadItem(url, resultHandler)
    }

    /**
     * Adds the specified identifier to the top of the queue.<br></br>
     * The identifier is set in the options "url" of the [SlashCommandEvent]
     *
     * @param hook the hook of the slash command interaction. (never `null`)
     * @param url  the url of the song to add. (never `null`)
     */
    fun playNext(@Nonnull hook: InteractionHook, @Nonnull url: String) {
        m_Hook = Objects.requireNonNull(hook, "hook must not be null!")
        Objects.requireNonNull(url, "url must not be null!")
        inactiveTask.cancel()
        playerManager.loadItem(url, object : DefaultPackaBotAudioLoadResultHandler(hook, this@Player) {

            override fun trackLoaded(track: AudioTrack) {
                track.userData = hook.interaction.user
                if (audioPlayer.playingTrack == null) {
                    audioPlayer.startTrack(track, true)
                }
                trackScheduler.queueStart(track)
                RestActionExecutor.queueSendMessageRestActionMapping(
                    hook.sendMessage("**" + track.info.title + "** has been added to the top of the queue")
                        .mapToResult(), LOG
                )
            }
        })
    }

    /**
     * Creates the default embed for an [AudioTrack]
     * @param track the track to create for.
     * @param user the user that requested this track.
     */
    private fun createDefaultAudioTrackEmbed(track: AudioTrack, user: User, thumbnailUrl: String?): MessageEmbed {
        val trackInfo = track.info
        val eb = EmbedBuilder()
        eb.setColor(Color(51, 51, 255))
        eb.setAuthor(trackInfo.author)
        eb.setTitle(trackInfo.title, trackInfo.uri)
        eb.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))
        if (!trackInfo.isStream) {
            // if it's a regular video
            val position = track.position
            val duration = trackInfo.length
            val formattedTimestamps = formatTimestampMillis(position, duration)
            val positionStr = formattedTimestamps[0]
            val durationStr = formattedTimestamps[1]
            eb.setDescription(createDurationSlider(position, duration, durationSliderSize))
            eb.addField("", "`$positionStr / $durationStr`", false)
        } else {
            // if it's a stream
            val duration = track.duration
            val durationStr = formatDuration(duration)
            eb.setDescription(createDurationSlider(duration, duration, durationSliderSize))
            eb.addField("", "Live since: `$durationStr`", false)
        }
        eb.addField("", "`Requested by:` " + user.asMention, false)
        eb.setThumbnail(thumbnailUrl)
        return eb.build()
    }

    /**
     * Creates the tune in radio embed for an [AudioTrack] of an [TuneInAudioOutline]
     * @param track the track to create for.
     * @param outline the outline of the tune in radio
     * @param user the user that requested the radio.
     */
    private fun createTuneInRadioAudioTrackEmbed(
        outline: TuneInAudioOutline,
        user: User,
    ): MessageEmbed {
        val builder = EmbedBuilder()
        builder.setColor(Color(51, 51, 255))
        builder.setTitle(outline.text)
        builder.setDescription(outline.profile.description)
        builder.setThumbnail(outline.image.toString())
        builder.addField("genre", outline.category.title, false)
        builder.addField("", "`Requested by:` " + user.asMention, false)
        builder.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))

        return builder.build()
    }

    /**
     * Creates the duration slider for the now playing song embed.
     *
     * @param position the position of the current song
     * @param duration the length of the song
     * @param size     the size the slider should have
     * @return the created duration slider.
     */
    private fun createDurationSlider(position: Long, duration: Long, size: Int): CharSequence {
        val dash = "▬"
        val marker = "🔘"
        val markerPosition =
            Math.round(java.lang.Float.valueOf(position.toFloat()) / java.lang.Float.valueOf(duration.toFloat()) * size)
        require(markerPosition <= size) { "Error while creating DurationSilder: markerPosition > size [$markerPosition > $size]" }
        val sb = StringBuilder()
        sb.append('`')
        for (i in 0 until markerPosition) {
            sb.append(dash)
        }
        sb.append(marker)
        for (i in 0 until size - markerPosition) {
            sb.append(dash)
        }
        sb.append('`')
        return sb.toString()
    }

    /**
     * Formats the given position and duration into hh:mm:ss where hours are not displayed when not bigger than 0.
     *
     * @param position The position of the song (in milliseconds)
     * @param duration The duration of the song (in milliseconds)
     * @return A String array with the formatted Strings (positionStr, formatStr)
     */
    private fun formatTimestampMillis(position: Long, duration: Long): Array<String?> {
        val ret = arrayOfNulls<String>(2)
        val positionStr: String
        val durationStr: String
        if (TimeUnit.MILLISECONDS.toHours(duration) > 0) {
            positionStr = String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(position),
                TimeUnit.MILLISECONDS.toMinutes(position) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(position)
                ),
                TimeUnit.MILLISECONDS.toSeconds(position) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(position)
                )
            )
            durationStr = String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(duration)
                ),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(duration)
                )
            )
        } else {
            positionStr = String.format(
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(position) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(position)
                ),
                TimeUnit.MILLISECONDS.toSeconds(position) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(position)
                )
            )
            durationStr = String.format(
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(duration)
                ),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(duration)
                )
            )
        }
        ret[0] = positionStr
        ret[1] = durationStr
        return ret
    }

    /**
     * Shuffles the queue of the music bot.
     *
     * @return returns `true` of the bot is now shuffled, `false` if the bot is no long shuffled.
     */
    fun shuffle(): Boolean {
        isShuffled = !isShuffled
        return if (isShuffled) {
            trackScheduler.shuffleQueue()
            true
        } else {
            trackScheduler.unshuffleQueue()
            false
        }
    }

    /**
     * Reshuffles the queue of the music bot. If the [Player] isn't already set to shuffled, it sets it.
     */
    fun reshuffle() {
        if (!isShuffled) {
            shuffle()
            return
        }
        trackScheduler.shuffleQueue()
    }

    /**
     * Displays the current queue of the music bot.
     *
     * @param hook the interaction hook the slash command event. (never `null`)
     */
    fun queue(@Nonnull hook: InteractionHook) {
        val controller = QueueEmbedController
        controller.addQueueEmbed(this, hook)
    }

    /**
     * Formats the duration of a song.
     *
     * @param duration the duration in milliseconds
     * @return a String with formatted duration in the format HH:mm:ss or mm:ss
     */
    private fun formatDuration(duration: Long): String {
        val durationStr: String
        durationStr = if (TimeUnit.MILLISECONDS.toHours(duration) > 0) {
            String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(duration)
                ),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(duration)
                )
            )
        } else {
            String.format(
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(duration)
                ),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(duration)
                )
            )
        }
        return durationStr
    }

    /**
     * Loops The currently playing song.
     */
    fun turnOffLoop() {
        trackScheduler.setLooped(false, trackScheduler.getLoopMode())
    }

    /**
     * Loops The currently playing song.
     */
    fun loop() {
        trackScheduler.setLooped(true, LoopMode.SINGLE)
    }

    /**
     * Loops the queue.
     */
    fun loopQueue() {
        trackScheduler.setLooped(true, LoopMode.QUEUE)
    }

    /**
     * Restarts the currently playing song.
     *
     * @return the restarted track.
     */
    fun restart(): AudioTrack {
        val currentTrack = audioPlayer.playingTrack
        val clonedTrack = currentTrack.makeClone()
        trackScheduler.queueStart(clonedTrack)
        audioPlayer.stopTrack()
        return clonedTrack
    }

    /**
     * Clears the queue and stops the current song.
     */
    fun clearQueue() {
        trackScheduler.clearQueue()
        audioPlayer.playingTrack.stop()
    }

    /**
     * Schedules the [.inactiveTask] when the queue is empty.<br></br>
     * Gets notified by [.m_TrackScheduler]
     */
    fun onQueueEmpty() {
        timer.schedule(TimeUnit.MINUTES.toMillis(5)) {
            inactiveTask
        }
    }

    /**
     * Gets the [VoiceChannel] of the [Player].
     *
     * @return The [VoiceChannel].
     */
    var channel: VoiceChannel? = null
        get() = m_Channel

    /**
     * Gets the length of the queue.
     *
     * @return the length of the queue.
     */
    val queueLength: Int
        get() = trackScheduler.queueLength

    /**
     * Indicates if the player is currently playing something.
     *
     * @return `true` when the player is currently playing something.
     */
    val isPlaying: Boolean
        get() = audioPlayer.playingTrack != null
}