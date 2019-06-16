package coverstats.server.controllers

import coverstats.server.RepoAttribute
import coverstats.server.ScmProviderAttribute
import coverstats.server.coverage.CoverageService
import coverstats.server.datastore.DataStore
import coverstats.server.models.coverage.CoverageStatus
import coverstats.server.models.datastore.Repository
import coverstats.server.models.scm.ScmPermission
import coverstats.server.models.session.UserSession
import coverstats.server.scm.ScmProvider
import coverstats.server.utils.generateToken
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

fun Route.repos(dataStore: DataStore, coverageService: CoverageService, scmProviders: Map<String, ScmProvider>) {
    get("/repos/{scm}") {
        val session = call.sessions.get<UserSession>()!!
        call.respond(FreeMarkerContent("repos.ftl", mapOf("session" to session)))
    }

    route("/repos/{scm}/{org}/{name}") {
        intercept(ApplicationCallPipeline.Call) {
            val session = call.sessions.get<UserSession>()!!
            val fullRepoName = call.parameters["org"]!! + "/" + call.parameters["name"]!!
            val scmProvider = scmProviders[call.parameters["scm"]]!!
            val installationId = session.installationIds.getValue(fullRepoName)

            var repo = dataStore.getRepositoryByName(scmProvider.name, fullRepoName)
            if (repo == null) {
                repo = Repository(scmProvider.name, fullRepoName, generateToken(), installationId, Date())
                dataStore.saveRepository(repo)
            }
            if (repo.installationId != installationId) {
                // Installation ID has changed, update it
                repo = repo.copy(installationId = installationId)
                dataStore.saveRepository(repo)
            }

            call.attributes.put(RepoAttribute, repo)
            call.attributes.put(ScmProviderAttribute, scmProvider)

            // Permission check
            // TODO: Allow public access to public repos
            if (!session.repositories.contains(fullRepoName)) {
                call.respond(HttpStatusCode.NotFound)
                return@intercept finish()
            }
        }

        get {
            val session = call.sessions.get<UserSession>()!!
            val repo = call.attributes[RepoAttribute]
            val scmProvider = call.attributes[ScmProviderAttribute]
            val permission = session.repositories.getValue(repo.name)

            val commits = scmProvider.getCommits(repo)

            val uploadUrl = URL(
                call.request.origin.scheme,
                call.request.origin.host,
                call.request.origin.port,
                "/upload").toString()

            call.respond(
                FreeMarkerContent(
                    "repo.ftl",
                    mapOf(
                        "repo" to repo,
                        "isAdmin" to (permission == ScmPermission.ADMIN),
                        "commits" to commits.map {
                            mapOf(
                                "commitId" to it.commitId,
                                "message" to it.message.split("\n").first(),
                                "report" to coverageService.getProcessedReport(repo.scm, repo.name, it.commitId)?.files?.get("")
                            )
                        },
                        "uploadUrl" to uploadUrl
                    )
                )
            )
        }

        get("/commits/{commitId}/files/{path...}") {
            val commitId = call.parameters["commitId"]!!
            val path = call.parameters.getAll("path")!!.joinToString("/")
            val repo = call.attributes[RepoAttribute]
            val scmProvider = call.attributes[ScmProviderAttribute]

            val content =
                scmProvider.getContent(repo, commitId, path).toString(StandardCharsets.UTF_8)
            val coverageReport = coverageService.getProcessedReport(scmProvider.name, repo.name, commitId)
            val coverageFile = coverageReport?.files?.get(path)
            // TODO: Do this properly at statement level.
            val lineMap = coverageFile?.statements?.map { it.lineNumber to it.status }?.toMap() ?: mapOf()

            val lines = content.split('\n').mapIndexed { index, line ->
                mapOf(
                    "content" to line,
                    "color" to when (lineMap[index + 1]) {
                        CoverageStatus.FULL -> "green"
                        CoverageStatus.PARTIAL -> "yellow"
                        CoverageStatus.NONE -> "red"
                        else -> "black"
                    }
                )
            }

            call.respond(
                FreeMarkerContent(
                    "file.ftl",
                    mapOf(
                        "lines" to lines
                    )
                )
            )
        }
    }
}
