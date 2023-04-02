package utils

import club.minnced.discord.webhook.WebhookClient
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.internal.utils.JDALogger
import java.io.PrintWriter
import java.io.StringWriter

object ExceptionWebhook {

    /** the logger */
    private val LOG = JDALogger.getLog(ExceptionWebhook::class.java)

    private const val EXCEPTION_WEBHOOK_URL =
        "https://discord.com/api/webhooks/1050439027540635769/KepfKz12Y0nQpjWR3TwGafbB_-zBuM2Nof7feG1uPF3bHTWXJlFCUJpEOOoAbppaDY1B"

    private val webhook: WebhookClient = WebhookClient.withUrl(EXCEPTION_WEBHOOK_URL)

    /**
     * To send an exception message to the exception web hook.
     *
     * @param exception the exception that occurred
     * @param botUserId the user id of the bot in which the exception occurred.
     */
    fun sendException(exception: Throwable, botUserId: String) {
        val stringWriter = StringWriter()
        val pw = PrintWriter(stringWriter)
        exception.printStackTrace(pw)
        val stackTrace = stringWriter.toString()

        val strBuilder = StringBuilder()
        strBuilder.appendLine("Exception in <@$botUserId>!")
        strBuilder.appendLine("```")
        strBuilder.appendLine(stackTrace)
        strBuilder.appendLine("```")

        webhook.send(strBuilder.toString()).exceptionallyAsync { exception ->
            run {
                LOG.error("Error when sending webhook message!", exception)
                return@run null
            }
        }
    }

    /**
     * To send an exception message to the exception web hook.
     *
     * @param exception the exception that occurred
     * @param botUserId the user id of the bot in which the exception occurred.
     */
    fun sendException(exception: Throwable, errMes: String, botUserId: String) {
        val stringWriter = StringWriter()
        val pw = PrintWriter(stringWriter)
        exception.printStackTrace(pw)
        val stackTrace = stringWriter.toString()

        val strBuilder = StringBuilder()
        strBuilder.appendLine("Exception in <@$botUserId>!")
        strBuilder.appendLine("_**$errMes**_")
        strBuilder.appendLine("```")
        strBuilder.appendLine(stackTrace)
        strBuilder.appendLine("```")

        webhook.send(strBuilder.toString()).exceptionallyAsync { exception ->
            run {
                LOG.error("Error when sending webhook message!", exception)
                return@run null
            }
        }
    }

    /**
     * To send an exception message to the exception web hook.
     *
     * @param exception the exception that occurred
     * @param jda the jda instance of the running bot.
     */
    fun sendException(exception: Throwable, jda: JDA) {
        val stringWriter = StringWriter()
        val pw = PrintWriter(stringWriter)
        exception.printStackTrace(pw)
        val stackTrace = stringWriter.toString()

        val strBuilder = StringBuilder()
        strBuilder.appendLine("Exception in ${jda.selfUser.asMention}!")
        strBuilder.appendLine("```")
        strBuilder.appendLine(stackTrace)
        strBuilder.appendLine("```")

        webhook.send(strBuilder.toString()).exceptionallyAsync { exception ->
            run {
                LOG.error("Error when sending webhook message!", exception)
                return@run null
            }
        }
    }

    /**
     * To send an exception message to the exception web hook.
     *
     * @param exception the exception that occurred
     * @param errMes the error message.
     * @param jda the jda instance of the running bot.
     */
    fun sendException(exception: Throwable, errMes: String, jda: JDA) {
        val stringWriter = StringWriter()
        val pw = PrintWriter(stringWriter)
        exception.printStackTrace(pw)
        val stackTrace = stringWriter.toString()

        val strBuilder = StringBuilder()
        strBuilder.appendLine("Exception in ${jda.selfUser.asMention}!")
        strBuilder.appendLine("_**$errMes**_")
        strBuilder.appendLine("```")
        strBuilder.appendLine(stackTrace)
        strBuilder.appendLine("```")

        webhook.send(strBuilder.toString()).exceptionallyAsync { exception ->
            run {
                LOG.error("Error when sending webhook message!", exception)
                return@run null
            }
        }
    }

}