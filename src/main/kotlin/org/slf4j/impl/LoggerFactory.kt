package org.slf4j.impl

import org.slf4j.ILoggerFactory

/**
 * Simple class to get Logger instances
 */
class LoggerFactory : ILoggerFactory {
    
    override fun getLogger(name: String?) = LoggerAdapter(name ?: "Logger")
}
