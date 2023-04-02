package commands.voice

import commands.ExecutableSlashCommand
import commands.SlashCommandAnnotation
import kotlinx.coroutines.*
import music.Player
import net.dv8tion.jda.api.audio.hooks.ConnectionListener
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.managers.AudioManager
import net.dv8tion.jda.internal.utils.JDALogger
import utils.RestActionExecutor

@SlashCommandAnnotation
object JoinVoice : ExecutableSlashCommand, RestActionExecutor() {

    /** the logger */
    override val LOG = JDALogger.getLog(JoinVoice::class.java)
    override val name = "join"
    override val slashCommandData: SlashCommandData = createJoinVoiceCommand()

    /**
     * Creates the join voice command data.
     * @return the created slash command data.
     */
    private fun createJoinVoiceCommand(): SlashCommandData {
        val commandData = Commands.slash(name, "Joins your voice channel.")
        commandData.isGuildOnly = true

        return commandData
    }

    override fun execute(interaction: SlashCommandInteraction, hook: InteractionHook) {
        executeWithoutSlashCommand(interaction.guild!!, interaction.messageChannel, hook)
    }

    /**
     * Method to execute the join command without a [SlashCommandInteraction].
     * **Only call this method if you don't have a [SlashCommandInteraction] and it is absolutely necessary!**
     *
     * @param guild the guild to join to.
     * @param boundChannel the message channel to bound the player to.
     * @param hook the interaction hook.
     */
    fun executeWithoutSlashCommand(guild: Guild, boundChannel: MessageChannel, hook: InteractionHook) {
        val player = Player.getAudioPlayerFromGuild(guild)

        val _interaction = hook.interaction

        // interaction.member is never null, because command is guild only
        if (!_interaction.member!!.voiceState!!.inAudioChannel()) {
            return userNotConnectedToAudioChannelMessage(hook)
        }

        val channel = _interaction.member!!.voiceState!!.channel!!.asVoiceChannel()
        val audioManager = guild.audioManager

        if (player == null) {
            if (channelConnect(channel, audioManager, hook)) {
                Player.createPlayer(audioManager, channel, boundChannel)
            }
        } else {
            if (channelConnect(channel, audioManager, hook)) {
                player.channel = channel
            }
        }
    }

    /**
     * Connects the audio manager of the guild to the given channel and sends a message to the interaction hook after he connected.
     * If the manager is already connected to the given channel, it will **not** send a connected message!
     *
     * @param channel the channel to connect to.
     * @param audioManager the audio manager of the guild to connect to the audio manager to.
     * @param hook the interaction hook to which the message should be sent.
     *
     * @return `true` after connecting to the channel or when the audio manager is already connected to the channel. Otherwise, returns `false`.
     */
    fun channelConnect(channel: AudioChannel, audioManager: AudioManager, hook: InteractionHook): Boolean {

        return runBlocking(Dispatchers.IO) {

            if (channel == audioManager.connectedChannel) {
                return@runBlocking true
            }

            return@runBlocking try {
                audioManager.isAutoReconnect = true
                audioManager.openAudioConnection(channel)
                val mes = hook.sendMessage("**Joining `${channel.name}`...")
                    .mapToResult()
                    .submit()

                mes.whenComplete { result, error ->
                    if (error != null) {
                        LOG.error("Completion error of message submit!", error)
                        return@whenComplete
                    }

                    result.onFailure { err -> LOG.error("Error when sending join message!", err) }
                }

                val job = checkConnectionStatusAsync(hook)

                when (val status = job.await()) {
                    ConnectionStatus.SHUTTING_DOWN -> LOG.error("Can not join! JDA is shutting down!")
                    ConnectionStatus.CONNECTED -> {
                        mes.get().onSuccess { message ->
                            message.editMessage("**Joined `${channel.name}` and bound to `${hook.interaction.guildChannel.name}`**")
                                .mapToResult().queue { result ->
                                    result.onFailure { error ->
                                        LOG.error(
                                            "Error when sending updated join message!",
                                            error
                                        )
                                    }
                                }
                        }
                    }

                    ConnectionStatus.ERROR_LOST_CONNECTION -> {
                        LOG.warn("Lost connection to channel!")
                        mes.get().onSuccess { voiceChannelJoiningFailedMessage(it, status) }
                        return@runBlocking false
                    }

                    ConnectionStatus.ERROR_CANNOT_RESUME -> {
                        LOG.warn("WebSocket was unable to resume an active channel session!")
                        mes.get().onSuccess { voiceChannelJoiningFailedMessage(it, status) }
                        return@runBlocking false
                    }

                    ConnectionStatus.ERROR_WEBSOCKET_UNABLE_TO_CONNECT -> {
                        LOG.warn("Websocket is unable to connect to channel!")
                        mes.get().onSuccess { voiceChannelJoiningFailedMessage(it, status) }
                        return@runBlocking false
                    }

                    ConnectionStatus.ERROR_UNSUPPORTED_ENCRYPTION_MODES -> {
                        LOG.warn("Unsupported encryption modes!")
                        mes.get().onSuccess { voiceChannelJoiningFailedMessage(it, status) }
                        return@runBlocking false
                    }

                    ConnectionStatus.ERROR_UDP_UNABLE_TO_CONNECT -> {
                        LOG.warn("Failed UDP setup to connect to channel!")
                        mes.get().onSuccess { voiceChannelJoiningFailedMessage(it, status) }
                        return@runBlocking false
                    }

                    ConnectionStatus.ERROR_CONNECTION_TIMEOUT -> {
                        LOG.error("Channel connection timeout!")
                        mes.get().onSuccess { voiceChannelJoiningFailedMessage(it, status) }
                        return@runBlocking false
                    }

                    else -> {
                        LOG.warn("Unexpected connection status! $status")
                    }
                }



                hook.interaction.guild!!.audioManager.connectionListener = null


                true
            } catch (e: InsufficientPermissionException) {
                queueRestActionResult(
                    hook.sendMessage("Unable to connect to the channel `${channel.name}`: ${e.message}").mapToResult(),
                    LOG
                )

                false
            }
        }
    }

