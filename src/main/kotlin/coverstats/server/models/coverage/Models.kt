package coverstats.server.models.coverage

import java.util.*

data class CoverageReport(
    val scm: String,
    val repo: String,
    val commitId: String,

    val files: List<CoverageFile>,

    // Summary for easy access in summary reports
    val missedStatements: Int,
    val coveredStatements: Int,
    val missedBranches: Int,
    val coveredBranches: Int,

    val createdAt: Date
)

data class CoverageFile(
    val path: String,
    val statements: List<CoverageStatement>
)

// Smallest unit of coverage. If start == end == -1, then the statement is the full line.
data class CoverageStatement(
    val lineNumber: Int,
    val start: Int,
    val end: Int,
    val status: CoverageStatus,
    val missedBranches: Int,
    val coveredBranches: Int
)

enum class CoverageStatus {
    FULL, PARTIAL, NONE
}
