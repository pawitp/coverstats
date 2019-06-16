package coverstats.server.coverage

import coverstats.server.coverage.processors.toReport
import coverstats.server.models.coverage.CoverageFile
import coverstats.server.models.coverage.CoverageStatement
import coverstats.server.models.coverage.CoverageStatus
import coverstats.server.models.scm.ScmFileType
import coverstats.server.repoFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CoverageServiceTest {

    @Test
    fun test_processReport_should_generate_directory_reports() {
        val coverageFiles = listOf(
            CoverageFile(
                "test/a/a/a.kt", listOf(
                    CoverageStatement(1, -1, -1, CoverageStatus.FULL, 0, 0),
                    CoverageStatement(2, -1, -1, CoverageStatus.PARTIAL, 2, 0)
                )
            ),
            CoverageFile(
                "test/a/a/b.kt", listOf(
                    CoverageStatement(1, -1, -1, CoverageStatus.NONE, 0, 0),
                    CoverageStatement(2, -1, -1, CoverageStatus.FULL, 6, 10),
                    CoverageStatement(3, -1, -1, CoverageStatus.FULL, 0, 0),
                    CoverageStatement(4, -1, -1, CoverageStatus.FULL, 0, 0),
                    CoverageStatement(5, -1, -1, CoverageStatus.FULL, 0, 0)
                )
            ),
            CoverageFile(
                "test/b.kt", listOf(
                    CoverageStatement(1, -1, -1, CoverageStatus.NONE, 0, 0),
                    CoverageStatement(2, -1, -1, CoverageStatus.FULL, 3, 5),
                    CoverageStatement(3, -1, -1, CoverageStatus.FULL, 0, 0)
                )
            )
        )
        val report = coverageFiles.toReport(repoFixture, "commitId").processReport()

        // Should contain all the existing files
        report.files.getValue("test/a/a/a.kt").let {
            assertEquals("test/a/a/a.kt", it.path)
            assertEquals(ScmFileType.FILE, it.type)
            assertEquals("test/a/a", it.parentPath)
            assertEquals(0, it.childrenPaths.size)
            assertEquals(coverageFiles[0].statements, it.statements)
            assertEquals(0, it.missedStatements)
            assertEquals(2, it.coveredStatements)
            assertEquals(2, it.missedBranches)
            assertEquals(0, it.coveredBranches)
        }
        report.files.getValue("test/a/a/b.kt").let {
            assertEquals("test/a/a/b.kt", it.path)
            assertEquals(ScmFileType.FILE, it.type)
            assertEquals("test/a/a", it.parentPath)
            assertEquals(0, it.childrenPaths.size)
            assertEquals(coverageFiles[1].statements, it.statements)
            assertEquals(1, it.missedStatements)
            assertEquals(4, it.coveredStatements)
            assertEquals(6, it.missedBranches)
            assertEquals(10, it.coveredBranches)
        }
        report.files.getValue("test/b.kt").let {
            assertEquals("test/b.kt", it.path)
            assertEquals(ScmFileType.FILE, it.type)
            assertEquals("test", it.parentPath)
            assertEquals(0, it.childrenPaths.size)
            assertEquals(coverageFiles[2].statements, it.statements)
            assertEquals(1, it.missedStatements)
            assertEquals(2, it.coveredStatements)
            assertEquals(3, it.missedBranches)
            assertEquals(5, it.coveredBranches)
        }

        // Should generate directory reports
        report.files.getValue("test/a/a").let {
            assertEquals("test/a/a", it.path)
            assertEquals(ScmFileType.DIRECTORY, it.type)
            assertEquals("test", it.parentPath) // Skip parents with one children
            assertEquals(setOf("test/a/a/a.kt", "test/a/a/b.kt"), it.childrenPaths)
            assertEquals(0, it.statements.size)
            assertEquals(1, it.missedStatements)
            assertEquals(6, it.coveredStatements)
            assertEquals(8, it.missedBranches)
            assertEquals(10, it.coveredBranches)
        }
        report.files.getValue("test").let {
            assertEquals("test", it.path)
            assertEquals(ScmFileType.DIRECTORY, it.type)
            assertEquals("", it.parentPath) // Skip parents with one children
            assertEquals(setOf("test/a/a", "test/b.kt"), it.childrenPaths)
            assertEquals(0, it.statements.size)
            assertEquals(2, it.missedStatements)
            assertEquals(8, it.coveredStatements)
            assertEquals(11, it.missedBranches)
            assertEquals(15, it.coveredBranches)
        }
        report.files.getValue("").let {
            assertEquals("", it.path)
            assertEquals(ScmFileType.DIRECTORY, it.type)
            assertEquals("", it.parentPath) // Skip parents with one children
            assertEquals(setOf("test"), it.childrenPaths)
            assertEquals(0, it.statements.size)
            assertEquals(2, it.missedStatements)
            assertEquals(8, it.coveredStatements)
            assertEquals(11, it.missedBranches)
            assertEquals(15, it.coveredBranches)
        }

        // Should not contain intermediate directory
        assertFalse(report.files.containsKey("test/a"))
    }

}