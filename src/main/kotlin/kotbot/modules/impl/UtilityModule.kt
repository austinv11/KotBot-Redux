package kotbot.modules.impl

import kotbot.KotBot
import kotbot.modules.*
import sx.blah.discord.handle.obj.IMessage
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.script.ScriptEngineManager

/**
 * This represents a collection of random utilities.
 */
class UtilityModule : BaseModule() { //TODO: define command
    
    override fun enableModule(): Boolean {
        registerCommands(object: Command("prefix", arrayOf(), "Switches the bot's active prefix.", arrayOf(Parameter("new prefix")),
                        CommandPermissionLevels.ADMIN) {
                    override fun execute(message: IMessage, args: List<Any>): String? {
                        if (args.size < 1)
                            throw CommandException("Need a new prefix to swtich to!")
                        
                        KotBot.CONFIG.PREFIX = args[0].toString()
                        KotBot.CONFIG.save()
                        
                        return "KotBot's prefix changed to `${KotBot.CONFIG.PREFIX}`"
                    }

                }, 
                object: Command("eval", arrayOf("evaluate"), "Evaluates an executable script.", 
                arrayOf(Parameter("language", true), Parameter("script, required if a language is provided", true)),
                CommandPermissionLevels.ADMIN, async = true) {
                    override fun execute(message: IMessage, args: List<Any>): String? {
                        if (args.size == 0) {
                            return buildString { appendln("Available languages are: "); Engines.values().forEach { appendln("* ${it.name.toLowerCase()}") } }
                        } else if (args.size > 1) {
                            return Engines.getEngineForInput(args[0].toString())?.execute(
                                    message.content.removePrefix(buildString { 
                                        val strings = message.content.split(' '); 
                                        append("${strings[0]} ${strings[1]} ") 
                                    })) ?: throw CommandException("Can't find engine for language `${args[0]}`.")
                        } else {
                            throw CommandException("Requires either 0 or 2 arguments.")
                        }
                    }

                })
        return true
    }
    
    enum class Engines(val aliases: Array<String>) {
        
        BASH(arrayOf("shell", "sh")), JAVASCRIPT(arrayOf("js"));
        
        companion object {

            val scriptFactory = ScriptEngineManager();
            
            init { //Add additional things to the script contexts
                scriptFactory.bindings.put("Client", KotBot.CLIENT)
            }
            
            fun getEngineForInput(input: String): Engines? {
                try {
                    return Engines.valueOf(input.toUpperCase())
                } catch (e: Exception) {
                    return Engines.values().filter { it.aliases.contains(input.toLowerCase()) }.getOrNull(0)
                }
            }
        }
        
        fun execute(script: String): String? { //TODO: Add support for luaj, jruby, jython, groovy, java (maybe use javassist?) and kotlin 
            try {
                val returnVal: String?
                when (this) {
                    BASH -> {
                        val SHEBANG = "#!/usr/bin/env bash\n"
                        val temp = File.createTempFile("eval", ".sh")
                        temp.writeText(SHEBANG + script)
                        val process = ProcessBuilder("sh", temp.absolutePath).redirectErrorStream(true).start()
                        process.waitFor()
                        returnVal = buildString { process.inputStream.bufferedReader().forEachLine { appendln(it) } }
                        temp.delete()
                    }
                    JAVASCRIPT -> {
                        val engine = scriptFactory.getEngineByName("JavaScript");
                        returnVal = engine.eval(script).toString()
                    }
                    else -> returnVal = null
                }
                return "Script returned: \n```\n$returnVal\n```"
            } catch (e: Exception) {
                val writer = StringWriter()
                e.printStackTrace(PrintWriter(writer))
                return "Script exited with error: ```\n${writer.toString()}\n```"
            }
        }
    }

    override fun disableModule() { //TODO
        throw UnsupportedOperationException()
    }
}
