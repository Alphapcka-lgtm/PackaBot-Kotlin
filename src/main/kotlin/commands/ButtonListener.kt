package commands

import music.queue_embed.QueueEmbed
import music.queue_embed.QueueEmbedController
import music.queue_embed.QueueEmbedUpdateDirection
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.internal.utils.JDALogger
import utils.ExceptionWebhook
import utils.PythonDiscordColors
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ButtonListener : ListenerAdapter() {

    /** the logger */
    private val LOG = JDALogger.getLog(ButtonListener::class.java)

    override fun onButtonInteraction(event: ButtonInteractionEvent) {

        val button = event.button
        try {
            if (QueueEmbed.ID_BTN_FORWARD == button.id || QueueEmbed.ID_BTN_BACKWARD == button.id) return queueEmbedButtonPress(
                event
            )

            event.reply("Unknown button").mapToResult().queue { result ->
                result.onFailure { error ->
                    LOG.error("Error when sending message!", error)
                }
            }
        } catch (e: Exception) {
            ExceptionWebhook.sendException(e, "Error in ${ButtonListener::class.simpleName}", event.jda)
        } finally {
            if (!event.isAcknowledged) {
                val embed = EmbedBuilder()
                embed.setTitle("${event.member!!.asMention} Error while processing button!")
                embed.setColor(PythonDiscordColors.RED.color)
                embed.setTimestamp(ZonedDateTime.now(ZoneOffset.UTC))
                embed.setFooter("error", event.jda.selfUser.effectiveAvatarUrl)
                event.replyEmbeds(embed.build()).mapToResult().queue { result ->
                    result.onFailure { error ->
                        LOG.error("Error when sending embeds!", error)
                    }
                }
            }
        }
    }

    private fun queueEmbedButtonPress(event: ButtonInteractionEvent) {
        event.deferEdit().queue { hook ->
            val button = event.button

            if (QueueEmbedController.hasActiveQueueEmbed(event.channel.idLong)) {
                val direction = QueueEmbedUpdateDirection.factorButtonIdToEnum(button.id!!)

                if (direction == null) {
                    LOG.error("Queue embed update direction is null for button [label: ${button.label}, id: ${button.id}]]")
                    hook.sendMessage("${event.member!!.asMention} Error while processing button!").mapToResult()
                        .queue { result ->
                            result.onFailure { error ->
                                LOG.error("Error when sending message!", error)
                            }
                        }
                    return@queue
                }

                val opt = QueueEmbedController.updateQueueEmbed(event.channel.idLong, direction)
                val restAction = if (opt.isPresent) {
                    hook.sendMessage("Updated embed.").setEphemeral(true).mapToResult()
                } else {
                    hook.sendMessage("${event.member!!.asMention} No active queue embed to update found!").mapToResult()
                }

                restAction.queue { result ->
                    result.onFailure { error ->
                        LOG.error("Error when sending message!", error)
                    }
                }
            }
        }
    }
}