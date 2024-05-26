import club.minnced.discord.webhook.WebhookClient
import commands.ButtonListener
import commands.CommandAutoCompleteListener
import commands.SlashCommandController
import commands.SlashCommandListener
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.internal.utils.JDALogger
import org.slf4j.Logger
import selection_menus.SelectionMenuListener
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.TimeUnit

/**
 * The Packa Bot.
 * @param jdaBuilder the jda builder to build from.
 * @param m_DatabaseConnection the database connection (can be `null`)
 */
class PackaBot(private val jdaBuilder: JDABuilder, m_DatabaseConnection: Connection?) {

    /**
     * The logger.
     */
    private val LOG: Logger = JDALogger.getLog(PackaBot::class.java)

    val m_Jda: JDA = jdaBuilder.build()

    /** coroutine to (re)connect to the database when the connection dies.*/
    private val databaseConnectionCoroutine = CoroutineScope(Dispatchers.Default)

    private val hookUrl =
        "https://discord.com/api/webhooks/1046897082503549028/VarZd0NxD-a4DNKpMNvby1g2FOrduWgDztGLmFDSjVQKJuFB6JedC7nrWIz_Y-p3NSsX"
    private val statusHook = WebhookClient.withUrl(hookUrl)

    var m_DatabaseConnection: Connection? = m_DatabaseConnection
        private set

    init {
        // await ready to always make sure that the jda instance is ready
        m_Jda.awaitReady()
        m_Jda.presence.setPresence(
            OnlineStatus.ONLINE,
            Activity.of(Activity.ActivityType.PLAYING, "Music! maybe...."),
            false
        )

        LOG.info("Connected to guilds [${m_Jda.guilds}]")
        LOG.info("users: ${m_Jda.users}")

        databaseConnectionCoroutine.launch { databaseConnect() }

        SlashCommandController.databaseConnection = m_DatabaseConnection
        SlashCommandController.registerSlashCommands(this, null)

        statusHook.send("${m_Jda.selfUser.asMention} is online.")

        addEventListener()

        if ((m_DatabaseConnection == null) || m_DatabaseConnection.isClosed) {
            statusHook.send("Bot has no database.").whenCompleteAsync { _, error ->
                if (error != null) {
                    LOG.error("Error when sending Webhook message!", error)
                }
            }
        }

    }

    /**
     * Adds the event listeners to the jda.
     */
    private fun addEventListener() {
        m_Jda.addEventListener(SlashCommandListener())
        m_Jda.addEventListener(CommandAutoCompleteListener())
        m_Jda.addEventListener(ButtonListener())
        m_Jda.addEventListener(SelectionMenuListener())

        m_Jda.addEventListener(object : ListenerAdapter() {
            override fun onShutdown(event: ShutdownEvent) {
                databaseConnectionCoroutine.cancel()
                m_DatabaseConnection?.close()
            }
        })
    }

    /**
     * Checks if the database is connected. If the database is not connected, it tries to connect to it.
     */
    private suspend fun databaseConnect() {
        val delayMin = 5L

        while (m_DatabaseConnection == null || m_DatabaseConnection!!.isClosed) {
            try {
                if (m_DatabaseConnection?.isClosed == true) {
                    m_DatabaseConnection = null
                }
                m_DatabaseConnection = createDatabaseConnection()
                if (m_DatabaseConnection != null) {
                    LOG.info("Connected to the database.")
                } else {
                    LOG.info("Database config is not given, unable to connect.")
                }
            } catch (e: SQLException) {
                LOG.warn("Unable to connect to database! Trying to connect again in $delayMin min!", e)
            }

            delay(TimeUnit.MINUTES.toMillis(delayMin))
        }
        delay(TimeUnit.MINUTES.toMillis(delayMin))
    }

    /**
     * Shuts down the packa bot
     */
    fun shutdown() {
        m_Jda.shutdown()
        databaseConnectionCoroutine.cancel()
        m_DatabaseConnection?.close()
    }

}