package org.slf4j.impl

import org.slf4j.helpers.MarkerIgnoringBase
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime

/**
 * This is used to intercept log messages from Discord4J.
 */
class LoggerAdapter() : MarkerIgnoringBase() {
    
    /**
     * @param name: The name of the log
     */
    constructor(name: String) : this() {
        this.name = name;
        
        if (CLEAR_LOG_FILES_ON_STARTUP) {
            ERROR_LOG_FILE.delete()
            LOG_FILE.delete()
        }
    }

    companion object {
        /**
         * The logging level
         */
        var LOG_LEVEL: Level = Level.DEBUG
        /**
         * If true, the logger clears log files on startup
         */
        var CLEAR_LOG_FILES_ON_STARTUP: Boolean = true
        /**
         * File to log errors
         */
        val ERROR_LOG_FILE: File = File("./errors.log")
        /**
         * File to log the rest of the log
         */
        val LOG_FILE: File = File("./bot.log")
        /**
         * Ignores loggers containing these names
         */
        val filteredNames: Array<String> = arrayOf("org.eclipse.jetty") //Jetty can get really spammy
    }
    
    override fun warn(p0: String?) {
        log(Level.WARN, p0?: "")
    }

    override fun warn(p0: String?, p1: Any?) {
        log(Level.WARN, p0?: "", arrayOf(p1))
    }

    override fun warn(p0: String?, p1: Any?, p2: Any?) {
        log(Level.WARN, p0?: "", arrayOf(p1, p2))
    }

    override fun warn(p0: String?, vararg p1: Any?) {
        log(Level.WARN, p0?: "", p1)
    }

    override fun warn(p0: String?, p1: Throwable?) {
        log(Level.WARN, p0?: "", error = p1)
    }

    override fun info(p0: String?) {
        log(Level.INFO, p0?: "")
    }

    override fun info(p0: String?, p1: Any?) {
        log(Level.INFO, p0?: "", arrayOf(p1))
    }

    override fun info(p0: String?, p1: Any?, p2: Any?) {
        log(Level.INFO, p0?: "", arrayOf(p1, p2))
    }

    override fun info(p0: String?, vararg p1: Any?) {
        log(Level.INFO, p0?: "", p1)
    }

    override fun info(p0: String?, p1: Throwable?) {
        log(Level.INFO, p0?: "", error = p1)
    }

    override fun isErrorEnabled() = true

    override fun error(p0: String?) {
        log(Level.ERROR, p0?: "")
    }

    override fun error(p0: String?, p1: Any?) {
        log(Level.ERROR, p0?: "", arrayOf(p1))
    }

    override fun error(p0: String?, p1: Any?, p2: Any?) {
        log(Level.ERROR, p0?: "", arrayOf(p1, p2))
    }

    override fun error(p0: String?, vararg p1: Any?) {
        log(Level.ERROR, p0?: "", p1)
    }

    override fun error(p0: String?, p1: Throwable?) {
        log(Level.ERROR, p0?: "", error = p1)
    }

    override fun isDebugEnabled() = true

    override fun debug(p0: String?) {
        log(Level.DEBUG, p0?: "")
    }

    override fun debug(p0: String?, p1: Any?) {
        log(Level.DEBUG, p0?: "", arrayOf(p1))
    }

    override fun debug(p0: String?, p1: Any?, p2: Any?) {
        log(Level.DEBUG, p0?: "", arrayOf(p1, p2))
    }

    override fun debug(p0: String?, vararg p1: Any?) {
        log(Level.DEBUG, p0?: "", p1)
    }

    override fun debug(p0: String?, p1: Throwable?) {
        log(Level.DEBUG, p0?: "", error = p1)
    }

    override fun isInfoEnabled() = true

    override fun trace(p0: String?) {
        log(Level.TRACE, p0?: "")
    }

    override fun trace(p0: String?, p1: Any?) {
        log(Level.TRACE, p0?: "", arrayOf(p1))
    }

    override fun trace(p0: String?, p1: Any?, p2: Any?) {
        log(Level.TRACE, p0?: "", arrayOf(p1, p2))
    }

    override fun trace(p0: String?, vararg p1: Any?) {
        log(Level.TRACE, p0?: "", p1)
    }

    override fun trace(p0: String?, p1: Throwable?) {
        log(Level.TRACE, p0?: "", error = p1)
    }

    override fun isWarnEnabled() = true

    override fun isTraceEnabled() = true
    
    private fun log(level: Level, message: String, toReplace: Array<out Any?>? = null, error: Throwable? = null) {
        for (string in filteredNames) { //Ignore filtered loggers names
            if (name.contains(string))
                return;
        }
        
        if (LOG_LEVEL < level)
            return
        
        var logMessage = "[$level][${LocalDateTime.now().toString()}][${getName()}] ";
        
        logMessage += message;
        
        if (toReplace != null) {
            for (obj in toReplace) {
                logMessage = logMessage.replaceFirst("{}", obj.toString())
            }
        }
        
        if (level == Level.ERROR || level == Level.WARN) {
            System.err.println(logMessage)
            if (level == Level.ERROR) 
                ERROR_LOG_FILE.appendText("$logMessage\n")
        } else {
            System.out.println(logMessage)
        }
        LOG_FILE.appendText("$logMessage\n")
        
        if (error != null) {
            var writer = StringWriter()
            error.printStackTrace(PrintWriter(writer))
            for (str in writer.toString().lines())
                log(level, str)
        }
    }

    /**
     * Represents specific logging levels.
     */
    public enum class Level {
        ERROR, WARN, INFO, DEBUG, TRACE
    }
}
