package utils

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.Result
import org.slf4j.Logger

open class RestActionExecutor {

    companion object {

        /**
         * Queues the given rest action. **This method is meant for rest action that are not mapped to a result!**
         *
         * @param action the rest action to queue.
         * @param logger the logger to log the error message to.
         *
         * @see queueRestActionResult
         */
        fun <T> queueRestAction(action: RestAction<T>, logger: Logger) {
            queueRestAction(action, logger, "Error when executing rest action!")
        }

        /**
         * Queues the given rest action. **This method is meant for rest action that are not mapped to a result!**
         *
         * @param action the rest action to queue.
         * @param logger the logger to log the error message to.
         * @param errorMes the message to log on error.
         *
         * @see queueRestActionResult
         */
        fun <T> queueRestAction(action: RestAction<T>, logger: Logger, errorMes: String) {
            queueRestActionResult(action.mapToResult(), logger, errorMes)
        }

        /**
         * Queues the rest action that was mapped to a result. On error, it logs the error with a default message.
         *
         * @param action the action to queue.
         * @param logger the logger to log to.
         */
        fun <T> queueRestActionResult(action: RestAction<Result<T>>, logger: Logger) {
            queueRestActionResult(action, logger, "Error when execution rest action!")
        }

        /**
         * Queues the rest action for sending a message that was mapped to a result. On error, it logs the error with the default message.
         *
         * @param action the message action to queue.
         * @param logger the logger to log to.
         */
        fun queueSendMessageRestActionMapping(action: RestAction<Result<Message>>, logger: Logger) {
            queueRestActionResult(action, logger, "Error when sending message!")
        }

        /**
         * Queues the rest action for **editing** a message that was mapped to a result. On error, it logs the error with the default message.
         *
         * @param action the message action to queue.
         * @param logger the logger to log to.
         */
        fun queueEditMessageRestActionMapping(action: RestAction<Result<Message>>, logger: Logger) {
            queueRestActionResult(action, logger, "Error when editing message!")
        }

        /**
         * Queues the rest action for sending a message **embed** that was mapped to a result. On error, it logs the error with the default message.
         *
         * @param action the message action to queue.
         * @param logger the logger to log to.
         */
        fun queueSendMessageEmbedRestActionMapping(action: RestAction<Result<Message>>, logger: Logger) {
            queueRestActionResult(action, logger, "Error when sending message embed!")
        }

        /**
         * Queues the rest action that was mapped to a result. On error, it logs the error with a default message.
         *
         * @param action the action to queue.
         * @param logger the logger to log to.
         * @param errorMes the error message to log on error.
         */
        fun <T> queueRestActionResult(action: RestAction<Result<T>>, logger: Logger, errorMes: String) {
            action.queue { result ->
                result.onFailure { error ->
                    logger.error(errorMes, error)

                    ExceptionWebhook.sendException(error, errorMes, action.jda)
                }
            }
        }
    }

}