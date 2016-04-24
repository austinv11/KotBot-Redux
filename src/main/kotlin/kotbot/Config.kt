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
     * If true, every bot restart clears the logs.
     */
    var CLEAR_LOGS_ON_STARTUP = true
    /**
     * The names of loggers to filter.
     */
    var FILTERED_LOGGERS = arrayOf("org.eclipse.jetty")
    /**
     * The prefix for commands (Note, an @mention in the beginning of the message is always a valid prefix).
     */
    var PREFIX = "~"
    /**
     * The user id of the owner.
     */
    var OWNER = ""
    /**
     * The bot oauth invite link.
     */
    var INVITE_LINK = ""
    /**
     * The string similarity algorithm to use, currently available are: NORMALIZED_LEVENSHTEIN, JARO_WINKLER, 
     * METRIC_LONGEST_COMMON_SUBSEQUENCE, N_GRAM, COSINE_SIMILARITY, JACCARD_INDEX and SORENSEN_DICE_COEFFICIENT.
     * More info available here: https://github.com/tdebatty/java-string-similarity#summary
     */
    var MESSAGE_MONITOR_MODE = "NORMALIZED_LEVENSHTEIN"
    /**
     * The maximum difference found in two strings.
     */
    var MESSAGE_SIMILARITY_CONSTANT = .5
    /**
     * The messages the bot should monitor.
     */
    var MESSAGE_MONITORS = arrayOf<MessageMonitorConfig>()
    
    /**
     * Saves the current version of the config into a file.
     */
    fun save() {
        KotBot.CONFIG_FILE.writeText(KotBot.GSON.toJson(this))
    }

    /**
     * This loads the config so that it applies to specific classes (i.e. LoggerAdapter options).
     */
    fun load() {
        LoggerAdapter.LOG_LEVEL = LoggerAdapter.Level.valueOf(LOG_LEVEL)
        LoggerAdapter.CLEAR_LOG_FILES_ON_STARTUP = CLEAR_LOGS_ON_STARTUP
        LoggerAdapter.FILTERED_LOGGERS = FILTERED_LOGGERS
    }

    /**
     * KEY_PHRASES = Phrases which activate the response.
     * RESPONSE = The response to give.
     * CHANNELS = The channel id black/whitelist for this monitor.
     * WHITELIST = if true, the CHANNELS list acts as a whitelist, blacklist if otherwise.
     */
    data class MessageMonitorConfig(val KEY_PHRASES: Array<String>, val RESPONSE: String, 
                                    val CHANNELS: Array<String>, val WHITELIST: Boolean = true)
}
