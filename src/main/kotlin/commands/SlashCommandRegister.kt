package commands

import PackaBot
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.utils.Result
import net.dv8tion.jda.internal.utils.JDALogger
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import javax.annotation.Nonnull
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * Registers all classes with the [MessageCommand] annotation and [AbstractSlashCommandData] as super class
 * as slash commands. <br></br>
 * When you specify a path to an slash command update script (xml-file) via [.setPathUpdateScript] it will
 * attempt to load the file during the method [.loadCommandsWithDataMap]
 *
 * @author Michael
 * @since 0.1.2
 */
class SlashCommandRegister(private val m_Bot: PackaBot) {

    /**
     * the logger
     */
    private val LOG = JDALogger.getLog(SlashCommandRegister::class.java)

    /**
     * contains all slash commands with their id as key
     */
    private val m_Commands: HashMap<String, PackaBotSlashCommand> = HashMap<String, PackaBotSlashCommand>()

    /**
     * contains all slash command data classes with the name as key and the object as value
     */
    private val m_UnaddedDataMap: HashMap<String, ExecutableSlashCommand> =
        HashMap<String, ExecutableSlashCommand>()

    /**
     * contains all slash command data classes with the name as key and the object as value
     */
    private val m_CommandData: HashMap<String, ExecutableSlashCommand> = HashMap<String, ExecutableSlashCommand>()
    /**
     * The path to the slash command update script.
     *
     * @return the path of the update script. (can be `null`)
     */
    /**
     * the update script
     */
    var pathUpdateScript: File? = null
        private set
    /**
     * Indicates if the commands map has been loaded.
     *
     * @return `true` when the commands map has been loaded.
     */
    /**
     * Indicates if the commands have been loaded
     */
    var isLoaded = false
        private set

    /**
     * Loads all the slash command data classes and returns them in a map with the command name as key and the
     * class-object as value.
     *
     * @return A map with the command name as key and the class-object as value.
     */
    val slashCommandData: MutableMap<String, ExecutableSlashCommand>
        get() {
            val configuration = ConfigurationBuilder()
            configuration.addScanners(Scanners.TypesAnnotated)
            configuration.forPackages("commands")
            val reflections = Reflections(configuration)
            val clazzez: Set<Class<*>> = reflections.getTypesAnnotatedWith(SlashCommandAnnotation::class.java)
            for (clazz in clazzez) {
                if (clazz.interfaces.contains(ExecutableSlashCommand::class.java)) {
                    val superClass: Class<out ExecutableSlashCommand?> = clazz.asSubclass(
                        ExecutableSlashCommand::class.java
                    )

                    // gets the object instance of the slash command instance
                    val cmd = Class.forName(superClass.name).kotlin.objectInstance as ExecutableSlashCommand
                    m_CommandData[cmd.name] = cmd

                } else {
                    LOG.warn(
                        "Class [$clazz] annotated with [${SlashCommandAnnotation::class.java}] does not have class [${ExecutableSlashCommand::class.java}] as super class!"
                    )
                }
            }
            // cmd.editCommand().apply(null).queue();
            return m_CommandData
        }

    /**
     * Retrieves all commands from the discord api and creates [PackaBotSlashCommand] objects with slash command
     * data from the data map. Returns a map with the command id as keys and the [PackaBotSlashCommand] as
     * values.<br></br>
     * **Requires an active PackaBot instance!**
     *
     * @param dataMap The map with the slash command name as key and the slash command data as value. (never
     * `null`)
     * @return a map with all commands that are not added. The map will be empty if all commands are added or
     * `null` if an error occurred.
     * @throws Exception While loading the commands.
     */
    @Throws(Exception::class)
    fun loadCommandsWithDataMap(
        @Nonnull dataMap: MutableMap<String, ExecutableSlashCommand>,
    ): HashMap<String, ExecutableSlashCommand> {

        Objects.requireNonNull<Map<String, ExecutableSlashCommand>>(dataMap, "dataMap must not be null!")

        val jda: JDA = m_Bot.m_Jda
        val updateScript = UpdateScript(pathUpdateScript)
        val updateMap: Map<String, ExecutableSlashCommand?> = updateScript.updatedCommands
        val deletedCommands = updateScript.deletedCommands

        // All normal commands are added on/to the global jda scope.
        for (_command in jda.retrieveCommands().complete()) {
            var command = _command

            val commandName = command.name
            if (updateMap.containsKey(commandName)) {
                val data: ExecutableSlashCommand = updateMap[commandName]!!
                command.editCommand().apply(data.slashCommandData).queue {
                    command = it
                    val commandId = command.id
                    m_Commands[commandId] = PackaBotSlashCommand(data, command, commandId)
                    dataMap.remove(commandName)
                    LOG.info("Updated command ${_command.name} [$_command] with ${command.name} [$command]")
                }
            } else if (deletedCommands.contains(commandName)) {
                command.delete().queue { LOG.info("Deleted command ${command.name} [$command]") }
            } else if (dataMap.containsKey(commandName)) {
                val data: ExecutableSlashCommand = dataMap[commandName]!!
                val commandId = command.id
                m_Commands[commandId] = PackaBotSlashCommand(data, command, commandId)
                dataMap.remove(commandName)
                LOG.info("Loaded command $command")
            } else {
                LOG.warn("Unknown command data for command: $command")
            }
        }

        for (data in dataMap.values) {
            LOG.warn("Unable to find commands for command data [$data]")
        }
        isLoaded = true
        m_UnaddedDataMap.putAll(dataMap)
        return m_UnaddedDataMap
    }

