package kotbot.modules

import kotbot.KotBot
import sx.blah.discord.Discord4J
import sx.blah.discord.api.EventSubscriber
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
    
    private var commands = mapOf<Array<String>, Command>()
    
    companion object {
        final val ASYNC_EXECUTOR = Executors.newCachedThreadPool() //This is used for executing commands asynchronously
        var lastAsyncCommands = mutableMapOf<IChannel, Pair<Command, Future<*>>>() //Keeps track of async commands
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
            val names = arrayOf(command.name)
            names + command.aliases
            this.commands + Pair(names, command)
        }
    }
    
    //Turns FooBar -> Foo Bar
    private fun camelcaseToSpaced(name: String): String {
        val stringBuilder = StringBuilder()
        
        for (letter in name.asIterable()) {
            if (stringBuilder.isEmpty() && letter.isUpperCase()) {
                stringBuilder.append(" ")
            }
            stringBuilder.append(letter)
        }
        
        return stringBuilder.toString()
    }
    
    @EventSubscriber
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
        val possibleCommands = commands.filter { it.key.contains(commandName) }.values
        if (possibleCommands.size > 1)
            KotBot.LOGGER.warn("Multiple possible commands found for '$commandName'")
        val command = possibleCommands.firstOrNull()
        
        if (command == null) {
            event.message.channel.sendMessage(formatErrorMessage("Command `$commandName` not found!"))
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

            var args: List<String>
            if (cleanedMessage.contains(" ")) {
                args = cleanedMessage.split(" ")
                args = args.drop(1)
            } else {
                args = listOf()
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
                    } finally {
                        if (event.message.channel.typingStatus)
                            event.message.channel.toggleTypingStatus()
                        
                        lastAsyncCommands.remove(event.message.channel)
                    }
                }
                
                lastAsyncCommands + Pair(event.message.channel, Pair(command, commandFuture))
                
            } else {
                val result = command.execute(event.message, generateArgs(args))
                
                if (result != null)
                    event.message.channel.sendMessage(result)
            }
        } catch(e: Exception) {
            event.message.channel.sendMessage(formatErrorMessage(e))
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
        return true
    }
    
    fun generateArgs(args: List<String>): Array<Any> {
        var newArgs = arrayOf<Any>()
        
        for (string in args) {
            if (string.equals("true", true) || string.equals("false", true)) {
                newArgs + string.toBoolean()
                continue
            }
            try {
                if (string.contains('.')) {
                    newArgs + string.toDouble()
                } else {
                    newArgs + string.toInt()
                }
            } catch(e: NumberFormatException) {
                newArgs + string
            }
        }
        
        return newArgs
    }
}
