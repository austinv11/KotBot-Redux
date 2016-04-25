package kotbot.modules.impl

import info.debatty.java.stringsimilarity.*
import info.debatty.java.stringsimilarity.interfaces.NormalizedStringDistance
import kotbot.KotBot
import kotbot.modules.BaseModule
import kotbot.modules.Command
import kotbot.modules.CommandException
import kotbot.modules.Parameter
import kotbot.utils.bufferedRequest
import sx.blah.discord.api.IListener
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This represents bot functions which relate to events (not necessarily Discord's) monitoring.
 */
class MonitorModule : BaseModule() {

    val timer = Timer("Monitor Timer", true)
    
    companion object {
        final val DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS
    }
    
    override fun enableModule(): Boolean {
        registerCommands(object: Command("timer", arrayOf("alert", "alarm", "reminder"), "Sets a reminder for the set time.",
                arrayOf(Parameter("description"), Parameter("time"), Parameter("unit", true))) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                if (args.size < 2)
                    throw CommandException("Expecting 2 arguments.")
                
                val description = args[0] as String
                val time = args[1].toString().toLong()
                val unit: TimeUnit
                if (args.size > 2) {
                    var unitString = (args[2] as String).toUpperCase()
                    if (!unitString.endsWith("S"))
                        unitString += "S"
                    unit = TimeUnit.valueOf(unitString)
                } else
                    unit = DEFAULT_TIME_UNIT
                
                timer.schedule(object: TimerTask() {
                    override fun run() {
                        bufferedRequest { message.channel.sendMessage("Times up! This is a reminder for \"$description\"") }
                    }
                }, unit.toMillis(time))
                
                return "Scheduled reminder for $time ${unit.toString().toLowerCase()} from now."
            }
        })
        
        initMonitors()
        
        return true
    }

    override fun disableModule() {
        timer.cancel()
    }
    
    private fun initMonitors() {
        //FIXME: Use BaseModule#registerListener() 
        client.dispatcher.registerListener(IListener<MessageReceivedEvent> { //Message responder monitor
            for (config in KotBot.CONFIG.MESSAGE_MONITORS) {
                if ((config.WHITELIST && config.CHANNELS.contains(it.message.channel.id)) 
                    || (!config.WHITELIST && !config.CHANNELS.contains(it.message.channel.id))) {
                    if (StringSimilarityFinder.findImplementation(KotBot.CONFIG.MESSAGE_MONITOR_MODE)
                            .containsSimilarString(it.message.content, config.KEY_PHRASES)) {
                        bufferedRequest {
                            it.message.reply(buildString {
                                appendln("**Based on your question, this seems like an appropriate answer:**\n")
                                appendln(config.RESPONSE + "\n")
                                appendln("*Note: This is an automated response. If you feel this is incorrect or you still " +
                                        "need additional assistance, please contact ${KotBot.OWNER_NAME}.*")
                            })
                        }
                        return@IListener
                    }
                }
            }
        })
    }
    
    enum class StringSimilarityFinder(val implementation: NormalizedStringDistance) {
        NORMALIZED_LEVENSHTEIN(NormalizedLevenshtein()), 
        JARO_WINKLER(JaroWinkler()), 
        METRIC_LONGEST_COMMON_SUBSEQUENCE(MetricLCS()), 
        N_GRAM(NGram()), 
        COSINE_SIMILARITY(Cosine()), 
        JACCARD_INDEX(Jaccard()), 
        SORENSEN_DICE_COEFFICIENT(SorensenDice()),
        NULL_IMPLEMENTATION(NormalizedStringDistance { string1, string2 -> return@NormalizedStringDistance 1.0 });

        fun containsSimilarString(input: String, possibleMatches: Array<String>): Boolean {
            for (possibleMatch in possibleMatches) {
                if (implementation.distance(input, possibleMatch) <= KotBot.CONFIG.MESSAGE_SIMILARITY_CONSTANT)
                    return true
            }
            
            return false
        }
        
        companion object {

            fun findImplementation(string: String): StringSimilarityFinder {
                try {
                    return StringSimilarityFinder.valueOf(string.replace('-', '_').replace(' ', '_').toUpperCase())
                } catch(e: Exception) {
                    KotBot.LOGGER.error("Unable to find string similarity implementation for '$string'. Using a null implementation (nothing is similar).")
                    return NULL_IMPLEMENTATION
                }
            }
        }
    }
}
