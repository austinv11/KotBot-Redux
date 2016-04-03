package kotbot.modules.impl

import kotbot.KotBot
import kotbot.modules.BaseModule
import kotbot.modules.Command
import sx.blah.discord.Discord4J
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IMessage
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Represents functions which analyzes various things
 */
class AnalyticsModule : BaseModule() {
    
    override fun enable(client: IDiscordClient?): Boolean {
        registerCommands(object: Command("uptime", arrayOf(), "Provides information regarding the bot's uptime.", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                val joiner = StringJoiner("\n")
                joiner.add("__Uptime:__")
                val cumulativeDifference = System.currentTimeMillis()-KotBot.startTime.atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                joiner.add("Cumulative Uptime: `${getUptimeMessage(cumulativeDifference)}`")
                val instanceDifference = System.currentTimeMillis()-Discord4J.getLaunchTime()
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                joiner.add("Instance Uptime: `${getUptimeMessage(instanceDifference)}`")
                joiner.add("Instance Iterations: `${KotBot.instances}`")
                return joiner.toString()
            }
        }, object: Command("ping", arrayOf(), "Provides a brief ping analysis.", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                var joiner = StringJoiner("\n")
                joiner.add("Pong!")
                joiner.add("Received this message `${System.currentTimeMillis()-message.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}`ms after it was sent.")
                return joiner.toString()
            }
        })
        return true
    }
    
    fun getUptimeMessage(timeDifference: Long): String {
        var difference = timeDifference
        var days: Int = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS).toInt()
        difference -= TimeUnit.MILLISECONDS.convert(days.toLong(), TimeUnit.DAYS)
        var hours: Int = TimeUnit.HOURS.convert(difference, TimeUnit.MILLISECONDS).toInt()
        difference -= TimeUnit.MILLISECONDS.convert(hours.toLong(), TimeUnit.HOURS)
        var minutes: Int = TimeUnit.MINUTES.convert(difference, TimeUnit.MILLISECONDS).toInt()
        difference -= TimeUnit.MILLISECONDS.convert(minutes.toLong(), TimeUnit.MINUTES)
        var seconds = TimeUnit.SECONDS.convert(difference, TimeUnit.MILLISECONDS)
        return "$days days, $hours hours, $minutes minutes, $seconds seconds"
    }

    override fun disable() { //TODO
        throw UnsupportedOperationException()
    }
}
