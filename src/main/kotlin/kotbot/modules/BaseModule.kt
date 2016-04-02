package kotbot.modules

import kotbot.KotBot
import sx.blah.discord.Discord4J
import sx.blah.discord.modules.IModule

/**
 * Module which implements basic features constant in all modules
 */
abstract class BaseModule : IModule {
    
    override fun getName(): String? {
        return "KotBot ${camelcaseToSpaced(this.javaClass.simpleName.removeSuffix("Kt"))}"
    }

    override fun getVersion(): String? {
        return KotBot.VERSION
    }

    override fun getMinimumDiscord4JVersion(): String? {
        return Discord4J.VERSION
    }

    override fun getAuthor(): String? {
        return KotBot.AUTHOR
    }
    
    //Turns FooBar -> Foo Bar
    private fun camelcaseToSpaced(name: String): String {
        val stringBuilder = StringBuilder()
        
        for (letter in name.asIterable()) {
            if (stringBuilder.isEmpty() && letter.isUpperCase()) {
                stringBuilder.append(" ")
            }
            stringBuilder.append(letter)
        }
        
        return stringBuilder.toString()
    }
}
