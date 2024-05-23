import database.DatabaseConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.internal.utils.JDALogger
import utils.ExceptionWebhook
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

val LOG = JDALogger.getLog("Starting-Logger")
lateinit var PROPERTIES: Properties
lateinit var DATABASE_CONFIG: DatabaseConfig
private val PROPERTIES_FILE_NAME = "config.properties"
private val DB_CONFIG_FILE_NAME = "database_conf.properties"

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

        if (!loadProperties()) return@runBlocking

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
 * @return the created jda instance
 */
fun setupJda(): JDABuilder {
//    val builder = JDABuilder.create(properties.getProperty("token"), EnumSet.allOf(GatewayIntent::class.java))
    val builder = JDABuilder.create(PROPERTIES.getProperty("token"), gatewayIntentions())

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

private fun loadProperties(): Boolean {
    val configFile = File(PROPERTIES_FILE_NAME)
    val dbConfigFile = File(DB_CONFIG_FILE_NAME)
    var filesExists = true
    if (!configFile.exists()) {
        filesExists = false
        LOG.warn("Config file $configFile was not found!")
        try {
            createConfigFile(configFile, "config.properties.blueprint.txt")
        } catch (e: Exception) {
            LOG.error("Unable to create config file $configFile!", e)
            return false
        }
        LOG.info("Created config file '$configFile' from blueprint!")
        println("Please configure the config file $configFile!")
    }
    if (!dbConfigFile.exists()) {
        filesExists = false
        LOG.warn("Config file for database $dbConfigFile was not found!")
        try {
            createConfigFile(dbConfigFile, "database_conf.properties.blueprint.txt")
        } catch (e: Exception) {
            LOG.error("Unable to create config file $dbConfigFile!", e)
            return false
        }
        LOG.info("Created db config file '$dbConfigFile' from blueprint!")
        println("Please configure the database config file $dbConfigFile!")
    }

    if (!filesExists) {
        return false
    }

    try {
        PROPERTIES = Properties()
        PROPERTIES.load(FileReader(PROPERTIES_FILE_NAME))

        val dbProperties = Properties()
        dbProperties.load(FileReader(DB_CONFIG_FILE_NAME))
        DATABASE_CONFIG = DatabaseConfig.loadFromProperties(dbProperties)
    } catch (e: Exception) {
        LOG.error("Error when loading config files!", e)
        return false
    }

    return true
}

private fun createConfigFile(file: File, bpRessourceFile: String) {
    if (!file.createNewFile()) {
        throw IOException("Unable to create file: " + file.path)
    }

    val input = ClassLoader.getSystemClassLoader().getResourceAsStream("data/$bpRessourceFile")
    val output = FileWriter(file)
    var c = input.read()
    while (c != -1) {
        output.write(c)
        c = input.read()
    }
    input.close()
    output.flush()
    output.close()
}

/**
 * Creates the database connection.
 * @return the database connection.
 * @throws SQLException if the database connection could not be created.
 */
fun createDatabaseConnection(): Connection {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver")
        return DriverManager.getConnection(
            DATABASE_CONFIG.databaseUrl,
            DATABASE_CONFIG.databaseUser,
            DATABASE_CONFIG.databasePassword
        )
    } catch (e: Exception) {
        throw SQLException(e)
    }
}
