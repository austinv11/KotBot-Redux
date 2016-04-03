package kotbot.modules.impl

import kotbot.modules.BaseModule
import kotbot.modules.Command
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IMessage

/**
 * Represents functions which adds emojis to chat.
 */
class EmoticonModule : BaseModule() {
    
    override fun enable(client: IDiscordClient?): Boolean {
        registerCommands(object: Command("shrug", arrayOf(), "¯\\_(ツ)_/¯", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "¯\\_(ツ)_/¯"
            }
        }, object: Command("lenny", arrayOf(), "( ͡° ͜ʖ ͡°)", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "( ͡° ͜ʖ ͡°)"
            }

        }, object: Command("ameno", arrayOf(), "༼ つ ◕_◕ ༽つ", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "༼ つ ◕_◕ ༽つ"
            }
        }, object: Command("fite", arrayOf(), "(ง ͠° ͟ل͜ ͡°)ง", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "(ง ͠° ͟ل͜ ͡°)ง"
            }
        }, object: Command("disapprove", arrayOf(), "ಠ_ಠ", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "ಠ_ಠ"
            }
        }, object: Command("heyo", arrayOf(), "(☞ﾟヮﾟ)☞", "") {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "(☞ﾟヮﾟ)☞"
            }
        })
        return true
    }

    override fun disable() {
        throw UnsupportedOperationException()
    }
}
