package kotbot.utils

import com.google.gson.Gson
import com.google.gson.JsonElement
import kotbot.KotBot
import java.io.Reader
import kotlin.reflect.jvm.jvmName

/**
 * Kotin-ifies Gson
 */

/**
 * Allows for reified type parameters instead of classes.
 */
inline fun <reified T> Gson.fromJson(json: String): T {
    return this.fromJson(json, Class.forName(T::class.jvmName) as Class<T>)
}

/**
 * Allows for reified type parameters instead of classes.
 */
inline fun <reified T> Gson.fromJson(json: Reader): T {
    return this.fromJson(json, Class.forName(T::class.jvmName) as Class<T>)
}

/**
 * Allows for reified type parameters instead of classes.
 */
inline fun <reified T> Gson.fromJson(json: JsonElement): T {
    return this.fromJson(json, Class.forName(T::class.jvmName) as Class<T>)
}

/**
 * Shortcut for Gson.toJson() on any object.
 */
fun Any.toJson(): String {
    return KotBot.GSON.toJson(this)
}
