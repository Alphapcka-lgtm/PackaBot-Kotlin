package music.queue_embed

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import music.Player
import music.TrackData
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.internal.interactions.component.ButtonImpl
import net.dv8tion.jda.internal.utils.JDALogger
import java.awt.Color
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.Nonnull
import kotlin.math.roundToInt

/**
 * Class to represent a queue embed.
 *
 * @author Michael
 */
class QueueEmbed(
    @Nonnull guild: Guild, @Nonnull hook: InteractionHook,
    @Nonnull currentTrack: AudioTrack, @Nonnull queue: List<AudioTrack>,
) {

    /** the logger */
    private val LOG = JDALogger.getLog(QueueEmbed::class.java)

    companion object {

        /**
         * Referred to from Discord as :arrow_forward:
         */
        const val UNICODE_EMOTE_ARROW_FORWARD = "U+25b6"

        /**
         * Referred to from Discord as :arrow_backward:
         */
        const val UNICODE_EMOTE_ARROW_BACKWARD = "U+25c0"

        /**
         * the id for the forward button
         */
        const val ID_BTN_FORWARD = "button_forward"

        /**
         * the id for the backward button
         */
        const val ID_BTN_BACKWARD = "button_backward"

        /**
         * button go forward to the next page of the queue embed
         */
        val BTN_FORWARD = ButtonImpl(
            ID_BTN_FORWARD, "Forward", ButtonStyle.PRIMARY, false,
            Emoji.fromUnicode(UNICODE_EMOTE_ARROW_FORWARD)
        )

        /**
         * button the go backwards to the next page of the queue embed
         */
        val BTN_BACKWARD = ButtonImpl(
            ID_BTN_BACKWARD, "Backward", ButtonStyle.PRIMARY, false,
            Emoji.fromUnicode(UNICODE_EMOTE_ARROW_BACKWARD)
        )
    }

    /**
     * the guild
     */
    private val m_Guild: Guild

    /**
     * the player
     */
    private val m_Player: Player?

    /**
     * the embed that is sent
     */
    private var m_Embed: MessageEmbed? = null

    /**
     * the current playing track when creating the embed
     */
    private val m_CurrentTrack: AudioTrack

    /**
     * the queue of the embed
     */
    private val m_Queue: List<AudioTrack>

    /**
     * list with all avaiable pages of the queue embed.
     */
    private var m_EmbedPages: List<MessageEmbed>? = null

    /**
     * the message sent
     */
    private var m_Message: Message? = null

    /**
     * the duration of the queue
     */
    private var m_QueueDuration: Long = 0
    /**
     * Gets the time stamp when it was created in milliseconds.
     *
     * @return the time stamp when it was created in milliseconds.
     */
    /**
     * the time when it was created
     */
    val timeCreated = System.currentTimeMillis()

    /**
     * the max pages
     */
    private val m_Pages: Int

    /**
     * the current page
     */
    private var m_Page: Int
    /**
     * Indicates if the embed is active.
     *
     * @return `true` if the embed is active and can be updated, otherwise `false`.
     */
    /**
     * if the embed is active
     */
    var isActive = false
        private set

    /**
     * Constructor.
     *
     * @param guild        the guild to create the embed in. (never `null`)
     * @param hook         the interaction hook the event. (never `null`)
     * @param currentTrack the current playing track. (never `null`)
     * @param queue        the queue. (never `null`)
     */
    init {
        m_Guild = Objects.requireNonNull(guild, "guild must not be null")
        m_Queue = Objects.requireNonNull(queue, "queue must not be null")
        m_CurrentTrack = Objects.requireNonNull(currentTrack, "currentTrack must not be null")
        m_Page = 0
        m_Pages = (m_Queue.size.toDouble() / 10.0).roundToInt() + 1
        m_Player = Player.getAudioPlayerFromGuild(guild)
        if (m_Player != null) {
            if (queue.isNotEmpty()) {
                m_QueueDuration = calcQueueDuration()
                m_EmbedPages = createQueueEmbedPages(
                    m_CurrentTrack, queue, hook.interaction.user,
                    m_Pages
                )
                m_Embed = m_EmbedPages!![0]

                // disable the backward button since this will always be on the first page of the embed
                val actionRow = ActionRow.of(BTN_BACKWARD.asDisabled(), BTN_FORWARD.asEnabled())
                hook.sendMessageEmbeds(m_Embed!!).addActionRow(actionRow.components).mapToResult().queue { result ->
                    result.onFailure { error ->
                        LOG.error("Error when sending queue embed!", error)
                    }.onSuccess {
                        m_Message = it
                        if (m_Pages == 0) {
                            // if there is only one page, the forward button must be disabled.
                            LayoutComponent.updateComponent(
                                listOf(actionRow),
                                BTN_FORWARD.id!!,
                                BTN_FORWARD.asDisabled()
                            )
                        }
                        m_Message!!.editMessageComponents(listOf(actionRow)).queue()
                        isActive = true
                    }
                }
            } else {
                val builder = EmbedBuilder()
                builder.setColor(Color.CYAN)
                builder.setDescription("Queue is currently empty.")
                builder.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))
                m_EmbedPages = null
                m_QueueDuration = 0L
                m_Embed = builder.build()
                m_Message = hook.sendMessageEmbeds(m_Embed!!).complete()
            }
        } else {
            val builder = EmbedBuilder()
            builder.setColor(Color.CYAN)
            builder.setDescription("Currently not playing anything.")
            builder.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))
            m_EmbedPages = null
            m_QueueDuration = 0L
            m_Embed = builder.build()
            m_Message = hook.sendMessageEmbeds(m_Embed!!).complete()
        }
    }

    /**
     * Returns the next ten tracks from the queue.
     *
     * @param tracks the current tracks in the queue. (never `null`)
     * @param page   the page of the queue embed.
     * @return a list with the next ten tracks. If there are no ten tracks remaining, it will return only them.
     */
    private fun nextTenTracks(@Nonnull tracks: List<AudioTrack>, page: Int): List<AudioTrack> {
        val ret = ArrayList<AudioTrack>(10)
        for (i in (10 * page) until (10 * page + 10)) {
            if (i < tracks.size) {
                ret.add(tracks[i])
            } else {
                return ret
            }
        }
        return ret
    }

    /**
     * Creates all pages of the queue embed.
     *
     * @param currentTrack the current playing track. (never `null`)
     * @param queue        the queue. (never `null`)
     * @param user         the user that send the command. (never `null`)
     * @param pages        the amount of pages the queue embed has.
     * @return a list with all pages of the queue embed
     */
    private fun createQueueEmbedPages(
        @Nonnull currentTrack: AudioTrack,
        @Nonnull queue: List<AudioTrack>, @Nonnull user: User, pages: Int,
    ): List<MessageEmbed> {
        Objects.requireNonNull(currentTrack, "currentTrack must not be null")
        Objects.requireNonNull(queue, "queue must not be null")
        Objects.requireNonNull(user, "user must not be null")
        val embeds = ArrayList<MessageEmbed>(pages)
        for (page in 0 until pages) {
            embeds.add(createQueueEmbed(currentTrack, queue, user, page, pages, queue.size))
        }
        return embeds
    }

    /**
     * Creates the [MessageEmbed] for the queue.
     *
     * @param currentTrack The current playing track. (never `null`)
     * @param queue        The tracks of the current page. (never `null`)
     * @param user         User that asked for the queue. (never `null`)
     * @param page         The page.
     * @param pages        The pages of the embed
     * @param queueLength  The length of the queue
     * @return A [MessageEmbed] for the queue.
     */
    private fun createQueueEmbed(
        @Nonnull currentTrack: AudioTrack, @Nonnull queue: List<AudioTrack>,
        @Nonnull user: User, page: Int, pages: Int, queueLength: Int,
    ): MessageEmbed {
        var pages = pages
        Objects.requireNonNull(currentTrack, "currentTrack must not be null")
        Objects.requireNonNull(user, "user must not be null")
        Objects.requireNonNull(queue, "tracks must not be null!")
        val tracks = nextTenTracks(queue, page)

        val eb = EmbedBuilder()
        eb.setColor(Color.CYAN)
        eb.setTitle("Queue for " + m_Guild.name)
        if (page == 0) {
            eb.addField(
                "__Now Playing:__",
                "[" + currentTrack.info.title + "](" + currentTrack.info.uri + ") | `" + formatDuration(
                    currentTrack.position
                ) + " Requested by:` ${(currentTrack.userData as TrackData).user.asMention}",
                false
            )
        }


        val z = page * 10
        for (i in tracks.indices) {
            val audioTrack = tracks[i]
            val fieldValue = StringBuilder()
            fieldValue.append("`")
            fieldValue.append(i + z + 1)
            fieldValue.append(".` [")
            fieldValue.append(audioTrack.info.title)
            fieldValue.append("](")
            fieldValue.append(audioTrack.info.uri)
            fieldValue.append(") | `")
            fieldValue.append(formatDuration(audioTrack.duration))
            fieldValue.append(" Requested by: `")
            fieldValue.append((currentTrack.userData as TrackData).user.asMention)
            fieldValue.append("")
            if (i == 0) {
                eb.addField("__Up next:__", fieldValue.toString(), false)
            } else {
                eb.addField("", fieldValue.toString(), false)
            }
        }
        eb.addField(
            "**" + queueLength + " songs in queue | " + formatDuration(m_QueueDuration) + " total length**", "",
            false
        )
        if (pages == 0) {
            pages = 1
        }
        eb.setFooter("Page " + (page + 1) + "/" + pages, user.avatarUrl)
        eb.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))
        return eb.build()
    }

    /**
     * Formats the duration of the given time in milliseconds.
     *
     * @param duration the time in milliseconds
     * @return the formated time.
     */
    private fun formatDuration(duration: Long): String {
        val durationStr = if (TimeUnit.MILLISECONDS.toHours(duration) > 0) {
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
     * Updates the embed if it's still active.
     *
     * @param direction the direction the embed should be updated. (never `null`)
     */
    fun updateEmbed(@Nonnull direction: QueueEmbedUpdateDirection) {
        if (!isActive) {
            // if the embed is not active, there is nothing to be update.
            return
        }
        when (direction) {
            QueueEmbedUpdateDirection.FORWARD -> if (m_Page <= m_Pages) {
                m_Page++
                val embed = m_EmbedPages!![m_Page]
                m_Message!!.editMessageEmbeds(embed).queue()
            }

            QueueEmbedUpdateDirection.BACKWARD -> if (m_Page > 1) {
                m_Page--
                val embed = m_EmbedPages!![m_Page]
                m_Message!!.editMessageEmbeds(embed).queue()
            }

            else -> {}
        }
        if (m_Page > 0) {
            val components = m_Message!!.actionRows
            LayoutComponent.updateComponent(components, BTN_BACKWARD.id!!, BTN_BACKWARD.asEnabled())
            m_Message!!.editMessageComponents(components).queue()
        } else {
            val components = m_Message!!.actionRows
            LayoutComponent.updateComponent(components, BTN_BACKWARD.id!!, BTN_BACKWARD.asDisabled())
            m_Message!!.editMessageComponents(components).queue()
        }
        if (m_Page < m_Pages) {
            val components = m_Message!!.actionRows
            LayoutComponent.updateComponent(components, BTN_FORWARD.id!!, BTN_FORWARD.asEnabled())
            m_Message!!.editMessageComponents(components).queue()
        } else {
            val components = m_Message!!.actionRows
            LayoutComponent.updateComponent(components, BTN_FORWARD.id!!, BTN_FORWARD.asDisabled())
            m_Message!!.editMessageComponents(components).queue()
        }
    }

    /**
     * Calculates the duration of the queue in milliseconds.
     *
     * @return the duration of the queue in milliseconds.
     */
    private fun calcQueueDuration(): Long {
        var queueDuration = 0L
        for (audioTrack in m_Queue) {
            queueDuration += audioTrack.duration
        }
        return queueDuration
    }

    /**
     * destroys the embed.
     */
    fun destroy() {
        val components = m_Message!!.actionRows
        LayoutComponent.updateComponent(components, BTN_FORWARD.id!!, BTN_FORWARD.asDisabled())
        LayoutComponent.updateComponent(components, BTN_BACKWARD.id!!, BTN_BACKWARD.asDisabled())
        m_Message!!.editMessageComponents(components).queue()
        isActive = false
    }

}