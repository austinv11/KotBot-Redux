package kotbot.modules.impl

import kotbot.DataBase
import kotbot.KotBot
import kotbot.modules.*
import kotbot.shutdown
import sx.blah.discord.Discord4J
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser
import java.io.File
import java.util.*

/**
 * Represents core features which cannot be disabled.
 */
class CoreModule : BaseModule() {
    
    override fun enableModule(): Boolean {
        registerCommands(object: Command("info", arrayOf("i", "about"), "Displays general information about this bot.", arrayOf()) {
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
                builder.add("Available Memory: ${Runtime.getRuntime().freeMemory()/100000}mb / ${Runtime.getRuntime().maxMemory()/100000}mb")
                builder.add("Discord4J Version: ${Discord4J.VERSION}")
                builder.add("Kotlin Version: ${KotBot.KOTLIN_VERSION}")
                builder.add("JVM Version: ${System.getProperty("java.version")}")
                builder.add("```")
                return builder.toString()
            }
        }, object: Command("help", arrayOf("?", "h"), "Displays a list of commands as well as information on how to use them.",
                arrayOf(Parameter("command name", true))) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                val joiner = StringJoiner("\n")
                if (args.size < 1) {
                    joiner.add("```")
                    val moduleCommandMap = mutableMapOf<BaseModule, MutableList<Command>>()
                    BaseModule.commands.forEach { 
                        val (module, command) = it.value
                        if (!moduleCommandMap.containsKey(module))
                            moduleCommandMap.put(module, mutableListOf<Command>())
                        if (!command.isHidden) 
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
                        val usage = buildString { 
                            command.usage.forEach { 
                                append(" ")
                                if (it.optional) {
                                    append("optional:")
                                }
                                append("[${it.name}]")
                            }
                        }
                        joiner.add("__Usage:__ `${KotBot.CONFIG.PREFIX}${command.name}$usage`")
                    } catch(e: NullPointerException) {
                        throw CommandException("Command `$commandName` not found!")
                    }
                }
                return joiner.toString()
            }

        }, object: Command("update", arrayOf("up", "compile"), 
                "Clones the git repo for this bot and then compiles and launches the latest version.", arrayOf(), 
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
                startNewBotInstance()
                return "Launched new instance!"
            }

        }, object: Command("kill", arrayOf("rip", "die", "diepleasedie"), "Kills the bot, RIP.", arrayOf(), CommandPermissionLevels.OWNER) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                shutdown()
                return "Shutting down..." //You shouldn't see this
            }

        }, object: Command("restart", arrayOf("reboot"), "Restarts the bot.", arrayOf(), CommandPermissionLevels.OWNER) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                KotBot.LOGGER.info("Request to restart received...")
                startNewBotInstance()
                return "Restarting..."
            }
            
        }, object: Command("blacklist", arrayOf(), "Toggles the blacklist for this channel or guild. When blacklisted, " +
                "this bot will not monitor events in the channel/guild but it WILL still monitor for commands.", 
                arrayOf(Parameter("for guild (true/false)", true)), botPermissionLevel = CommandPermissionLevels.ADMIN) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                var isChannel = true
                if (args.size > 0) {
                    if (args[0] !is Boolean) {
                        throw CommandException("Argument `${args[0]}` is not a boolean!")
                    } else {
                        isChannel = !(args[0] as Boolean)
                    }
                }
                
                var result: Boolean
                if (isChannel) {
                    result = DataBase.toggleBlacklist(message.channel.id)
                } else {
                    result = DataBase.toggleBlacklist(message.guild.id)
                }
                
                return "This ${if (isChannel) "channel" else "guild"} is now ${if (result) "blacklisted" else "un-blacklisted"}!"
            }
            
        }, object: Command("promote", arrayOf(), "Promotes a user one permission level up.", arrayOf(Parameter("user (id or mention)")), 
                botPermissionLevel = CommandPermissionLevels.OWNER) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                if (args.size < 1)
                    throw CommandException("No user provided!")
                
                val effected = message.mentions.filter { it != KotBot.CLIENT.ourUser }
                        .getOrElse(0, { KotBot.CLIENT.getUserByID(args[0].toString()) }) 
                        ?: throw CommandException("Can't find user `${args[0]}`.")
                
                if (effected == KotBot.CLIENT.ourUser)
                    throw CommandException("Can't modify an owner's permissions!")

                val currentPerms = DataBase.getUserPermissions(effected.id)
                if (currentPerms == CommandPermissionLevels.ADMIN)
                    throw CommandException("User ${effected.mention()} already has the highest permission level possible (`$currentPerms`)")
                
                DataBase.updateUserPermissions(effected.id, CommandPermissionLevels.values()[currentPerms.ordinal - 1])
                
                return "User ${effected.mention()} now has the permission level `${DataBase.getUserPermissions(effected.id)}`"
            }
            
        }, object: Command("demote", arrayOf(), "Demotes a user one permission level down.", arrayOf(Parameter("user (id or mention)")),
                botPermissionLevel = CommandPermissionLevels.OWNER) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                if (args.size < 1)
                    throw CommandException("No user provided!")

                val effected = message.mentions.filter { it != KotBot.CLIENT.ourUser }
                        .getOrElse(0, { KotBot.CLIENT.getUserByID(args[0].toString()) })
                        ?: throw CommandException("Can't find user `${args[0]}`.")

                if (effected == KotBot.CLIENT.ourUser)
                    throw CommandException("Can't modify an owner's permissions!")
                
                val currentPerms = DataBase.getUserPermissions(effected.id)
                if (currentPerms == CommandPermissionLevels.NONE)
                    throw CommandException("User ${effected.mention()} already has the lowest permission level possible (`$currentPerms`)")

                DataBase.updateUserPermissions(effected.id, CommandPermissionLevels.values()[currentPerms.ordinal + 1])

                return "User ${effected.mention()} now has the permission level `${DataBase.getUserPermissions(effected.id)}`"
            }
            
        }, object: Command("whoami", arrayOf(), "Gets your current user information.", arrayOf(), 
                botPermissionLevel = CommandPermissionLevels.NONE) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return buildUserInfoMessage(message.author)
            }
            
        }, object: Command("whois", arrayOf(), "Gets current user information for the provided user.", arrayOf(Parameter("user (id or mention)"))) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                if (args.size < 1)
                    throw CommandException("No user provided!")

                val effected = message.mentions.filter { it != KotBot.CLIENT.ourUser }
                        .getOrElse(0, { KotBot.CLIENT.getUserByID(args[0].toString()) })
                        ?: throw CommandException("Can't find user `${args[0]}`.")
                
                return buildUserInfoMessage(effected)
            }

        }, object: Command("whereami", arrayOf(), "Gets the current channel information.", arrayOf(Parameter("user (id or mention)")),
                botPermissionLevel = CommandPermissionLevels.NONE) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                val channel = message.channel
                return buildString {
                    appendln("Information for channel ${channel.mention()}:")
                    appendln("```")
                    appendln("Name: #${channel.name}")
                    appendln("ID: ${channel.id}")
                    appendln("Channel Creation Date: ${channel.creationDate}")
                    appendln("Is Channel Blacklisted?: ${DataBase.checkBlacklist(channel.id)}")
                    appendln("```")
                    val guild = channel.guild
                    appendln("In guild ${guild.name}:")
                    appendln("```")
                    appendln("ID: ${guild.id}")
                    appendln("Owner: ${guild.owner.name}#${guild.owner.discriminator}")
                    appendln("Icon: ${guild.iconURL}")
                    appendln("Region: ${guild.region}")
                    appendln("Guild Creation Date: ${guild.creationDate}")
                    appendln("Is Guild Blacklisted?: ${DataBase.checkBlacklist(guild.id)}")
                    append("```")
                }
            }
            
        })
        return true
    }
    
    private fun startNewBotInstance() {
        ProcessBuilder("java", "-jar", "./KotBot.jar", "${KotBot.CLIENT.token.removePrefix("Bot ")}").inheritIO().start()
    }
    
    private fun buildUserInfoMessage(user: IUser): String {
        return buildString { 
            appendln("Information for user ${user.mention()}:")
            appendln("```")
            appendln("Name: ${user.name}#${user.discriminator}")
            appendln("ID: ${user.id}")
            appendln("Is a Bot?: ${user.isBot}")
            appendln("Avatar: ${user.avatarURL}")
            appendln("User Account Creation Date: ${user.creationDate}")
            appendln("KotBot Permission Level: ${DataBase.getUserPermissions(user.id)}")
            append("```")
        }
    }

    override fun disableModule() {
        throw UnsupportedOperationException()
    }
}
