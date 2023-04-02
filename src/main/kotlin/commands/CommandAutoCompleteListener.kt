package commands

import commands.music.Playlist
import commands.music.Radio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.internal.utils.JDALogger
import utils.ExceptionWebhook

class CommandAutoCompleteListener : ListenerAdapter() {

    /** the logger */
    private val LOG = JDALogger.getLog(CommandAutoCompleteListener::class.java)

    /** the coroutine for executing [CommandAutoCompleteInteractionEvent] */
    private val commandAutoCompleteCoroutine = CoroutineScope(Dispatchers.Default)

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        LOG.info("command auto complete interaction event: $event")
        try {

            commandAutoCompleteCoroutine.launch {

                when (event.name) {
                    Radio.name -> Radio.commandAutoComplete(event)
                    Playlist.name -> Playlist.commandOptionAutoComplete(event)
                }

            }
        } catch (e: Exception) {
            ExceptionWebhook.sendException(e, "Error in ${CommandAutoCompleteListener::class.simpleName}", event.jda)
        }
    }
}