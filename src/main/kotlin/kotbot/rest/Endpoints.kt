package kotbot.rest

/**
 * Represents the endpoints for KotBot's REST api.
 */
class Endpoints {
    
    companion object {
        /**
         * Base api url
         */
        const val BASE = "http://localhost:4567/kotbot/api/"
        /**
         * The bot's api
         */
        const val BOT = BASE + "bot/"
        /**
         * The ping api
         */
        const val PING = BASE + "ping/"
    }
}
