package coverstats.server.coverage.processors

import coverstats.server.models.coverage.CoverageFile
import coverstats.server.models.coverage.CoverageReport
import coverstats.server.models.scm.ScmFile
import coverstats.server.models.scm.ScmFileType
import java.lang.RuntimeException

interface CoverageProcessor {
    fun readCoverage(report: String, scmFiles: List<ScmFile>): List<CoverageFile>?
}

fun List<CoverageProcessor>.readCoverage(report: String, scmFiles: List<ScmFile>): CoverageReport {
    for (processor in this) {
        val parsedReport = processor.readCoverage(report, scmFiles)
        if (parsedReport != null) {
            return generateReport(parsedReport)
        }
    }
    throw RuntimeException("Unable to read coverage report")
}

fun generateReport(coverageFiles: List<CoverageFile>): CoverageReport {
    // Sanity check that we don't have any duplicate files
    val paths = coverageFiles.map { it.path }
    if (paths.size != paths.toSet().size) {
        throw RuntimeException("Duplicate files found in report")
    }

    val directoryReports = mutableMapOf<String, CoverageFile>()
    for (file in coverageFiles) {
        var filePath = file.path
        while (filePath != "") {
            val lastIndex = filePath.lastIndexOf("/").let { if (it == -1) 0 else it }
            filePath = filePath.substring(0, lastIndex)
            val dirReport = directoryReports[filePath]
                ?: CoverageFile(filePath, ScmFileType.DIRECTORY, listOf(), 0, 0, 0, 0, 0, 0)
            val newDirReport = dirReport.copy(
                missedLines = dirReport.missedLines + file.missedLines,
                coveredLines = dirReport.coveredLines + file.coveredLines,
                missedInstructions = dirReport.missedInstructions + file.missedInstructions,
                coveredInstructions = dirReport.coveredInstructions + file.coveredInstructions,
                missedBranches = dirReport.missedBranches + file.missedBranches,
                coveredBranches = dirReport.coveredBranches + file.coveredBranches
            )
            directoryReports[filePath] = newDirReport
        }
    }

    return CoverageReport(coverageFiles + directoryReports.values)
}