    /**
     * Adds all commands to the discord api and creates [PackaBotSlashCommand] objects with slash command data
     * from the data map. Returns a map with the command id as keys and the [PackaBotSlashCommand] as values.<br></br>
     * **Requires an active PackaBot instance!**
     *
     * @param dataMap The map with the slash command name as key and the slash command data as value. (never
     * `null`)
     * @return a map with all commands that are not added. The map will be empty if all commands are added or
     * `null` if an error occurred.
     */
    fun addCommandsWithDataMap(
        @Nonnull dataMap: Map<String, ExecutableSlashCommand>,
    ): HashMap<String, ExecutableSlashCommand> {
        val jda: JDA = m_Bot.m_Jda

        val completionList = ArrayList<Future<Result<Command>>>(dataMap.values.size)

        for (data in dataMap.values) {
            LOG.info("Adding command [$data] to global.")

            val future = jda.upsertCommand(data.slashCommandData).mapToResult().onSuccess { result ->
                result.onSuccess { command ->
                    val commandId = command.id
                    val com = PackaBotSlashCommand(data, command, commandId)
                    m_Commands[commandId] = com
                    m_UnaddedDataMap.remove(data.slashCommandData.name)
                }.onFailure { error ->
                    LOG.warn("Error when adding command [$data] to global!", error)
                    m_UnaddedDataMap[data.slashCommandData.name] = data
                }
            }.submit()

            completionList.add(future)
        }

        LOG.info("Waiting for JDA-Response of the adding command action.")
        // Creates a completable future of all jda upserts and waits until all of them are completed.
        CompletableFuture.allOf(*completionList.toArray(arrayOfNulls<CompletableFuture<*>>(0))).get()

        isLoaded = true
        return m_UnaddedDataMap
    }

    /**
     * Adds the command id as key and the [PackaBotSlashCommand] as value to the command map. If the commands are added, it can check for their executions.
     *
     * @param commandId the id of the command. (never `null`)
     * @param cmd       the command. (never `null`)
     */
    fun addCommand(@Nonnull commandId: String, @Nonnull cmd: PackaBotSlashCommand) {
        m_Commands[commandId] = cmd
    }

    /**
     * @return A Map with the command id as key and the [PackaBotSlashCommand] as value or an empty map if the
     * commands where not loaded yet or none are added. (never `null`)
     */
    val commands: HashMap<String, PackaBotSlashCommand>
        get() = m_Commands

    /**
     * @return `true` if not all available slash command classes are added as commands to the api.
     */
    fun hasUnaddedCommandData(): Boolean {
        return !m_UnaddedDataMap.isEmpty()
    }

    /**
     * Sets the path to the slash command update script. If the path is `null`, the update script will not be
     * loaded. (default: `null`)
     *
     * @param path the path of the update script. (can be `null`)
     */
    fun setPathUpdateScript(path: String?) {
        if (path != null) {
            pathUpdateScript = File(path)
        }
    }

    /**
     * Sets the path to the slash command update script. If the path is `null`, the update script will not be
     * loaded. (default: `null`)
     *
     * @param path the path of the update script. (can be `null`)
     */
    fun setPathUpdateScript(path: File?) {
        pathUpdateScript = path
    }

