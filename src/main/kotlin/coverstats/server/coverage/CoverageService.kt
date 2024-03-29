package coverstats.server.coverage

import coverstats.server.cache.Cache
import coverstats.server.cache.cachedNullable
import coverstats.server.datastore.DataStore
import coverstats.server.models.coverage.*
import coverstats.server.models.scm.ScmFileType
import coverstats.server.utils.linesChangedInPatch

class CoverageService(private val dataStore: DataStore, private val cache: Cache) {
    suspend fun getProcessedReport(scm: String, repo: String, commitId: String): ProcessedCoverageReport? {
        return cache.cachedNullable("r:$scm/$repo/$commitId") {
            dataStore.getReportByCommitId(scm, repo, commitId)?.processReport()
        }
    }
}

fun CoverageReport.processReport(): ProcessedCoverageReport {
    val processedFiles = mutableMapOf<String, ProcessedCoverageFile>()

    for (file in files) {
        val missedStatements = file.statements.count { it.status == CoverageStatus.NONE }
        val coveredStatements = file.statements.count { it.status != CoverageStatus.NONE }
        val missedBranches = file.statements.sumBy { it.missedBranches }
        val coveredBranches = file.statements.sumBy { it.coveredBranches }

        // Generate individual report
        processedFiles[file.path] = ProcessedCoverageFile(
            file.path,
            ScmFileType.FILE,
            file.path.parentDirectory(),
            setOf(),
            file.statements,
            missedStatements,
            coveredStatements,
            missedBranches,
            coveredBranches
        )

        // Generate directory report(s)
        var currentPath = file.path
        while (currentPath != "") {
            val parentPath = currentPath.parentDirectory()
            val dirReport = processedFiles[parentPath]
                ?: ProcessedCoverageFile(
                    parentPath,
                    ScmFileType.DIRECTORY,
                    parentPath.parentDirectory(),
                    setOf(),
                    listOf(),
                    0,
                    0,
                    0,
                    0
                )
            val newDirReport = dirReport.copy(
                childrenPaths = dirReport.childrenPaths + currentPath,
                missedStatements = dirReport.missedStatements + missedStatements,
                coveredStatements = dirReport.coveredStatements + coveredStatements,
                missedBranches = dirReport.missedBranches + missedBranches,
                coveredBranches = dirReport.coveredBranches + coveredBranches
            )
            processedFiles[parentPath] = newDirReport
            currentPath = parentPath
        }
    }

    // Remove intermediate directories
    processedFiles.flatten("")

    return ProcessedCoverageReport(processedFiles)
}

fun CoverageReport.patchCoverage(patches: Map<String, String>): PatchCoverageReport {
    var missedStatements = 0
    var coveredStatements = 0

    for (file in files) {
        val changedLines = patches[file.path]?.let { linesChangedInPatch(it) }.orEmpty()
        val changedStatements = file.statements.filter { changedLines.contains(it.lineNumber) }
        missedStatements += changedStatements.count { it.status == CoverageStatus.NONE }
        coveredStatements += changedStatements.count { it.status != CoverageStatus.NONE }
    }

    return PatchCoverageReport(missedStatements, coveredStatements)
}

private fun MutableMap<String, ProcessedCoverageFile>.flatten(path: String) {
    val dir = this.getValue(path)
    if (dir.path != "" && dir.childrenPaths.size == 1) {
        // Flatten the path
        val childPath = dir.childrenPaths.iterator().next()

        // Update parent
        val parent = this.getValue(dir.parentPath)
        this[dir.parentPath] = parent.copy(
            childrenPaths = parent.childrenPaths - dir.path + childPath
        )

        // Update child
        val child = this.getValue(childPath)
        this[childPath] = child.copy(
            parentPath = parent.path
        )

        // Remove self
        this.remove(dir.path)
    }

    // Recursive flatten
    dir.childrenPaths.forEach { this.flatten(it) }
}

private fun String.parentDirectory(): String {
    val lastIndex = this.lastIndexOf("/").let { if (it == -1) 0 else it }
    return this.substring(0, lastIndex)
}