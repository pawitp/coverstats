package coverstats.server

import com.google.gson.FieldNamingPolicy
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import coverstats.server.models.github.GitHubInstallations
import coverstats.server.models.github.GitHubRepositories
import coverstats.server.models.github.GitHubUser
import coverstats.server.models.session.UserSession
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.features.origin
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.HttpStatusCode
import io.ktor.request.host
import io.ktor.request.path
import io.ktor.request.port
import io.ktor.request.queryString
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.sessions.*
import kotlinx.coroutines.async
import org.slf4j.event.Level

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

val httpClient = HttpClient {
    install(JsonFeature) {
        serializer = GsonSerializer {
            setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        }
    }
}


val config: Config = ConfigFactory.load()

fun Application.module() {
    install(DefaultHeaders)
    install(XForwardedHeaderSupport)
    install(CallLogging) {
        level = Level.INFO
        mdc("ip") { call -> call.request.origin.remoteHost }
        mdc("session") { call -> call.request.cookies["SESSION"] }
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(javaClass.classLoader, "templates")
    }
    install(Sessions) {
        // TODO: Switch to another session storage
        cookie<UserSession>("SESSION", SessionStorageMemory()) {
            cookie.path = "/"
        }
    }
    install(Authentication) {
        oauth {
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "github",
                    authorizeUrl = "https://github.com/login/oauth/authorize",
                    accessTokenUrl = "https://github.com/login/oauth/access_token",
                    clientId = config.getString("providers.github.clientId"),
                    clientSecret = config.getString("providers.github.clientSecret")
                )
            }
            client = httpClient
            urlProvider = { redirectUrl() }
            skipWhen { call -> call.sessions.get<UserSession>() != null }
        }
    }

    routing {
        get("/") {
            if (call.sessions.get<UserSession>() != null) {
                call.respondRedirect("/repos")
            } else {
                call.respond(FreeMarkerContent("index.ftl", null))
            }
        }

        authenticate {
            get("/repos/github") {
                val session = call.sessions.get<UserSession>()!!
                call.respond(FreeMarkerContent("repos.ftl", mapOf("session" to session)))
            }

            get("/repos/github/{org}/{name}") {
                val session = call.sessions.get<UserSession>()!!
                val fullRepoName = call.parameters["org"]!! + "/" + call.parameters["name"]!!
                if (!session.repositories.contains(fullRepoName)) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respondText("Repo Page: $fullRepoName")
                }
            }

            get("/oauth/github") {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                if (principal != null) {
                    val accessToken = principal.accessToken

                    // Fetch username
                    val userPromise = async {
                        httpClient.get<GitHubUser>("https://api.github.com/user") {
                            header("Authorization", "Bearer $accessToken")
                            header("Accept", "application/vnd.github.machine-man-preview+json")
                        }
                    }

                    // Fetch repos
                    val installations =
                        httpClient.get<GitHubInstallations>("https://api.github.com/user/installations") {
                            header("Authorization", "Bearer $accessToken")
                            header("Accept", "application/vnd.github.machine-man-preview+json")
                        }

                    val repositoryPromises = installations.installations.map {
                        async {
                            httpClient.get<GitHubRepositories>("https://api.github.com/user/installations/${it.id}/repositories") {
                                header("Authorization", "Bearer $accessToken")
                                header("Accept", "application/vnd.github.machine-man-preview+json")
                            }
                        }
                    }

                    val repositories = repositoryPromises.flatMap { it.await().repositories }.map { it.fullName }

                    val user = userPromise.await()

                    call.sessions.set(UserSession(principal.accessToken, user.login, user.name, repositories))

                    val returnPath = call.parameters["return"]
                    if (returnPath != null &&
                        // Prevent specifying full-URL for return path (possible attack to redirect off-site?)
                        returnPath.startsWith("/")
                    ) {
                        call.respondRedirect(returnPath)
                    } else {
                        call.respondRedirect("/repos/github")
                    }
                } else {
                    call.respondText("Login failed")
                }
            }
        }
    }
}

private fun ApplicationCall.redirectUrl(): String {
    val hostPort = request.host() + request.port().let { port -> if (port == 80) "" else ":$port" }
    val path = request.path() + request.queryString().let { qs -> if (qs.isNotEmpty()) "?$qs" else "" }
    val protocol = request.origin.scheme
    return "$protocol://$hostPort/oauth/github?return=$path"
}