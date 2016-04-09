package kotbot

import kotbot.modules.CommandPermissionLevels
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Database

/**
 * SQL database built on JetBrain's Exposed SQL framework for Kotlin
 */
class DataBase {
    companion object {
        val db = Database.connect("jdbc:h2:./database", driver = "org.h2.Driver")
        
        fun init() {
            db.transaction { 
                create(Users)
                create(Blacklist)
            }
        }

        /**
         * Gets the permission level for a given user id.
         */
        fun getUserPermissions(userId: String): CommandPermissionLevels {
            var result: CommandPermissionLevels? = null
            
            if (KotBot.CONFIG.OWNER == userId) {
                return CommandPermissionLevels.OWNER //Don't want to store this in the database in case the owner gets changed
            }
            
            db.transaction {
                val user = User.all().find { it.uid == userId }
                if (user != null) {
                    result = CommandPermissionLevels.valueOf(user.permissions)
                } else {
                    User.new { 
                        uid = userId
                        permissions = CommandPermissionLevels.DEFAULT.name
                    }
                    result = CommandPermissionLevels.DEFAULT
                }
            }
            return result ?: CommandPermissionLevels.NONE
        }

        /**
         * Checks if an id is in the blacklist. Returns true if blacklisted, false if otherwise.
         */
        fun checkBlacklist(id: String): Boolean {
            var returnVal: Boolean? = null
            db.transaction { 
                val entry = BlacklistEntry.all().find { it.uid == id }
                if (entry == null)
                   returnVal = false
                else 
                    returnVal = true
            }
            return returnVal ?: false
        }

        /**
         * Toggles whether or not this id is in the blacklist. Returns true if it is now blacklisted, false if otherwise.
         */
        fun toggleBlacklist(id: String): Boolean {
            var returnVal: Boolean? = null
            db.transaction {
                val entry = BlacklistEntry.all().find { it.uid == id }
                if (entry == null) {
                    BlacklistEntry.new { 
                        uid = id
                    }
                    returnVal = true
                } else {
                    entry.delete()
                    returnVal = false
                }
            }
            return returnVal ?: true
        }
    }
}

object Blacklist : IntIdTable() {
    val uid = text("uid")
}

class BlacklistEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BlacklistEntry>(Blacklist)

    var uid by Blacklist.uid
}

object Users : IntIdTable() {
    val uid = text("uid")
    val permissions = text("permissions")
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var uid by Users.uid
    var permissions by Users.permissions
}
