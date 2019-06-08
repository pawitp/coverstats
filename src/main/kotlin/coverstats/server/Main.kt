package coverstats.server

import com.google.gson.FieldNamingPolicy
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import coverstats.server.controllers.auth
import coverstats.server.controllers.home
import coverstats.server.controllers.repos
import coverstats.server.controllers.upload
import coverstats.server.coverage.processors.CoverageProcessor
import coverstats.server.coverage.processors.JacocoProcessor
import coverstats.server.datastore.DataStore
import coverstats.server.datastore.memory.MemoryDataStore
import coverstats.server.exceptions.UnknownScmException
import coverstats.server.models.datastore.Repository
import coverstats.server.models.session.UserSession
import coverstats.server.scm.GitHubProvider
import coverstats.server.scm.ScmProvider
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.OAuthServerSettings
import io.ktor.auth.authenticate
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.*
import io.ktor.freemarker.FreeMarker
import io.ktor.http.HttpStatusCode
import io.ktor.request.host
import io.ktor.request.path
import io.ktor.request.port
import io.ktor.request.queryString
import io.ktor.response.respond
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
            setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        }
    }
}

private val config: Config = ConfigFactory.load()
private val httpsMode = config.getBoolean("security.httpsRedirect")

private val scmProviders: Map<String, ScmProvider> = config.getObject("scm").map { e ->
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
            subConfig.getString("appId"),
            subConfig.getString("privateKey"),
            httpClient
        )
    } else {
        throw RuntimeException("Unknown SCM: $type")
    }
}.toMap()

private val coverageProcessors: List<CoverageProcessor> = listOf(JacocoProcessor)

private val dataStore: DataStore = MemoryDataStore()

val RepoAttribute = AttributeKey<Repository>("Repo")
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
            providerLookup = { scmProviders[parameters["scm"]]?.oAuthServerSettings ?: throw UnknownScmException() }
            client = httpClient
            urlProvider = { settings -> redirectUrl(settings) }
            skipWhen { call ->
                val session = call.sessions.get<UserSession>()
                session != null && session.scm == call.parameters["scm"]
            }
        }
    }

    routing {
        home()
        upload(dataStore, scmProviders, coverageProcessors)

        authenticate {
            repos(dataStore, scmProviders)
            auth(scmProviders)
        }
    }
}

private fun ApplicationCall.redirectUrl(oAuthServerSettings: OAuthServerSettings): String {
    val hostPort = request.host() + request.port().let { port -> if (port == 80) "" else ":$port" }
    val path = request.path() + request.queryString().let { qs -> if (qs.isNotEmpty()) "?$qs" else "" }
    val protocol = request.origin.scheme
    return "$protocol://$hostPort/oauth/${oAuthServerSettings.name}?return=$path"
}