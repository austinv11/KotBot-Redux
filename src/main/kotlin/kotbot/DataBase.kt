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
            }
        }
        
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
    }
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
