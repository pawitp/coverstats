package coverstats.server.controllers

import coverstats.server.coverage.processors.CoverageProcessor
import coverstats.server.coverage.processors.readCoverage
import coverstats.server.datastore.DataStore
import coverstats.server.scm.ScmProvider
import coverstats.server.utils.copyToSuspend
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.post
import java.io.ByteArrayOutputStream
import java.lang.RuntimeException

fun Route.upload(
    dataStore: DataStore,
    scmProviders: Map<String, ScmProvider>,
    coverageProcessors: List<CoverageProcessor>
) {
    post("/upload") {
        val multipart = call.receiveMultipart()

        var token = ""
        var commit: String? = null
        var report: String? = null

        // Read multipart data
        multipart.forEachPart { part ->
            if (part is PartData.FormItem) {
                when {
                    part.name == "token" -> token = part.value
                    part.name == "commit" -> commit = part.value
                }
            } else if (part is PartData.FileItem) {
                if (report != null) {
                    throw RuntimeException("Only one report supported")
                }

                val baos = ByteArrayOutputStream()
                part.streamProvider().use { its ->
                    its.copyToSuspend(baos)
                }
                report = baos.toString("utf-8")
            }

            part.dispose()
        }

        val repo = dataStore.getRepositoryByToken(token)
        when {
            repo == null -> call.respond(HttpStatusCode.Forbidden, "Invalid token")
            commit == null -> call.respond(HttpStatusCode.BadRequest, "No commit provided")
            report == null -> call.respond(HttpStatusCode.BadRequest, "No report provided")
            else -> {
                val scmProvider = scmProviders.getValue(repo.scm)
                val tree = scmProvider.getFiles(scmProvider.getAppToken(repo), repo.name, commit!!)
                val processedReport = coverageProcessors.readCoverage(report!!, tree)

                // TODO: Store report in database
                // TOOD: Send status check
                call.respondText("OK")
            }
        }
    }
}
