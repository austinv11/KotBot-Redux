package kotbot.modules.impl

import kotbot.modules.BaseModule
import kotbot.modules.Command
import kotbot.modules.CommandException
import kotbot.modules.Parameter
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
                        message.channel.sendMessage("Times up! This is a reminder for \"$description\"")
                    }
                }, unit.toMillis(time))
                
                return "Scheduled reminder for $time ${unit.toString().toLowerCase()} from now."
            }
        })
        
        return true
    }

    override fun disableModule() {
        timer.cancel()
    }
}
