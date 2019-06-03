package coverstats.server.coverage.processors

import coverstats.server.models.coverage.CoverageReport
import coverstats.server.models.scm.ScmFile
import java.lang.RuntimeException

interface CoverageProcessor {
    fun readCoverage(report: String, files: List<ScmFile>): CoverageReport?
}

fun List<CoverageProcessor>.readCoverage(report: String, files: List<ScmFile>): CoverageReport {
    for (processor in this) {
        val parsedReport = processor.readCoverage(report, files)
        if (parsedReport != null) {
            return parsedReport
        }
    }
    throw RuntimeException("Unable to read coverage report")
}