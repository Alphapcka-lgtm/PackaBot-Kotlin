package selection_menus

import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.internal.utils.JDALogger
import utils.RestActionExecutor
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer

object SelectionMenuController {

    /** the logger */
    private val LOG = JDALogger.getLog(SelectionMenuController::class.java)

    private val SELECTION_MENUS = HashMap<String, SelectionMenuExecutionWrapper>()

    private val DURATION_TIME = TimeUnit.MINUTES.toMillis(5L)

    private val m_SelectionMenuExperationCheck: Job = CoroutineScope(Dispatchers.Default).launch {
        val toRemove = ArrayList<String>(SELECTION_MENUS.size)
        for ((id, wrapper) in SELECTION_MENUS) {
            if (wrapper.createdMillis <= System.currentTimeMillis()) {
                wrapper.removeSelectionMenu()
                toRemove.add(id)
            }
        }

        for (id in toRemove) {
            SELECTION_MENUS.remove(id)
            LOG.info("Removed SelectionModel $id")
        }

        delay(DURATION_TIME)
    }

    fun addSelectionMenuExecution(
        customId: String,
        execution: BiConsumer<StringSelectInteractionEvent, InteractionHook>,
        menu: SelectMenu,
        mes: Message,
    ) {
        SELECTION_MENUS[customId] =
            SelectionMenuExecutionWrapper(execution, menu, mes, mes.timeCreated.toInstant().toEpochMilli())
    }

    fun execute(event: StringSelectInteractionEvent) {
        event.deferReply(true).mapToResult().queue { result ->
            result.onSuccess { hook ->
                val id = event.component.id
                if (!SELECTION_MENUS.containsKey(id)) {
                    return@onSuccess RestActionExecutor.queueSendMessageRestActionMapping(
                        hook.sendMessage("Unknown selection menu! This error occurs when the selection menu is too old and should have been deleted from this message or was not deleted/disabled due to shutting down of the bot.")
                            .setEphemeral(true).mapToResult(), LOG
                    )
                }

                val wrapper = SELECTION_MENUS[id]!!
                wrapper.execute(event, hook)
            }.onFailure { error ->
                LOG.error("Error when defer replying to ${StringSelectInteractionEvent::class.java}!", error)
            }
        }
    }

    private class SelectionMenuExecutionWrapper(
        private val execution: BiConsumer<StringSelectInteractionEvent, InteractionHook>,
        private val menu: SelectMenu,
        private val mes: Message,
        val createdMillis: Long,
    ) {
        /**
         * Executes the execution.
         */
        fun execute(event: StringSelectInteractionEvent, hook: InteractionHook) {
            execution.accept(event, hook)
            removeSelectionMenu()
        }

        /**
         * Removes the selection model from the message.
         */
        fun removeSelectionMenu() {
            LayoutComponent.updateComponent(mes.components, menu, null)
        }
    }

}