    /**
     * Checks asynchronous to connection status and returns the connection status, when once the bot is connected or an error state occurred while connecting.
     *
     * @param hook the interaction hook.
     * @return the deferred connection status.
     */
    private suspend fun checkConnectionStatusAsync(hook: InteractionHook): Deferred<ConnectionStatus?> {
        return runBlocking(Dispatchers.IO) {
            return@runBlocking async {

                if (hook.interaction.guild!!.audioManager.connectionStatus == ConnectionStatus.CONNECTED) {
                    return@async ConnectionStatus.CONNECTED
                }

                var bool = true

                var ret: ConnectionStatus? = null

                // coroutine to make sure that the program doesn't end up in an endless loop
                val timeLoopTrigger = launch {
                    delay(hook.interaction.guild!!.audioManager.connectTimeout)
                    bool = false
                    ret = ConnectionStatus.ERROR_CONNECTION_TIMEOUT
                }

                while (bool) {
                    hook.interaction.guild!!.audioManager.connectionListener = object : ConnectionListener {

                        val LOG = JDALogger.getLog(this::class.java)

                        override fun onPing(ping: Long) {
                        }

                        override fun onStatusChange(status: ConnectionStatus) {

                            ret = status

                            when (status) {
                                ConnectionStatus.NOT_CONNECTED -> {}
                                ConnectionStatus.SHUTTING_DOWN -> {
                                    LOG.error("Can not join! JDA is shutting down!")
                                    bool = false
                                }

                                ConnectionStatus.CONNECTING_AWAITING_ENDPOINT -> {}
                                ConnectionStatus.CONNECTING_AWAITING_WEBSOCKET_CONNECT -> {}
                                ConnectionStatus.CONNECTING_AWAITING_AUTHENTICATION -> {}
                                ConnectionStatus.CONNECTING_ATTEMPTING_UDP_DISCOVERY -> {}
                                ConnectionStatus.CONNECTING_AWAITING_READY -> {}
                                ConnectionStatus.CONNECTED -> {
                                    bool = false
                                }

                                ConnectionStatus.DISCONNECTED_LOST_PERMISSION,
                                ConnectionStatus.DISCONNECTED_CHANNEL_DELETED,
                                ConnectionStatus.DISCONNECTED_REMOVED_FROM_GUILD,
                                ConnectionStatus.DISCONNECTED_KICKED_FROM_CHANNEL,
                                ConnectionStatus.DISCONNECTED_REMOVED_DURING_RECONNECT,
                                ConnectionStatus.DISCONNECTED_AUTHENTICATION_FAILURE,
                                ConnectionStatus.AUDIO_REGION_CHANGE,
                                -> {
                                }

                                ConnectionStatus.ERROR_LOST_CONNECTION -> {
                                    bool = false
                                }

                                ConnectionStatus.ERROR_CANNOT_RESUME -> {
                                    bool = false
                                }

                                ConnectionStatus.ERROR_WEBSOCKET_UNABLE_TO_CONNECT -> {
                                    bool = false
                                }

                                ConnectionStatus.ERROR_UNSUPPORTED_ENCRYPTION_MODES -> {
                                    bool = false
                                }

                                ConnectionStatus.ERROR_UDP_UNABLE_TO_CONNECT -> {
                                    bool = false
                                }

                                ConnectionStatus.ERROR_CONNECTION_TIMEOUT -> {
                                    bool = false
                                }
                            }
                        }

                        override fun onUserSpeaking(user: User, speaking: Boolean) {
                        }

                    }

                }
                timeLoopTrigger.cancel()
                return@async ret
            }
        }
    }

    /**
     * Updates the joining message when on an error response!.
     *
     * @param mes the message
     * @param status the connection status
     */
    private fun voiceChannelJoiningFailedMessage(mes: Message, status: ConnectionStatus) {
        mes.editMessage("Failed to join the channel with status: `$status`!").mapToResult().queue { result ->
            result.onFailure { error ->
                LOG.error(
                    "Error when sending updated join message!",
                    error
                )
            }
        }
    }

}