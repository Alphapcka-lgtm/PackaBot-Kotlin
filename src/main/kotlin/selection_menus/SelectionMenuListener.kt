package selection_menus

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.internal.utils.JDALogger

class SelectionMenuListener : ListenerAdapter() {

    /** the logger */
    private val LOG = JDALogger.getLog(SelectionMenuListener::class.java)

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        LOG.info("StringSelectionInteraction: $event")
        SelectionMenuController.execute(event)
    }
}