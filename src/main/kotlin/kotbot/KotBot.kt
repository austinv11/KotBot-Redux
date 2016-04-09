package kotbot

import com.google.gson.GsonBuilder
import com.mashape.unirest.http.Unirest
import kotbot.modules.BaseModule
import kotbot.rest.Endpoints
import kotbot.rest.RequestType
import kotbot.rest.Server
import kotbot.rest.exceptions.AuthFailedException
import kotbot.rest.objects.Information
import kotbot.rest.objects.Message
import kotbot.rest.objects.Ping
import kotbot.utils.fromJson
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import spark.Request
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.IListener
import sx.blah.discord.handle.impl.events.DiscordDisconnectedEvent
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.modules.ModuleLoader
import sx.blah.discord.util.DiscordException
import java.io.File
import java.time.LocalDateTime

fun main(args: Array<String>) {
    if (args.size < 1)
        throw IllegalArgumentException("Requires a bot account token!")
    
    //Has to be before logging in so that modules automatically get registered to the client
    registerModules()
    
    KotBot.LOGGER.info("Logging in...")
    val token = args[0]
    try {
        KotBot.INSTANCE.client = ClientBuilder().withToken(token).login()
    } catch(e: DiscordException) {
        KotBot.LOGGER.error("Error occurred while logging in! Aborting startup...", e)
        return
    }
    
    searchAndDestroy()
    
    initLifcycleListeners()
    
    //Handles cleanup
    Runtime.getRuntime().addShutdownHook(object: Thread() {
        override fun run() {
            KotBot.CONFIG.save()//So that the config file is saved at shut down in case of changes while running
            BaseModule.ASYNC_EXECUTOR.shutdownNow()
        }
    })
    
    initRESTServer()
}

/**
 * Dynamically finds and registers modules from this bot.
 */
private fun registerModules() {
    val modules = Reflections(ConfigurationBuilder().setUrls(ClasspathHelper.forPackage("kotbot.modules.impl"))
            .setScanners(SubTypesScanner())).getSubTypesOf(BaseModule::class.java)

    for (moduleClass in modules) {
        ModuleLoader.addModuleClass(moduleClass)
    }
}

/**
 * Searches for another KotBot instance and attempts to destroy that instance.
 */
private fun searchAndDestroy() {
    //The following: Attempts to query the KotBot api, if it doesn't error, it then requests that the other kotbot instance shuts down.
    try {
        val info: Information = KotBot.GSON.fromJson(Unirest.get(Endpoints.BOT).headers(mapOf(Pair("auth", KotBot.CLIENT.token))).asString().body)
        KotBot.startTime = LocalDateTime.parse(info.startTime)
        KotBot.instances += info.instances 

        KotBot.LOGGER.info("Another KotBot instance found, closing it...")
        Unirest.delete(Endpoints.BOT).headers(mapOf(Pair("auth", KotBot.CLIENT.token))).asStringAsync().cancel(true)
    } catch(e: Exception) {
        KotBot.LOGGER.warn("Unable to reach KotBot's REST API, assuming no other running instance...")
    }
}

/**
 * Creates listeners for Ready and Disconnect events.
 */
private fun initLifcycleListeners() {
    KotBot.CLIENT.dispatcher.registerListener(IListener<ReadyEvent> {
        if (KotBot.isReconnecting) {
            KotBot.LOGGER.info("Reconnect succeeded!")
            KotBot.isReconnecting = false
        }

        KotBot.LOGGER.info("Logged in as ${it.client.ourUser.name}")
    })

    KotBot.CLIENT.dispatcher.registerListener(IListener<DiscordDisconnectedEvent> {
        KotBot.LOGGER.warn("Discord has been disconnected for reason ${it.reason}")

        if (KotBot.isReconnecting) {
            KotBot.LOGGER.error("Unable to reconnect, aborting!")
            shutdown()
            return@IListener
        }

        if (it.reason != DiscordDisconnectedEvent.Reason.LOGGED_OUT) {
            KotBot.LOGGER.info("Attempting to reconnect...")
            try {
                KotBot.isReconnecting = true
                KotBot.CLIENT.login()
            } catch(e: Exception) {
                KotBot.LOGGER.error("Error occurred while attempting to reconnect, aborting!", e)
                shutdown()
                return@IListener
            }
        }
    })

    KotBot.CLIENT.dispatcher.registerListener(IListener<MessageReceivedEvent> {
        BaseModule.onMessageReceived(it)
    })
    
    DataBase.init()
}

/**
 * Initializes the rest API server.
 */
private fun initRESTServer() {
    Server.startup()
    
    val checkAuthToken = { request: Request -> 
        if (request.headers("auth") == null || !Server.isTokenValid(request.headers("auth")))
            throw AuthFailedException()
    }
    
    //Exception handlers start:
    Server[AuthFailedException::class] = { exception, request, response -> response.status(403); response.body(KotBot.GSON.toJson(Message("Authentication failed"))) }
    
    //Requests start:
    Server["/kotbot/api/bot/", RequestType.GET] = { request, response -> checkAuthToken(request); Information(KotBot.CLIENT.launchTime.toString(), KotBot.instances) }
    Server["/kotbot/api/bot/", RequestType.DELETE] = { request, response -> checkAuthToken(request); shutdown(); "{}" }
    Server["/kotbot/api/ping/", RequestType.GET] = { request, response -> Ping(System.currentTimeMillis()) }
}

/**
 * Shuts down this bot.
 */
fun shutdown() {
    KotBot.LOGGER.info("Shutdown request received, shutting down...")
    if (!KotBot.isReconnecting) //Prevents infinite reconnect loops
        KotBot.CLIENT.logout()
    Server.stop()
    System.exit(1)
}

class KotBot {
    
    var client: IDiscordClient? = null
    
    companion object {
        final val LOGGER = LoggerFactory.getLogger("KotBot")
        final val GSON = GsonBuilder().serializeNulls().setPrettyPrinting().create()
        final val CONFIG_FILE = File("./config.json")
        final val CONFIG = if (CONFIG_FILE.exists()) GSON.fromJson<Config>(CONFIG_FILE.reader()) else Config()
        const val VERSION = "1.0.0-SNAPSHOT"
        const val KOTLIN_VERSION = "1.0.0"
        val OWNER_NAME: String
            get() {return if (INSTANCE.client != null) "${OWNER.name}#${OWNER.discriminator}" else "Austin"}
        val OWNER: IUser
            get() {return CLIENT.getUserByID(CONFIG.OWNER)!!}
        
        init {
            CONFIG.save() //Ensures that a config file is always at least present.
            CONFIG.load()
        }
        
        var startTime = LocalDateTime.now()
        var instances = 1
        var isReconnecting = false
        
        final var CLIENT: IDiscordClient
            get() = INSTANCE.client!!
            set(value) {}
        
        final val INSTANCE: KotBot = KotBot()
    }
}
