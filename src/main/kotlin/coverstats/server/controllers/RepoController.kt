package coverstats.server.controllers

import coverstats.server.RepoNameAttribute
import coverstats.server.ScmProviderAttribute
import coverstats.server.datastore.DataStore
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
}
