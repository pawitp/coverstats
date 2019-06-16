package coverstats.server.cache

import coverstats.server.utils.deserializeJson
import coverstats.server.utils.serializeJson
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8

interface Cache {

    suspend fun put(name: String, content: ByteArray, expirationMs: Int? = null)
    suspend fun get(name: String): ByteArray?
    suspend fun remove(name: String)

}

fun createCacheFromUri(uri: String): Cache {
    val parsedUri = URI(uri)

    return when (parsedUri.scheme) {
        "chm" -> HashMapCache()
        "memcached" -> MemcachedCache(parsedUri)
        else -> throw Exception("Unknown cache uri: $uri")
    }
}

// Try to get from cache, otherwise fetch from original source.
// If value is null, then it will not be cached.
suspend inline fun <reified T : Any> Cache.tryGetFromCache(name: String, fetch: () -> T?): T? {
    val cachedValue = this.get(name)
    return if (cachedValue != null) {
        cachedValue.toString(UTF_8).deserializeJson()
    } else {
        val value = fetch()
        if (value != null) this.put(name, value.serializeJson().toByteArray(UTF_8))
        value
    }
}