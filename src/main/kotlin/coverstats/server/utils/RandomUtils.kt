package coverstats.server.utils

import java.security.SecureRandom
import java.util.*

private val random = SecureRandom()
private val encoder = Base64.getUrlEncoder().withoutPadding()

fun generateToken(): String {
    val buffer = ByteArray(20)
    random.nextBytes(buffer)
    return encoder.encodeToString(buffer)
}
