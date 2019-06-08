package coverstats.server.session

import coverstats.server.cache.Cache
import io.ktor.sessions.SessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.readRemaining
import kotlinx.coroutines.io.writer
import kotlinx.io.core.readBytes

class CacheSessionStorage(private val cache: Cache) : SessionStorage {

    override suspend fun <R> read(id: String, consumer: suspend (ByteReadChannel) -> R): R {
        return cache.get("s:$id")?.let { data -> consumer(ByteReadChannel(data)) }
            ?: throw NoSuchElementException("Session $id not found")
    }

    override suspend fun write(id: String, provider: suspend (ByteWriteChannel) -> Unit) {
        coroutineScope {
            val channel = writer(Dispatchers.Unconfined, autoFlush = true) {
                provider(channel)
            }.channel

            cache.put("s:$id", channel.readRemaining().readBytes())
        }
    }

    override suspend fun invalidate(id: String) {
        cache.remove("s:$id")
    }

}