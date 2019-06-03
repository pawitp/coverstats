package coverstats.server.coverage.processors

import coverstats.server.models.coverage.CoverageReport
import coverstats.server.models.scm.ScmFile

object JacocoProcessor : CoverageProcessor {

    override fun readCoverage(report: String, files: List<ScmFile>): CoverageReport? {
        TODO()
    }

}