package kotbot.modules.impl

import kotbot.KotBot
import kotbot.modules.BaseModule
import kotbot.modules.Command
import kotbot.modules.CommandException
import kotbot.modules.CommandPermissionLevels
import kotbot.shutdown
import sx.blah.discord.Discord4J
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IMessage
import java.io.File
import java.util.*

/**
 * Represents core features which cannot be disabled.
 */
class CoreModule : BaseModule() {
    
    override fun enable(client: IDiscordClient): Boolean {
        registerCommands(object: Command("info", arrayOf("i", "about"), "Displays general information about this bot.", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                var builder = StringJoiner("\n")
                builder.add("```")
                builder.add("KotBot v${KotBot.VERSION}")
                builder.add("--------${StringBuilder().padEnd(KotBot.VERSION.length, '-')}")
                builder.add("KotBot is a bot written in Kotlin built on top of the Discord4J library.")
                builder.add("Invite Link: ${KotBot.CONFIG.INVITE_LINK}")
                builder.add("Github: https://github.com/austinv11/KotBot-Redux")
                builder.add("Prefix: \"${KotBot.CONFIG.PREFIX}\" or @${KotBot.CLIENT.ourUser.name}#" +
                        "${KotBot.CLIENT.ourUser.discriminator}")
                builder.add("Connected To ${KotBot.CLIENT.guilds.size} servers")
                builder.add("Bot User: ${KotBot.CLIENT.ourUser.name}#${KotBot.CLIENT.ourUser.discriminator} (ID: " +
                        "${KotBot.CLIENT.ourUser.id})")
                builder.add("Bot Owner: ${KotBot.OWNER_NAME} (ID: ${KotBot.OWNER.id})")
                builder.add("Bot Instance Iterations: ${KotBot.instances}")
                builder.add("Initial Bot Instance Started At: ${KotBot.startTime}")
                builder.add("Last Bot Instance Started At: ${Discord4J.getLaunchTime()}")
                builder.add("Discord4J Version: ${Discord4J.VERSION}")
                builder.add("Kotlin Version: ${KotBot.KOTLIN_VERSION}")
                builder.add("JVM Version: ${System.getProperty("java.version")}")
                builder.add("```")
                return builder.toString()
            }
        }, object: Command("help", arrayOf("?", "h"), "Displays a list of commands as well as information on how to use them.",
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

        }, object: Command("update", arrayOf("up", "compile"), 
                "Clones the git repo for this bot and then compiles and launches the latest version.", "", 
                CommandPermissionLevels.OWNER, async = true) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                KotBot.LOGGER.info("Request to recompile received, ensuring git is installed...")
                ProcessBuilder("git", "--version").inheritIO().start().waitFor() //Ensures git is installed
                KotBot.LOGGER.info("Git installed, cloning the KotBot repo...")
                val gitDir = File("./kotbot-git/")
                val localRepoDir = File("./kotbot-git/KotBot-Redux/")
                if (!gitDir.exists())
                    gitDir.mkdir()
                if (localRepoDir.exists())
                    localRepoDir.deleteRecursively()
                ProcessBuilder("git", "clone", "https://github.com/austinv11/KotBot-Redux.git").inheritIO().directory(gitDir).start().waitFor()
                localRepoDir.deleteOnExit()
                KotBot.LOGGER.info("Git repo cloned, building KotBot...")
                ProcessBuilder("./gradlew", "installShadowApp").inheritIO().directory(localRepoDir).start().waitFor()
                val newJar = File("./kotbot-git/KotBot-Redux/build/libs/KotBot-all.jar")
                val oldJar = File("./KotBot.jar")
                if (!oldJar.exists()) {
                    KotBot.LOGGER.warn("KotBot jar not found! Creating placeholder...")
                    oldJar.createNewFile()
                }
                if (!oldJar.renameTo(File("./KotBot-backup.jar"))) {
                    KotBot.LOGGER.error("Unable to make backup jar, aborting update!")
                    throw CommandException("Unable to make backup jar!")
                }
                if (newJar.renameTo(File("./KotBot.jar"))) {
                    oldJar.deleteOnExit()
                } else {
                    KotBot.LOGGER.error("Unable to move updated jar, aborting update!")
                    oldJar.renameTo(File("./KotBot.jar"))
                    throw CommandException("Unable to move updated jar!")
                }
                KotBot.LOGGER.info("KotBot built and replaced, running...")
                ProcessBuilder("java", "-jar", "./KotBot.jar", "${KotBot.CLIENT.token.removePrefix("Bot ")}").inheritIO().start()
                return "Launched new instance!"
            }

        }, object: Command("kill", arrayOf("rip", "die", "diepleasedie"), "Kills the bot RIP.", "", CommandPermissionLevels.OWNER) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                shutdown()
                return "Shutting down..." //You shouldn't see this
            }

        })
        return true
    }

    override fun disable() {
        throw UnsupportedOperationException()
    }
}
