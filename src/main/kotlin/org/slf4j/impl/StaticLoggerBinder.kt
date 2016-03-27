@file:JvmName("StaticLoggerBinder")//Don't want the "Kt" suffix on this class

package org.slf4j.impl

import org.slf4j.spi.LoggerFactoryBinder

/**
 * This is a dummy StaticLoggerBinder to get slf4j to recognize my custom LoggerAdapter
 */
public class StaticLoggerBinder : LoggerFactoryBinder {
    
    companion object {
        const val REQUESTED_API_VERSION: String = "1.6.99" //Required field
        
        final val BINDER = StaticLoggerBinder() //Binder instance
        
        @JvmStatic
        fun getSingleton() : StaticLoggerBinder = BINDER//Required function
    }
    
    override fun getLoggerFactory() = LoggerFactory()

    override fun getLoggerFactoryClassStr() = LoggerFactory::class.java.name;
}
