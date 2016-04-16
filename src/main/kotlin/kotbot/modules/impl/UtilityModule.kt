package kotbot.modules.impl

import kotbot.KotBot
import kotbot.modules.*
import sx.blah.discord.api.IListener
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import java.util.*
import kotlin.concurrent.thread

/**
 * This represents a collection of random utilities.
 */
class UtilityModule : BaseModule() {
    
    override fun enableModule(): Boolean {
        registerCommands(object: Command("cli", arrayOf(), "Mirrors the bot's OS's CLI.", arrayOf(Parameter("arguments", true)),
                CommandPermissionLevels.OWNER, async = true) {
                            override fun execute(message: IMessage, args: List<Any>): String? {//FIXME: Some weird things happen in the cli
                                message.channel.sendMessage("Entering CLI mode, any messages ${message.author.mention()} sends in this " +
                                        "channel will be sent to this process until it terminates.")
                                val command = message.content.split(" ").toMutableList()
                                command.removeAt(0)
                                val process = ProcessBuilder(command)
                                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                                        .redirectErrorStream(true)
                                        .redirectInput(ProcessBuilder.Redirect.PIPE)
                                        .start()
                                val monitor = thread(isDaemon = true, block = { //InputStream monitor
                                    val scanner = Scanner(process.inputStream)
                                    while (process.isAlive) {
                                        if (scanner.hasNextLine()) {
                                            message.channel.sendMessage("`${scanner.nextLine()}`")
                                        }
                                    }
                                })
                                val channelCLI = IListener<MessageReceivedEvent> {
                                    if (it.message.channel == message.channel && it.message.author == message.author) {
                                        process.outputStream.bufferedWriter().appendln(it.message.content)
                                    }
                                }
                                client.dispatcher.registerListener(channelCLI)
                                process.waitFor()
                                client.dispatcher.unregisterListener(channelCLI)
                                return "CLI mode ended."
                            }
                
                        },
                object: Command("prefix", arrayOf(), "Switches the bot's active prefix.", arrayOf(Parameter("new prefix")),
                        CommandPermissionLevels.ADMIN) {
                    override fun execute(message: IMessage, args: List<Any>): String? {
                        if (args.size < 1)
                            throw CommandException("Need a new prefix to swtich to!")
                        
                        KotBot.CONFIG.PREFIX = args[0].toString()
                        KotBot.CONFIG.save()
                        
                        return "KotBot's prefix changed to `${KotBot.CONFIG.PREFIX}`"
                    }

                })
        return true
    }

    override fun disableModule() { //TODO
        throw UnsupportedOperationException()
    }
}
