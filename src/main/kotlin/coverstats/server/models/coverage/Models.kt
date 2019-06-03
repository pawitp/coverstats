package coverstats.server.models.coverage

import coverstats.server.models.scm.ScmFileType

data class CoverageReport(
    val files: List<CoverageFile>
)

data class CoverageFile(
    val path: String,
    val type: ScmFileType,
    val lines: List<CoverageLine>,
    val missedLines: Int,
    val coveredLines: Int,
    val missedInstructions: Int,
    val coveredInstructions: Int,
    val missedBranches: Int,
    val coveredBranches: Int
)

data class CoverageLine(
    val lineNumber: Int,
    val status: CoverageStatus
)

enum class CoverageStatus {
    FULL, PARTIAL, NONE
}
