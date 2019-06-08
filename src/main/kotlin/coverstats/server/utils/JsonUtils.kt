package coverstats.server.utils

import com.google.gson.Gson
import kotlin.reflect.KClass

private val gson = Gson()

fun <T> T.serializeJson(): String {
    return gson.toJson(this)
}

fun <T : Any> String.deserializeJson(classOf: KClass<T>): T {
    return gson.fromJson(this, classOf.java)
}