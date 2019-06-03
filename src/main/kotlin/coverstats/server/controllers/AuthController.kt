package coverstats.server.controllers

import coverstats.server.scm.ScmProvider
import io.ktor.application.call
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.authentication
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set

fun Route.auth(scmProviders: Map<String, ScmProvider>) {

    get("/oauth/{scm}") {
        val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
        val scm = scmProviders[call.parameters["scm"]]
        if (principal != null && scm != null) {
            val userSession = scm.processOAuth(principal, this)
            call.sessions.set(userSession)

            val returnPath = call.parameters["return"]
            if (returnPath != null &&
                // Prevent specifying full-URL for return path (possible attack to redirect off-site?)
                returnPath.startsWith("/")
            ) {
                call.respondRedirect(returnPath)
            } else {
                call.respondRedirect("/repos/${userSession.scm}")
            }
        } else {
            call.respondText("Login failed")
        }
    }

}
