package coverstats.server.controllers

import coverstats.server.RepoNameAttribute
import coverstats.server.ScmProviderAttribute
import coverstats.server.datastore.DataStore
import coverstats.server.models.coverage.CoverageStatus
import coverstats.server.models.datastore.Repository
import coverstats.server.models.scm.ScmPermission
import coverstats.server.models.session.UserSession
import coverstats.server.scm.ScmProvider
import coverstats.server.utils.generateToken
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import java.nio.charset.StandardCharsets

fun Route.repos(dataStore: DataStore, scmProviders: Map<String, ScmProvider>) {
    get("/repos/{scm}") {
        val session = call.sessions.get<UserSession>()!!
        call.respond(FreeMarkerContent("repos.ftl", mapOf("session" to session)))
    }

    route("/repos/{scm}/{org}/{name}") {
        intercept(ApplicationCallPipeline.Call) {
            val session = call.sessions.get<UserSession>()!!
            val fullRepoName = call.parameters["org"]!! + "/" + call.parameters["name"]!!

            call.attributes.put(RepoNameAttribute, fullRepoName)
            call.attributes.put(ScmProviderAttribute, scmProviders[call.parameters["scm"]]!!)

            // Permission check
            // TODO: Allow public access to public repos
            if (!session.repositories.contains(fullRepoName)) {
                call.respond(HttpStatusCode.NotFound)
                return@intercept finish()
            }
        }

        get {
            val session = call.sessions.get<UserSession>()!!
            val fullRepoName = call.attributes[RepoNameAttribute]
            val scmProvider = call.attributes[ScmProviderAttribute]
            val permission = session.repositories.getValue(fullRepoName)
            val installationId = session.installationIds.getValue(fullRepoName)

            var repo = dataStore.getRepositoryByName(scmProvider.name, fullRepoName)
            if (repo == null) {
                repo = Repository(scmProvider.name, fullRepoName, generateToken(), installationId)
                dataStore.saveRepository(repo)
            }
            if (repo.installationId != installationId) {
                // Installation ID has changed, update it
                repo = repo.copy(installationId = installationId)
                dataStore.saveRepository(repo)
            }

            val commits = scmProvider.getCommits(session.token, fullRepoName)
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
                                "report" to dataStore.getReportByCommitId(repo.scm, repo.name, it.commitId)
                            )
                        }
                    )
                )
            )
        }

        get("/commits/{commitId}/files/{path...}") {
            val session = call.sessions.get<UserSession>()!!
            val commitId = call.parameters["commitId"]!!
            val path = call.parameters.getAll("path")!!.joinToString("/")
            val fullRepoName = call.attributes[RepoNameAttribute]
            val scmProvider = call.attributes[ScmProviderAttribute]

            val content =
                scmProvider.getContent(session.token, fullRepoName, commitId, path).toString(StandardCharsets.UTF_8)
            val coverageReport = dataStore.getReportByCommitId(scmProvider.name, fullRepoName, commitId)
            val coverageFile = coverageReport?.files?.find { it.path == path }

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
