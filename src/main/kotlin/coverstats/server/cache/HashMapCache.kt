package coverstats.server.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * Cache backed by CHM for testing
 */
class HashMapCache : Cache {

    private val cache = ConcurrentHashMap<String, ByteArray>()

    override suspend fun put(name: String, content: ByteArray, expirationMs: Int?) {
        cache[name] = content
    }

    override suspend fun get(name: String): ByteArray? {
        return cache[name]
    }

    override suspend fun remove(name: String) {
        cache.remove(name)
    }

}