package kotbot.utils

import sx.blah.discord.api.Event
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.IListener
import sx.blah.discord.util.RequestBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This Kotlin-ifies Discord4J
 */

/**
 * Locks thread execution until an event is received and the event handler returns true.
 */
fun <T: Event> Any.waitFor(client: IDiscordClient, handler: (event: T) -> Boolean) : T {
    var lock = AtomicBoolean(true)
    var event: T? = null
    val eventWatcher = IListener<T> {
        if (handler(it)) {
            lock.set(false)
            event = it
        }
    }
    client.dispatcher.registerListener(eventWatcher)
    this.run {
        while(lock.get()) {}
        client.dispatcher.unregisterListener(eventWatcher)
        return event!!
    }
}

/**
 * Locks thread execution until an event is received.
 */
fun <T: Event> Any.waitFor(client: IDiscordClient) : T {
    return this.waitFor<T>(client, { true })
}

/**
 * Buffers any requests which may hit a rate limit.
 */
fun bufferedRequest(request: () -> Unit) {
    RequestBuffer.request { request() }
}
