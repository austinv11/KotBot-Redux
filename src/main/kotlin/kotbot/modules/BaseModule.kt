package kotbot.modules

import kotbot.KotBot
import sx.blah.discord.Discord4J
import sx.blah.discord.api.internal.DiscordUtils
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.modules.IModule
import sx.blah.discord.util.MissingPermissionsException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Module which implements basic features constant in all modules
 */
abstract class BaseModule : IModule {
    
    companion object {
        var commands = mutableMapOf<List<String>, Pair<BaseModule, Command>>()
        final val ASYNC_EXECUTOR = Executors.newCachedThreadPool() //This is used for executing commands asynchronously
        var lastAsyncCommands = mutableMapOf<IChannel, Pair<Command, Future<*>>>() //Keeps track of async commands

        fun onMessageReceived(event: MessageReceivedEvent) {
            var cleanedMessage: String //Message without prefix

            if (event.message.content.startsWith(KotBot.CONFIG.PREFIX)) {
                cleanedMessage = event.message.content.removePrefix(KotBot.CONFIG.PREFIX)
            } else if (event.message.content.startsWith(KotBot.CLIENT.ourUser.mention())) {
                cleanedMessage = event.message.content.removePrefix(KotBot.CLIENT.ourUser.mention())
            } else {
                return //No command
            }

            val commandName = if (cleanedMessage.contains(" ")) cleanedMessage.split(" ")[0] else cleanedMessage
            cleanedMessage = cleanedMessage.removePrefix(commandName).trim()
            val command: Command
            try {
                command = commands.filter { it.key.contains(commandName) }.values.firstOrNull()!!.second
            } catch(e: NullPointerException) {
                event.message.channel.sendMessage(formatErrorMessage("Command `$commandName` not found! Use ${KotBot.CONFIG.PREFIX}help for a list of commands."))
                return
            }

            if ((!command.directMessages && event.message.channel.isPrivate) ||
                    (!command.channelMessages && !event.message.channel.isPrivate)) {
                event.message.channel.sendMessage(formatErrorMessage("Command `$commandName` cannot be used here"))
                return
            }

            try {
                if (!hasPermission(event.message.author, command.botPermissionLevel)) {
                    event.message.channel.sendMessage(formatErrorMessage("${event.message.author.mention()}, you don't have the required permissions for this command!"))
                    return
                }

                if (!event.message.channel.isPrivate)
                    DiscordUtils.checkPermissions(KotBot.CLIENT, event.message.channel, command.permissionsRequired)

                var args: MutableList<String>?
                if (cleanedMessage.contains(" ")) {
                    args = cleanedMessage.split(" ").toMutableList()
                    args = args.drop(1).toMutableList()
                } else if (!cleanedMessage.isEmpty()) {
                    args = mutableListOf(cleanedMessage)
                } else {
                    args = null
                }

                if (command.async) {
                    if (!event.message.channel.typingStatus)
                        event.message.channel.toggleTypingStatus() //The bot appears to type as it is processing the command

                    val commandFuture = ASYNC_EXECUTOR.submit {
                        try {
                            val result = command.execute(event.message, generateArgs(args))
                            if (result != null)
                                event.message.channel.sendMessage(result)

                        } catch(e: Exception) {
                            event.message.channel.sendMessage(formatErrorMessage(e))
                            e.printStackTrace()
                        } finally {
                            if (event.message.channel.typingStatus)
                                event.message.channel.toggleTypingStatus()

                            lastAsyncCommands.remove(event.message.channel)
                        }
                    }

                    lastAsyncCommands.put(event.message.channel, Pair(command, commandFuture))

                } else {
                    val result = command.execute(event.message, generateArgs(args))

                    if (result != null)
                        event.message.channel.sendMessage(result)
                }
            } catch(e: Exception) {
                event.message.channel.sendMessage(formatErrorMessage(e))
                e.printStackTrace()
            }
        }

        fun formatErrorMessage(message: String?): String {
            return "__**ERROR EXECUTING COMMAND:**__ $message"
        }

        fun formatErrorMessage(exception: Exception): String {
            if (exception is CommandException) {
                return formatErrorMessage(exception.message) //Here in case I add special handling
            } else if (exception is MissingPermissionsException) {
                return formatErrorMessage(exception.errorMessage)
            } else {
                return formatErrorMessage("Unhandled exception `${exception.javaClass.simpleName}: ${exception.message}`")
            }
        }

        fun hasPermission(user: IUser, requiredLevel: CommandPermissionLevels): Boolean { //FIXME: Add permissions database
            return if (requiredLevel == CommandPermissionLevels.OWNER) user == KotBot.OWNER else true
        }

        fun generateArgs(args: List<String>?): List<Any> {
            var newArgs = mutableListOf<Any>()

            if (args != null) {
                for (string in args) {
                    if (string.equals("true", true) || string.equals("false", true)) {
                        newArgs.add(string.toBoolean())
                        continue
                    }
                    try {
                        if (string.contains('.')) {
                            newArgs.add(string.toDouble())
                        } else {
                            newArgs.add(string.toInt())
                        }
                    } catch(e: NumberFormatException) {
                        newArgs.add(string)
                    }
                }
            }

            return newArgs
        }
    }
    
    override fun getName(): String? {
        return "KotBot ${camelcaseToSpaced(this.javaClass.simpleName.removeSuffix("Kt"))}"
    }

    override fun getVersion(): String? {
        return KotBot.VERSION
    }

    override fun getMinimumDiscord4JVersion(): String? {
        return Discord4J.VERSION //This bot will always be built upon a compatible version of Discord4J
    }

    override fun getAuthor(): String? {
        return KotBot.OWNER_NAME
    }

    /**
     * This registers a set of commands.
     */
    fun registerCommands(vararg commands: Command) {
        for (command in commands) {
            val names = mutableListOf(command.name)
            names.addAll(command.aliases)
            BaseModule.commands.put(names, Pair(this, command))
            KotBot.LOGGER.trace("Registered command ${command.name}")
        }
    }
    
    //Turns FooBar -> Foo Bar
    private fun camelcaseToSpaced(name: String): String {
        val stringBuilder = StringBuilder()
        
        for (letter in name.asIterable()) {
            if (!stringBuilder.isEmpty() && letter.isUpperCase()) {
                stringBuilder.append(" ")
            }
            stringBuilder.append(letter)
        }
        
        return stringBuilder.toString()
    }
}
