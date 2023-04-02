import database.DatabaseConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.internal.utils.JDALogger
import utils.ExceptionWebhook
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

val LOG = JDALogger.getLog("Starting-Logger")

/**
 * Main function
 * @param args starting arguments
 */
fun main(args: Array<String>) {
    runBlocking {
        LOG.info("Hello World!")

        // Try adding program arguments via Run/Debug configuration.
        // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
        LOG.info("Program arguments: ${args.joinToString()}")

        val databaseConnection = async {
            var databaseConnection: Connection? = null
            for (i in 0..1) {
                try {
                    databaseConnection = createDatabaseConnection()
                    break
                } catch (e: SQLException) {
                    LOG.error("Error while crating database connection!", e)
                    if (i == 0) {
                        LOG.error("Trying to create a database connection a second time.")
                    } else {
                        LOG.warn("Going forward with out database connection!")
                        databaseConnection = null
                        break
                    }
                }
            }
            return@async databaseConnection
        }

        val jdaBuilder = setupJda()


        try {
            val packaBot = PackaBot(jdaBuilder, databaseConnection.await())
        } catch (e: Exception) {
            ExceptionWebhook.sendException(e, "Exception in ${this.javaClass.simpleName}", "850745928133771264")
            e.printStackTrace()
        }
    }

}

/**
 * Sets up the jda connection.
 * @param consumer consumer of the jda builder to make custom changes.
 * @return the created jda instance
 */
fun setupJda(): JDABuilder {
    val stream = object {}.javaClass.getResource("config.properties").openStream()
    val properties = Properties()
    properties.load(stream)

//    val builder = JDABuilder.create(properties.getProperty("token"), EnumSet.allOf(GatewayIntent::class.java))
    val builder = JDABuilder.create(properties.getProperty("token"), gatewayIntentions())

    builder.setActivity(Activity.of(Activity.ActivityType.COMPETING, "Initializing"))
    builder.setStatus(OnlineStatus.DO_NOT_DISTURB)

    return builder
}

/**
 * The gateway intentions for the bot.
 */
private fun gatewayIntentions(): Collection<GatewayIntent> {

    return listOf(
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.GUILD_VOICE_STATES,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.MESSAGE_CONTENT,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
    )
}

/**
 * Creates the database connection.
 * @return the database connection.
 * @throws SQLException if the database connection could not be created.
 */
fun createDatabaseConnection(): Connection {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver")
        val databaseConfig = DatabaseConfig.loadFromResource("database_conf.properties")
        return DriverManager.getConnection(
            databaseConfig.databaseUrl,
            databaseConfig.databaseUser,
            databaseConfig.databasePassword
        )
    } catch (e: Exception) {
        throw SQLException(e)
    }
}
