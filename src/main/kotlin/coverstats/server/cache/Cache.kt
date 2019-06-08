package coverstats.server.cache

import java.net.URI

interface Cache {

    suspend fun put(name: String, content: ByteArray, expirationMs: Int? = null)
    suspend fun get(name: String): ByteArray?
    suspend fun remove(name: String)

}

fun createCachefromUri(uri: String): Cache {
    val parsedUri = URI(uri)

    return when (parsedUri.scheme) {
        "chm" -> HashMapCache()
        "memcached" -> MemcachedCache(parsedUri)
        else -> throw Exception("Unknown cache uri: $uri")
    }
}