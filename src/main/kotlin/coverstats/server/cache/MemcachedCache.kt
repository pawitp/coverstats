package coverstats.server.cache

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import mu.KotlinLogging
import net.spy.memcached.ConnectionFactoryBuilder
import net.spy.memcached.MemcachedClient
import net.spy.memcached.auth.AuthDescriptor
import net.spy.memcached.auth.PlainCallbackHandler
import net.spy.memcached.internal.GetFuture
import net.spy.memcached.internal.OperationFuture
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val logger = KotlinLogging.logger {}
private const val DEFAULT_EXPIRATION = 60 * 60 * 24 * 30 // 30 days, max for memcached

/**
 * Cache backed by memcached
 */
class MemcachedCache(uri: URI) : Cache {

    private val connectionFactory = ConnectionFactoryBuilder().apply {
        if (uri.userInfo != null) {
            val (username, password) = uri.userInfo.split(':')
            setAuthDescriptor(AuthDescriptor(arrayOf("PLAIN"), PlainCallbackHandler(username, password)))
        }
        setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
    }.build()

    private val cache = MemcachedClient(
        connectionFactory,
        listOf(InetSocketAddress(uri.host, uri.port))
    )

    override suspend fun put(name: String, content: ByteArray, expirationMs: Int?) {
        logger.debug { "memcached put name=$name value=${content.toString(UTF_8)}"}
        val result = cache.set(name, expirationMs ?: DEFAULT_EXPIRATION, content).await()
        if (!result) {
            logger.warn { "Failed to set $name to cache" }
        }
    }

    override suspend fun get(name: String): ByteArray? {
        val result = cache.asyncGet(name).await() as ByteArray?
        logger.debug { "memcached get name=$name value=${result?.toString(UTF_8)}"}
        return result
    }

    override suspend fun remove(name: String) {
        val result = cache.delete(name).await()
        if (!result) {
            logger.info { "Failed to delete $name from cache" }
        }
    }

}

// Clone of Kotlin's CompletionStage<T>.await()
private suspend fun GetFuture<Any>.await(): Any? {
    if (isDone) {
        @Suppress("BlockingMethodInNonBlockingContext")
        return get()
    }
    // slow path -- suspend
    return suspendCancellableCoroutine { cont: CancellableContinuation<Any> ->
        addListener {
            try {
                cont.resume(it.get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
        cont.invokeOnCancellation {
            cancel(true)
        }
    }
}

private suspend fun OperationFuture<Boolean>.await(): Boolean {
    if (isDone) {
        @Suppress("BlockingMethodInNonBlockingContext")
        return get()
    }
    // slow path -- suspend
    return suspendCancellableCoroutine { cont: CancellableContinuation<Boolean> ->
        addListener {
            try {
                cont.resume(it.get() as Boolean)
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
        cont.invokeOnCancellation {
            @Suppress("DEPRECATION")
            cancel(true)
        }
    }
}