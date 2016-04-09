package kotbot.modules.impl

import kotbot.modules.BaseModule
import kotbot.modules.Command
import sx.blah.discord.handle.obj.IMessage

/**
 * Represents functions which adds emojis to chat.
 */
class EmoticonModule : BaseModule() {
    
    override fun enableModule(): Boolean {
        registerCommands(object: Command("shrug", arrayOf(), "¯\\_(ツ)_/¯", arrayOf()) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "¯\\_(ツ)_/¯"
            }
        }, object: Command("lenny", arrayOf(), "( ͡° ͜ʖ ͡°)", arrayOf()) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "( ͡° ͜ʖ ͡°)"
            }

        }, object: Command("ameno", arrayOf(), "༼ つ ◕_◕ ༽つ", arrayOf()) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "༼ つ ◕_◕ ༽つ"
            }
        }, object: Command("fite", arrayOf(), "(ง ͠° ͟ل͜ ͡°)ง", arrayOf()) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "(ง ͠° ͟ل͜ ͡°)ง"
            }
        }, object: Command("disapprove", arrayOf(), "ಠ_ಠ", arrayOf()) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "ಠ_ಠ"
            }
        }, object: Command("heyo", arrayOf(), "(☞ﾟヮﾟ)☞", arrayOf()) {
            override fun execute(message: IMessage, args: List<Any>): String? {
                return "(☞ﾟヮﾟ)☞"
            }
        })
        return true
    }

    override fun disableModule() { //TODO
        throw UnsupportedOperationException()
    }
}
