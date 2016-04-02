package kotbot

import org.slf4j.impl.LoggerAdapter

/**
 * Kotlin class representation of the config file.
 */
class Config {
    /**
     * Logger level.
     */
    var LOG_LEVEL = "DEBUG"
    /**
     * If true, every bot restart clears the logs
     */
    var CLEAR_LOGS_ON_STARTUP = true
    /**
     * The names of loggers to filter
     */
    var FILTERED_LOGGERS = arrayOf("org.eclipse.jetty")
    
    /**
     * Saves the current version of the config into a file
     */
    fun save() {
        KotBot.CONFIG_FILE.writeText(KotBot.GSON.toJson(this))
    }

    /**
     * This loads the config so that it applies to specific classes (i.e. LoggerAdapter options)
     */
    fun load() {
        LoggerAdapter.LOG_LEVEL = LoggerAdapter.Level.valueOf(LOG_LEVEL)
        LoggerAdapter.CLEAR_LOG_FILES_ON_STARTUP = CLEAR_LOGS_ON_STARTUP
        LoggerAdapter.FILTERED_LOGGERS = FILTERED_LOGGERS
    }
}
