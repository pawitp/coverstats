package coverstats.server.controllers

import coverstats.server.models.session.UserSession
import io.ktor.application.call
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.sessions.get
import io.ktor.sessions.sessions

fun Route.home() {
    get("/") {
        val session = call.sessions.get<UserSession>()
        if (session != null) {
            call.respondRedirect("/repos/${session.scm}")
        } else {
            call.respond(FreeMarkerContent("index.ftl", null))
        }
    }
}
