package coverstats.server

import com.google.gson.FieldNamingPolicy
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import coverstats.server.datastore.DataStore
import coverstats.server.datastore.memory.MemoryDataStore
import coverstats.server.exceptions.UnknownScmException
import coverstats.server.models.datastore.Repository
import coverstats.server.models.scm.ScmPermission
import coverstats.server.models.session.UserSession
import coverstats.server.scm.GitHubProvider
import coverstats.server.scm.ScmProvider
import coverstats.server.utils.generateToken
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
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
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.sessions.*
import io.ktor.util.AttributeKey
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
val httpsMode = config.getBoolean("security.httpsRedirect")

val providers: Map<String, ScmProvider> = config.getObject("scm").map { e ->
    val name = e.key
    val subConfig = config.getConfig("scm.$name")
    val type = subConfig.getString("type")
    if (type == "github") {
        name to GitHubProvider(
            name,
            subConfig.getString("basePath"),
            subConfig.getString("baseApiPath"),
            subConfig.getString("clientId"),
            subConfig.getString("clientSecret"),
            httpClient
        )
    } else {
        throw RuntimeException("Unknown SCM: $type")
    }
}.toMap()

val dataStore: DataStore = MemoryDataStore()

val RepoNameAttribute = AttributeKey<String>("RepoName")
val ScmProviderAttribute = AttributeKey<ScmProvider>("ScmProvider")

fun Application.module() {
    install(DefaultHeaders)
    install(XForwardedHeaderSupport)

    if (httpsMode) {
        install(HttpsRedirect) {
            permanentRedirect = true
        }
    }

    install(CallLogging) {
        level = Level.INFO
        mdc("ip") { call -> call.request.origin.remoteHost }
        mdc("session") { call -> call.request.cookies["SESSION"] }
    }
    install(StatusPages) {
        exception<UnknownScmException> {
            call.respond(HttpStatusCode.NotFound)
        }
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(javaClass.classLoader, "templates")
    }
    install(Sessions) {
        // TODO: Switch to another session storage
        cookie<UserSession>("SESSION", SessionStorageMemory()) {
            cookie.path = "/"
            cookie.secure = httpsMode
        }
    }
    install(Authentication) {
        oauth {
            providerLookup = { providers[parameters["scm"]]?.oAuthServerSettings ?: throw UnknownScmException() }
            client = httpClient
            urlProvider = { settings -> redirectUrl(settings) }
            skipWhen { call ->
                val session = call.sessions.get<UserSession>()
                session != null && session.scm == call.parameters["scm"]
            }
        }
    }

    routing {
        get("/") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                call.respondRedirect("/repos/${session.scm}")
            } else {
                call.respond(FreeMarkerContent("index.ftl", null))
            }
        }

        authenticate {
            get("/repos/{scm}") {
                val session = call.sessions.get<UserSession>()!!
                call.respond(FreeMarkerContent("repos.ftl", mapOf("session" to session)))
            }

            route("/repos/{scm}/{org}/{name}") {
                intercept(ApplicationCallPipeline.Call) {
                    val session = call.sessions.get<UserSession>()!!
                    val fullRepoName = call.parameters["org"]!! + "/" + call.parameters["name"]!!

                    call.attributes.put(RepoNameAttribute, fullRepoName)
                    call.attributes.put(ScmProviderAttribute, providers[call.parameters["scm"]]!!)

                    // Permission check
                    if (!session.repositories.contains(fullRepoName)) {
                        call.respond(HttpStatusCode.NotFound)
                        return@intercept finish()
                    }
                }

                get {
                    val session = call.sessions.get<UserSession>()!!
                    val fullRepoName = call.attributes[RepoNameAttribute]
                    val scmProvider = call.attributes[ScmProviderAttribute]
                    val repoPermission = session.repositories.getValue(fullRepoName)

                    var repo = dataStore.getRepositoryByName(scmProvider.name, fullRepoName)
                    if (repo == null) {
                        repo = Repository(scmProvider.name, fullRepoName, generateToken())
                        dataStore.saveRepository(repo)
                    }

                    val commits = scmProvider.getCommits(session.token, fullRepoName)
                    call.respond(
                        FreeMarkerContent(
                            "repo.ftl",
                            mapOf(
                                "repo" to repo,
                                "isAdmin" to (repoPermission == ScmPermission.ADMIN),
                                "commits" to commits
                            )
                        )
                    )
                }
            }

            get("/oauth/{scm}") {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                val scm = providers[call.parameters["scm"]]
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
    }
}

private fun ApplicationCall.redirectUrl(oAuthServerSettings: OAuthServerSettings): String {
    val hostPort = request.host() + request.port().let { port -> if (port == 80) "" else ":$port" }
    val path = request.path() + request.queryString().let { qs -> if (qs.isNotEmpty()) "?$qs" else "" }
    val protocol = request.origin.scheme
    return "$protocol://$hostPort/oauth/${oAuthServerSettings.name}?return=$path"
}