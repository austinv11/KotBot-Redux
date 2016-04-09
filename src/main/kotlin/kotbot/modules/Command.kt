package kotbot.modules

import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.Permissions
import java.util.*

/**
 * This is used to represent a command.
 */
abstract class Command(val name: String, val aliases: Array<String>, val description: String, val usage: Array<Parameter>,
                   val botPermissionLevel: CommandPermissionLevels = CommandPermissionLevels.DEFAULT,
                   val directMessages: Boolean = true, val channelMessages: Boolean = true,
                   val isHidden: Boolean = false, val async: Boolean = false,
                   val permissionsRequired: EnumSet<Permissions> = EnumSet.of(Permissions.SEND_MESSAGES)) {

    /**
     * This is called when the command is executed.
     */
    abstract fun execute(message: IMessage, args: List<Any>): String?
}

/**
 * Represents a command parameter. Allows for cleaner help messages.
 */
data class Parameter(val name: String, val optional: Boolean = false);
