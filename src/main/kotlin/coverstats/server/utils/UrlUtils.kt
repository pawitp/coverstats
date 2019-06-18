package coverstats.server.utils

import java.net.URL

fun generateUrl(scheme: String, host: String, port: Int, path: String): String {
    val uploadUrl = URL(scheme, host, port, path)

    // Don't specify port if it is the default port
    return if (uploadUrl.port == uploadUrl.defaultPort) {
        URL(scheme, host, path).toString()
    } else {
        uploadUrl.toString()
    }
}
