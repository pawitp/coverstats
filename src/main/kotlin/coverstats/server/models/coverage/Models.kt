package coverstats.server.models.coverage

import coverstats.server.models.scm.ScmFileType
import java.util.*

// Report for fast access (stored in cache)
data class ProcessedCoverageReport(
    val files: Map<String, ProcessedCoverageFile>
)

data class ProcessedCoverageFile(
    val path: String,
    val type: ScmFileType,
    val parentPath: String,
    val childrenPaths: Set<String>,
    val statements: List<CoverageStatement>,
    val missedStatements: Int,
    val coveredStatements: Int,
    val missedBranches: Int,
    val coveredBranches: Int
)

// Raw report stored in database
data class CoverageReport(
    val scm: String,
    val repo: String,
    val commitId: String,
    val files: List<CoverageFile>,
    val createdAt: Date
)

data class CoverageFile(
    val path: String,
    val statements: List<CoverageStatement>
)

// Patch coverage summary
data class PatchCoverageReport(
    val missedStatements: Int,
    val coveredStatements: Int
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
