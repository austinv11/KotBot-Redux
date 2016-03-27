package kotbot.rest

import kotbot.KotBot
import kotbot.utils.toJson
import spark.Request
import spark.Response
import spark.Spark
import spark.Spark.*
import java.security.SecureRandom
import kotlin.reflect.KClass

/**
 * Represents the web server created by this bot.
 */
class Server {
    
    companion object {

        private final val RANDOM = SecureRandom()
        
        /**
         * These are tokens which are authorized to do private functions.
         */
        private var AUTH_TOKENS = arrayOf(KotBot.CLIENT.token)

        /**
         * Generates a 20 byte long random auth token
         */
        fun generateAuthToken(): String {
            val builder = StringBuilder()
            
            var bytes = ByteArray(20)
            RANDOM.nextBytes(bytes)
            bytes.forEach { builder.append(it) }

            val token = builder.toString()
            AUTH_TOKENS += token
            
            return token
        }

        /**
         * Checks if a token is valid.
         */
        fun isTokenValid(token: String): Boolean {
            return AUTH_TOKENS.contains(token)
        }
        
        /**
         * Starts up the webserver.
         */
        fun startup(ip: String = "0.0.0.0", port: Int = 4567) {
            KotBot.LOGGER.info("Starting server on $ip:$port...")
            
            Spark.ipAddress(ip)
            Spark.port(port)
        }

        /**
         * Stops the server.
         */
        fun stop() {
            Spark.stop()
        }
        
        /**
         * Allows for rest requests to be added via array syntax, i.e. Server["/", RequestType.GET] = { request: Request, response: Response -> "Hello World" }
         */
        operator fun set(route: String, type: RequestType, callback: (request: Request, response: Response) -> Any?) {
            when(type) {
                RequestType.CONNECT -> connect(route, callback, { value: Any -> value.toJson() })
                RequestType.DELETE -> delete(route, callback, { value: Any -> value.toJson() })
                RequestType.GET -> get(route, callback, { value: Any -> value.toJson() })
                RequestType.HEAD -> head(route, callback, { value: Any -> value.toJson() })
                RequestType.OPTIONS -> options(route, callback, { value: Any -> value.toJson() })
                RequestType.PATCH -> patch(route, callback, { value: Any -> value.toJson() })
                RequestType.POST -> post(route, callback, { value: Any -> value.toJson() })
                RequestType.PUT -> put(route, callback, { value: Any -> value.toJson() })
                RequestType.TRACE -> trace(route, callback, { value: Any -> value.toJson() })
            }
        }

        /**
         * Allows for rest request filters to be added via array syntax, i.e. Server["/", FilterType.BEFORE] = { request: Request, response: Response -> FilterResponse(false) }
         */
        operator fun set(route: String?, type: FilterType, callback: (request: Request, response: Response) -> FilterResponse) {
            when(type) {
                FilterType.AFTER -> { 
                    if (route == null) 
                        after({ request: Request, response: Response -> 
                            val filterResponse = callback(request, response)
                            if (filterResponse.halt) {
                                halt(filterResponse.responseCode, filterResponse.message)
                            }
                        })
                    else
                        after(route,{ request: Request, response: Response ->
                            val filterResponse = callback(request, response)
                            if (filterResponse.halt) {
                                halt(filterResponse.responseCode, filterResponse.message)
                            }
                        })
                }
                FilterType.BEFORE -> {
                    if (route == null)
                        before({ request: Request, response: Response ->
                            val filterResponse = callback(request, response)
                            if (filterResponse.halt) {
                                halt(filterResponse.responseCode, filterResponse.message)
                            }
                        })
                    else
                        before(route,{ request: Request, response: Response ->
                            val filterResponse = callback(request, response)
                            if (filterResponse.halt) {
                                halt(filterResponse.responseCode, filterResponse.message)
                            }
                        })
                }
            }
        }

        /**
         * See above ^
         */
        operator fun set(type: FilterType, callback: (request: Request, response: Response) -> FilterResponse) {
            set(null, type, callback)
        }

        /**
         * Allows for mapping exception handlers via array syntax, i.e. Server[Exception::class] = { exception: Exception, request: Request, response: Response -> fooBar() }
         */
        operator fun set(e: KClass<out Exception>, callback: (exception: Exception, request: Request, response: Response) -> Unit) {
            exception(e.java, callback)
        }

        /**
         * Adds a static file based on the relative classpath directory via addition syntax, i.e. Server + "/public"
         */
        operator fun plus(location: String) {
            staticFileLocation(location)
        }
    }
}
