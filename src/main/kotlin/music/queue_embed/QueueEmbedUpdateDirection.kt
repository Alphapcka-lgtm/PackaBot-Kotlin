package music.queue_embed

import javax.annotation.Nonnull

/**
 * Enum to indicate in which direction the embed of the queue should be updated.
 *
 * @author Michael
 * @see QueueEmbedUpdateDirection#FORWARD
 * @see QueueEmbedUpdateDirection#BACKWARD
 */
enum class QueueEmbedUpdateDirection {


    /**
     * For advancing the embed by one step forward<br>
     * e.g.: Going form page 2 to page 1
     */
    FORWARD,

    /**
     * For advancing the embed by one step backward<br>
     * e.g.: Going form page 1 to page 2
     */
    BACKWARD;

    companion object {
        /**
         * Factors the id of the button to a value of the enum [QueueEmbedUpdateDirection].
         *
         * @param buttonId the id of the button, pressed to update the embed. The id needs to be [QueueEmbed.ID_BTN_FORWARD] to get the forward direction and [QueueEmbed.ID_BTN_BACKWARD] to get the backward direction
         * @return a value of [QueueEmbedUpdateDirection] or `null` if no fitting value was found.
         */
        fun factorButtonIdToEnum(@Nonnull buttonId: String): QueueEmbedUpdateDirection? {
            return when (buttonId) {
                QueueEmbed.ID_BTN_FORWARD -> FORWARD
                QueueEmbed.ID_BTN_BACKWARD -> BACKWARD
                else -> null
            }
        }
    }

}