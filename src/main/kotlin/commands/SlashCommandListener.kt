package commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.Result
import net.dv8tion.jda.internal.utils.JDALogger
import utils.RestActionExecutor

class SlashCommandListener : ListenerAdapter() {

    /** the logger */
    private val LOG = JDALogger.getLog(SlashCommandListener::class.java)

    /** the coroutine for executing [SlashCommandInteractionEvent] */
    private val slashCommandCoroutine = CoroutineScope(Dispatchers.Default)

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        LOG.info("slash command interaction event: $event")
        slashCommandCoroutine.launch {

            var replyMapAction: RestAction<Result<InteractionHook>>? = null
            try {
                if (!SlashCommandController.checkAndRunSlashCommand(event)) {
                    replyMapAction = event.reply("Error when executing command: Command not found!").mapToResult()
                }
            } catch (e: IllegalStateException) {
                replyMapAction = event.reply("Error when executing command: ${e.message}").mapToResult()
            }

            if (replyMapAction != null) {
                RestActionExecutor.queueRestActionResult(replyMapAction, LOG)
            }

//            replyMapAction.queue { result ->
//                result.onFailure { error ->
//                    LOG.error("Error when responding to slash command interaction event!", error)
//                }
//            }
        }
    }

}