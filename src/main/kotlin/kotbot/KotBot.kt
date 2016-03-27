package kotbot

import com.google.gson.GsonBuilder
import com.mashape.unirest.http.Unirest
import kotbot.rest.Endpoints
import kotbot.rest.RequestType
import kotbot.rest.Server
import kotbot.rest.exceptions.AuthFailedException
import kotbot.rest.objects.Information
import kotbot.rest.objects.Message
import kotbot.rest.objects.Ping
import kotbot.utils.fromJson
import org.slf4j.LoggerFactory
import spark.Request
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.IListener
import sx.blah.discord.handle.impl.events.DiscordDisconnectedEvent
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.util.DiscordException
import java.time.LocalDateTime

fun main(args: Array<String>) {
    if (args.size < 1)
        throw IllegalArgumentException("Requires a bot account token!")
    
    KotBot.LOGGER.info("Logging in...")
    val token = args[0]
    try {
        KotBot.INSTANCE.client = ClientBuilder().withToken(token).login()
    } catch(e: DiscordException) {
        KotBot.LOGGER.error("Error occurred while logging in! Aborting startup...", e)
        return
    }
    
    //The following: Attempts to query the KotBot api, if it doesn't error, it then requests that the other kotbot instance shuts down.
    try {
        val info: Information = KotBot.GSON.fromJson(Unirest.get(Endpoints.BOT).headers(mapOf(Pair("auth", KotBot.CLIENT.token))).asString().body)
        KotBot.startTime = LocalDateTime.parse(info.startTime)
        
        KotBot.LOGGER.info("Another KotBot instance found, closing it...")
        Unirest.delete(Endpoints.BOT).headers(mapOf(Pair("auth", KotBot.CLIENT.token))).asStringAsync().cancel(true)
    } catch(e: Exception) {
        KotBot.LOGGER.warn("Unable to reach KotBot's REST API, assuming no other running instance...")
    }

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
    
    initRESTServer()
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
    Server["/kotbot/api/bot/", RequestType.GET] = { request, response -> checkAuthToken(request); Information(KotBot.CLIENT.launchTime.toString()) }
    Server["/kotbot/api/bot/", RequestType.DELETE] = { request, response -> checkAuthToken(request); shutdown(); "{}" }
    Server["/kotbot/api/ping/", RequestType.GET] = { request, response -> Ping(System.currentTimeMillis()) }
}

/**
 * Shuts down this bot.
 */
private fun shutdown() {
    KotBot.LOGGER.info("Shutdown request received, shutting down...")
    KotBot.CLIENT.logout()
    Server.stop()
    System.exit(1)
}

class KotBot {
    
    var client: IDiscordClient? = null
    
    companion object {
        final val LOGGER = LoggerFactory.getLogger("KotBot")
        final val GSON = GsonBuilder().serializeNulls().setPrettyPrinting().create()
        
        var startTime = LocalDateTime.now()
        var isReconnecting = false
        
        final var CLIENT: IDiscordClient
            get() = INSTANCE.client!!
            set(value) {}
        
        final val INSTANCE: KotBot = KotBot()
    }
}
