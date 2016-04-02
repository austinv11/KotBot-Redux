package kotbot.modules

/**
 * Represents permissions levels required for a command to be executed.
 * Owner = the bot's owner
 * Admin = bot administrators
 * Default = Most other users
 * None = Users with perms removed
 */
enum class CommandPermissionLevels {
    OWNER, ADMIN, DEFAULT, NONE
}