    /**
     * Inner class to represent the update xml
     *
     * @author Michael
     */
    inner class UpdateScript {
        /**
         * Map with the updated commands: command name as key and the [AbstractSlashCommandData] object as value
         */
        private val m_UpdatedCommands: HashMap<String, ExecutableSlashCommand?> =
            HashMap<String, ExecutableSlashCommand?>()

        /**
         * List with the names of the commands to be deleted.
         */
        private val m_DeletedCommands = LinkedList<String>()

        /**
         * Constructor
         *
         * @param path The path to the update xml. Can be `null` if no update script was provided.
         * @throws IOException                  on error when loading the update xml
         * @throws SAXException                 on error when loading the update xml
         * @throws ParserConfigurationException on error when loading the update xml
         * @throws IllegalStateException        on error when loading the update xml
         */
        constructor(path: String?) {
            if (path == null) {
                loadXml(null)
            } else {
                loadXml(FileInputStream(path))
            }
        }

        /**
         * Constructor
         *
         * @param path The path to the update xml. Can be `null` if no update script was provided.
         * @throws IOException                  on error when loading the update xml
         * @throws SAXException                 on error when loading the update xml
         * @throws ParserConfigurationException on error when loading the update xml
         * @throws IllegalStateException        on error when loading the update xml
         */
        constructor(path: File?) {
            var stream: InputStream? = null
            if (path == null) {
                try {
                    stream = this.javaClass.classLoader.getResourceAsStream(DEFAULT_UPDATE_SCRIPT)
                } catch (e: URISyntaxException) {
                    LOG.warn("Unable to get update.xml from resources.", e)
                }
            } else {
                stream = FileInputStream(path)
            }
            loadXml(stream)
        }

        /**
         * Loads the update xml
         *
         * @param stream the stream to update xml. Can be `null` if no file was given.
         * @throws ParserConfigurationException on error when loading the update xml
         * @throws SAXException                 on error when loading the update xml
         * @throws IOException                  on error when loading the update xml
         * @throws IllegalStateException        on error when loading the update xml
         */
        @Throws(
            ParserConfigurationException::class,
            SAXException::class,
            IOException::class,
            IllegalStateException::class
        )
        private fun loadXml(stream: InputStream?) {
            if (stream == null) {
                m_UpdatedCommands.clear()
                m_DeletedCommands.clear()
            } else {
                val commandDataMap: HashMap<String, ExecutableSlashCommand> = m_CommandData
                val factory = DocumentBuilderFactory.newInstance()
//                factory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                val documentBuilder = factory.newDocumentBuilder()
                val doc = documentBuilder.parse(stream)
                doc.documentElement.normalize()
                val updateList = doc.getElementsByTagName("update")
                for (i in 0 until updateList.length) {
                    val updateNode = updateList.item(i)
                    if (updateNode.nodeType == Node.ELEMENT_NODE) {
                        val element = updateNode as Element
                        val childs = element.getElementsByTagName("command")
                        for (x in 0 until childs.length) {
                            val childNode = childs.item(x)
                            if (childNode.nodeType == Node.ELEMENT_NODE) {
                                val childElement = childNode as Element
                                val name = childElement.getAttribute("name")
                                val hasNewName =
                                    java.lang.Boolean.parseBoolean(childElement.getAttribute("has_new_name"))
                                if (hasNewName) {
                                    // element.textContent does not return the string "nowplaying" but something like "\n\nnowplaying\n\n"!
                                    // because of this, you need to use the child element as a node to get the correct string
                                    val newName = childNode.textContent
                                    if (commandDataMap.containsKey(newName)) {
                                        m_UpdatedCommands[name] = commandDataMap[newName]
                                    } else {
                                        throw IllegalStateException(
                                            "commandDataMap does not contain command data for new name [$newName]"
                                        )
                                    }
                                } else {
                                    if (commandDataMap.containsKey(name)) {
                                        m_UpdatedCommands[name] = commandDataMap[name]
                                    } else {
                                        throw IllegalStateException(
                                            "commandDataMap does not contain command data for name [$name]"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val deleteList = doc.getElementsByTagName("delete")
                for (i in 0 until deleteList.length) {
                    val node = deleteList.item(i)
                    if (node.nodeType == Node.ELEMENT_NODE) {
                        val element = node as Element
                        val childs = element.getElementsByTagName("command")
                        for (x in 0 until childs.length) {
                            val childNode = childs.item(x)
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                val childElement = childNode as Element
                                val name = childElement.getAttribute("name")
                                m_DeletedCommands.add(name)
                            }
                        }
                    }
                }
            }
        }

        /**
         * Gets the map with updated commands: the **name** of the command to update as **key** and the
         * [AbstractSlashCommandData] object to update with as **value**.
         *
         * @return the map with updated commands: the **name** of the command to update as **key** and the
         * [AbstractSlashCommandData] object to update with as **value**.
         */
        val updatedCommands: HashMap<String, ExecutableSlashCommand?>
            get() = m_UpdatedCommands

        /**
         * Gets the names of commands to be deleted from the bot.
         *
         * @return the names of commands to be deleted from the bot.
         */
        val deletedCommands: Collection<String>
            get() = m_DeletedCommands

        /**
         * constant with the default update script in the resource
         */
        private val DEFAULT_UPDATE_SCRIPT = "update.xml"
    }
}