package music.queue_embed

import kotlinx.coroutines.*
import music.Player
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.internal.utils.JDALogger
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.Nonnull

/**
 * Controller for queue embeds.
 *
 * @author Michael
 */
object QueueEmbedController {

    /** the logger */
    val LOG = JDALogger.getLog(QueueEmbedController::class.java)

    /**
     * Coroutine for checking if the duration of the queue embeds has expired.
     */
    val m_QueueEmbedExperationChecker: Job

    /**
     * the duration time until an active queue embed will be destroyed
     */
    private val DURATION_TIME = TimeUnit.MINUTES.toMillis(10L)

    /**
     * mapping of the channel (channel id) and the queue embed in the channel.
     */
    private val m_ChannelQueueEmbedMap = HashMap<Long, QueueEmbed>()

    /**
     * Constructor
     */
    init {
        m_QueueEmbedExperationChecker = CoroutineScope(Dispatchers.Default).launch {
            val toRemove = ArrayList<Long>(m_ChannelQueueEmbedMap.size)
            for ((key, value) in m_ChannelQueueEmbedMap) {
                if (value.timeCreated + DURATION_TIME <= System.currentTimeMillis()) {
                    value.destroy()
                    toRemove.add(key)
                }
            }

            for (id in toRemove) {
                m_ChannelQueueEmbedMap.remove(id)
                LOG.info("Removed queue embed $id")
            }
            delay(TimeUnit.MINUTES.toMillis(5))
        }
    }

    /**
     * Adds and sends a new embed to the channel of the interaction hook and maps the embed with the channel id.
     *
     * @param player the player for which the queue embed should be created. (never `null`)
     * @param hook   the interaction hook of the slash command event. (never `null`)
     * @return the added queue embed
     */
    fun addQueueEmbed(@Nonnull player: Player, @Nonnull hook: InteractionHook): QueueEmbed? {
        val channelId = hook.interaction.channel!!.idLong
        // When a new queue embed should be created and the controller still has an active queue embed for that channel,
        // it should destroy it first
        if (m_ChannelQueueEmbedMap.containsKey(channelId) && m_ChannelQueueEmbedMap[channelId]!!.isActive) {
            m_ChannelQueueEmbedMap[channelId]!!.destroy()
        }
        val queueEmbed = QueueEmbed(
            hook.interaction.guild!!, hook,
            player.audioPlayer.playingTrack, player.trackScheduler.queue
        )
        return m_ChannelQueueEmbedMap.put(hook.interaction.channel!!.idLong, queueEmbed)
    }

    /**
     * Updates the active queue embed in the channel and returns an optional with the queue embed that was updated. If
     * no active embed exists for the given channel, it will return an empty optional.
     *
     * @param channelId the channel id of the queue embed.
     * @param direction the direction in which it should be updated. Use [QueueEmbedUpdateDirection.FORWARD] to go
     * for e.g. from page 1 to page 2 and [QueueEmbedUpdateDirection.BACKWARD] to go from e.g. page 2 to page 1.
     * (never `null`)
     * @return an Optional with the queue embed that was updated or an empty optional if no queue embed to update was
     * found.
     */
    fun updateQueueEmbed(
        @Nonnull channelId: Long,
        @Nonnull direction: QueueEmbedUpdateDirection?,
    ): Optional<QueueEmbed> {
        if (!hasActiveQueueEmbed(channelId)) {
            return Optional.empty()
        }
        val queueEmbed = m_ChannelQueueEmbedMap[channelId]
        queueEmbed!!.updateEmbed(direction!!)
        return Optional.of(queueEmbed)
    }

    /**
     * Indicates if there is an active queue embed for the given channel id.
     *
     * @param channelId the id of the channel to check for.
     * @return `true` if an active queue embed is present, `false` if not.
     */
    fun hasActiveQueueEmbed(@Nonnull channelId: Long): Boolean {
        return m_ChannelQueueEmbedMap.containsKey(channelId) && m_ChannelQueueEmbedMap[channelId]!!.isActive
    }

}