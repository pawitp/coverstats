package coverstats.server.coverage.processors

import coverstats.server.models.coverage.CoverageFile
import coverstats.server.models.coverage.CoverageReport
import coverstats.server.models.coverage.CoverageStatus
import coverstats.server.models.datastore.Repository
import coverstats.server.models.scm.ScmFile
import java.util.*

interface CoverageProcessor {
    fun readCoverage(report: String, scmFiles: List<ScmFile>): List<CoverageFile>?
}

fun List<CoverageProcessor>.readCoverage(report: String, scmFiles: List<ScmFile>): List<CoverageFile> {
    for (processor in this) {
        val parsedReport = processor.readCoverage(report, scmFiles)
        if (parsedReport != null) {
            return parsedReport
        }
    }
    throw RuntimeException("Unable to read coverage report")
}

fun List<CoverageFile>.toReport(repo: Repository, commitId: String): CoverageReport {
    // Sanity check that we don't have any duplicate files
    val paths = this.map { it.path }
    if (paths.size != paths.toSet().size) {
        throw RuntimeException("Duplicate files found in report")
    }

    var missedStatements = 0
    var coveredStatements = 0
    var missedBranches = 0
    var coveredBranches = 0

    for (file in this) {
        for (stmt in file.statements) {
            coveredBranches += stmt.coveredBranches
            missedBranches += stmt.missedBranches

            if (stmt.status != CoverageStatus.NONE) {
                coveredStatements++
            } else {
                missedStatements++
            }

        }
    }

    return CoverageReport(
        repo.scm,
        repo.name,
        commitId,
        this,
        missedStatements,
        coveredStatements,
        missedBranches,
        coveredBranches,
        Date()
    )
}
