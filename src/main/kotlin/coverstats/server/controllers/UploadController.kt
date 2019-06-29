package coverstats.server.controllers

import coverstats.server.coverage.patchCoverage
import coverstats.server.coverage.processReport
import coverstats.server.coverage.processors.CoverageProcessor
import coverstats.server.coverage.processors.readCoverage
import coverstats.server.coverage.processors.toReport
import coverstats.server.datastore.DataStore
import coverstats.server.scm.ScmProvider
import coverstats.server.utils.copyToSuspend
import coverstats.server.utils.generateUrl
import io.ktor.application.call
import io.ktor.features.origin
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
                val tree = scmProvider.getFiles(repo, commit!!)
                val patches = scmProvider.getCommitChanges(repo, tree.commitId)
                val coverageFiles = coverageProcessors.readCoverage(report!!, tree.files)
                val coverageReport = coverageFiles.toReport(repo, tree.commitId)

                dataStore.saveReport(coverageReport)

                // TODO: Customizable pass/fail percentage

                // Send status checks
                val processedReport = coverageReport.processReport()
                val rootFile = processedReport.files.getValue("")
                val percentStatements =
                    rootFile.coveredStatements / (rootFile.coveredStatements + rootFile.missedStatements).toDouble() * 100
                val percentBranches =
                    rootFile.coveredBranches / (rootFile.coveredBranches + rootFile.missedBranches).toDouble() * 100

                val url = generateUrl(
                    call.request.origin.scheme,
                    call.request.origin.host,
                    call.request.origin.port,
                    "/repos/${scmProvider.name}/${repo.name}/commits/${tree.commitId}"
                )

                scmProvider.addStatus(
                    repo,
                    tree.commitId,
                    true,
                    url,
                    "${percentStatements.formatString()}% Statement Coverage",
                    "coverstats/statement"
                )

                scmProvider.addStatus(
                    repo,
                    tree.commitId,
                    true,
                    url,
                    "${percentBranches.formatString()}% Branch Coverage",
                    "coverstats/branch"
                )

                val patchReport = coverageReport.patchCoverage(patches)
                if (patchReport.coveredStatements + patchReport.missedStatements > 0) {
                    val percentPatch = patchReport.coveredStatements / (patchReport.coveredStatements + patchReport.missedStatements).toDouble() * 100
                    scmProvider.addStatus(
                        repo,
                        tree.commitId,
                        true,
                        url,
                        "${percentPatch.formatString()}% Patch Statement Coverage",
                        "coverstats/patch"
                    )
                } else {
                    scmProvider.addStatus(
                        repo,
                        tree.commitId,
                        true,
                        url,
                        "No covered file changed in patch",
                        "coverstats/patch"
                    )
                }

                call.respondText("OK")
            }
        }
    }
}

private fun Double.formatString(): String = "%.2f".format(this)
