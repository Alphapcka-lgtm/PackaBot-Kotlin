package music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import net.dv8tion.jda.internal.utils.JDALogger
import utils.RestActionExecutor
import java.util.*
import javax.annotation.Nonnull

/**
 * Listener that gets notified when an audio event happens.
 *
 * @param player The player this [TrackScheduler] belongs to.
 * @author Michael
 * @version 1.0
 * @see AudioEventAdapter
 *
 * @since 0.0.1
 */
open class TrackScheduler(@Nonnull player: Player) : AudioEventAdapter() {

    /** the logger */
    private val LOG = JDALogger.getLog(TrackScheduler::class.java)

    /**
     * Gets the queue of the bot.
     *
     * @return The queue.
     */
    /**
     * The queue with all songs to play. This queue can get shuffled, if it gets unshuffled, the invertedShuffleQueue
     * will be set as queue.
     */
    val queue = LinkedList<AudioTrack>()
    /**
     * Gets the unshuffled queue.
     *
     * @return The unshuffled queue. This queue can never be shuffled.
     */
    /**
     * Queue which never gets shuffled. When the queue should get unshuffled, this queue will be set as the normal
     * queue.
     */
    val unshuffledQueue = LinkedList<AudioTrack>()
    /**
     * Gets the player of this TrackScheduler.
     *
     * @return The player of the TrackScheduler.
     */
    /**
     * The [Player] this track scheduler belongs to.
     */
    val player: Player
    /**
     * Indicates if the bot is looped
     *
     * @return `true`if the bot is looped. Other wise `false`.
     */
    /**
     * If the player is looped
     */
    var isLooped = false
        private set

    /**
     * The type of the loop
     */
    internal var loopMode: LoopMode? = null

    /**
     * Default Constructor.
     */
    init {
        this.player = player
    }

    /**
     * Adds the [AudioTrack] to the queue.
     *
     * @param track The track to add. (never `null`)
     */
    fun queue(@Nonnull track: AudioTrack) {
        Objects.requireNonNull(track, "track must not be null!")
        queue.add(track)
        unshuffledQueue.add(track)
    }

    /**
     * Adds the [AudioTrack] to the top of the queue.
     *
     * @param track The track to add to the top of the queue. (never `null`)
     */
    fun queueStart(@Nonnull track: AudioTrack) {
        Objects.requireNonNull(track, "track must not be null!")
        queue.addFirst(track)
        unshuffledQueue.addFirst(track)
    }

    val queueDuration: Long
        get() {
            var queueDuration: Long = 0
            for (audioTrack in queue) {
                queueDuration += audioTrack.duration
            }
            return queueDuration
        }

    /**
     * Gets the length of the queue
     *
     * @return The length of the queue
     */
    val queueLength: Int
        get() = queue.size

    internal operator fun next(): AudioTrack? {
        while (queue.iterator().hasNext()) {
            return queue.iterator().next()
        }
        return null
    }

    internal operator fun hasNext(): Boolean {
        return queue.iterator().hasNext()
    }

    /**
     * Sets if the bot is looped.
     *
     * @param loop `true` if the bot should be looped.
     */
    fun setLooped(loop: Boolean, loopMode: LoopMode?) {
        isLooped = loop
        this.loopMode = loopMode
    }

    /**
     * Gets the [LoopMode]
     *
     * @return The mode of the loop.
     */
    fun getLoopMode(): LoopMode? {
        return loopMode
    }

    /**
     * Removes the [AudioTrack] at the given index from the queue.
     *
     * @param index the index of the track to remove.
     *
     * @return the skipped track or null if you of bounce.
     */
    fun skipTrack(index: Int): AudioTrack? {

        if (index > queue.size) {
            return null
        }

        val track = queue.removeAt(index)
        unshuffledQueue.remove(track)

        return track
    }

    /**
     * Removes the [AudioTrack] from x to y from the queue (including x and y).
     *
     * @param x The start point to skip from.
     * @param y The end point to skip to.
     */
    fun skipTracks(x: Int, y: Int) {
        // Do it with list toRemove to avoid possible IndexOutOfBoundsException
        val toRemove = LinkedList<AudioTrack?>()
        for (i in x..y + 1) {
            toRemove.add(queue[x - 1])
        }
        queue.removeAll(toRemove)
        unshuffledQueue.removeAll(toRemove)
    }

    override fun onPlayerPause(player: AudioPlayer) {
        // Do nothing.
    }

    override fun onPlayerResume(player: AudioPlayer) {
        // Do nothing.
    }

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        queue.remove()
        unshuffledQueue.remove(track)
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        // Start next track
        if (isLooped) {
            when (loopMode) {
                LoopMode.SINGLE -> player.startTrack(track.makeClone(), true)
                LoopMode.QUEUE -> {
                    if (hasNext()) {
                        player.startTrack(next(), true)
                    }
                    queue(track.makeClone())
                }

                else -> throw FriendlyException(
                    "Loop Error: Unknown loopMode! [$loopMode]!", FriendlyException.Severity.FAULT,
                    null
                )
            }
        } else {
            if (hasNext()) {
                player.playTrack(next())
            } else {
                this.player.onQueueEmpty()
            }
        }
        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        // clone of this back to your queue
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        // An already playing track threw an exception (track end event will still be received separately)
        LOG.error("Error while playing track [$track][${track.info.title}]!", exception)
        player.stopTrack()
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
        println("threshold: $thresholdMs")
        player.stopTrack()

        RestActionExecutor.queueSendMessageRestActionMapping(
            this.player.boundChannel.sendMessage("Track `${track.info.title}` is stuck. Skipping track...")
                .mapToResult(), LOG
        )
    }

    /**
     * Removes all Elements from the Queue<br></br>
     * *Also clears the unshuffledQueue*
     */
    fun clearQueue() {
        queue.clear()
        unshuffledQueue.clear()
    }

    fun shuffleQueue() {
        Collections.shuffle(queue)
    }

    fun unshuffleQueue() {
        // remove all elements.
        queue.clear()
        // use addAll(index, collection) method both queues can be final.
        queue.addAll(0, unshuffledQueue)
    }
}