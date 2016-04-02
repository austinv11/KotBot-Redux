package kotbot.modules.impl

import kotbot.KotBot
import kotbot.modules.BaseModule
import kotbot.modules.Command
import kotbot.modules.CommandException
import sx.blah.discord.Discord4J
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IMessage
import java.util.*

/**
 * Represents core features which cannot be disabled.
 */
class CoreModule : BaseModule() {
    
    override fun enable(client: IDiscordClient): Boolean {
        registerCommands(object: Command("info", arrayOf("i", "about"), "Displays general information about this bot", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                var builder = StringJoiner("\n")
                builder.add("```")
                builder.add("KotBot v${KotBot.VERSION}")
                builder.add("--------${StringBuilder().padEnd(KotBot.VERSION.length, '-')}")
                builder.add("KotBot is a bot written in Kotlin built on top of the Discord4J library.")
                builder.add("Github: https://github.com/austinv11/KotBot-Redux")
                builder.add("Prefix: \"${KotBot.CONFIG.PREFIX}\" or @${KotBot.CLIENT.ourUser.name}#" +
                        "${KotBot.CLIENT.ourUser.discriminator}")
                builder.add("Connected to ${KotBot.CLIENT.guilds.size} servers")
                builder.add("Bot User: ${KotBot.CLIENT.ourUser.name}#${KotBot.CLIENT.ourUser.discriminator} (ID: " +
                        "${KotBot.CLIENT.ourUser.id})")
                builder.add("Bot Owner: ${KotBot.OWNER_NAME} (ID: ${KotBot.OWNER.id})")
                builder.add("Discord4J Version: ${Discord4J.VERSION}")
                builder.add("Kotlin Version: ${KotBot.KOTLIN_VERSION}")
                builder.add("JVM Version: ${System.getProperty("java.version")}")
                builder.add("```")
                return builder.toString()
            }
        }, object: Command("help", arrayOf("?", "h"), "Displays a list of commands as well as information on how to use them",
                "optional:[command name]") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                val joiner = StringJoiner("\n")
                if (args.size < 1) {
                    joiner.add("```")
                    val moduleCommandMap = mutableMapOf<BaseModule, MutableList<Command>>()
                    BaseModule.commands.forEach { 
                        val (module, command) = it.value
                        if (!moduleCommandMap.containsKey(module))
                            moduleCommandMap.put(module, mutableListOf<Command>())
                        moduleCommandMap[module]!!.add(command)
                    }
                    for (module in moduleCommandMap.keys) {
                        joiner.add("Commands for module '${module.name}':")
                        for (command in moduleCommandMap[module]!!)
                            joiner.add("*${command.name}")
                    }
                    joiner.add("```")
                } else {
                    val commandName = args[0].toString()
                    try {
                        val (module, command) = BaseModule.commands.filter { it.key.contains(commandName) }.values.firstOrNull()!!
                        joiner.add("Help page for `$commandName`")
                        joiner.add("__Name:__ `${command.name}`")
                        joiner.add("__Module:__ `${module.name}`")
                        joiner.add("__Permission Level:__ `${command.botPermissionLevel}`")
                        val aliasList = StringJoiner(", ")
                        for (alias in command.aliases)
                            aliasList.add(alias)
                        joiner.add("__Aliases:__ `${aliasList.toString()}`")
                        joiner.add("__Description:__ ${command.description}")
                        joiner.add("__Usage:__ `${KotBot.CONFIG.PREFIX}${command.name} ${command.usage}`")
                    } catch(e: NullPointerException) {
                        throw CommandException("Command `$commandName` not found!")
                    }
                }
                return joiner.toString()
            }

        })
        return true
    }

    override fun disable() {
        throw UnsupportedOperationException()
    }
}
