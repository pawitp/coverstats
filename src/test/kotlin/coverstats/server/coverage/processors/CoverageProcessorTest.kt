package coverstats.server.coverage.processors

import coverstats.server.models.coverage.CoverageFile
import coverstats.server.models.coverage.CoverageStatement
import coverstats.server.models.coverage.CoverageStatus
import coverstats.server.repoFixture
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class CoverageProcessorTest {

    @Test
    fun test_generateReport_should_generate_summary_reports() {
        val coverageFiles = listOf(
            CoverageFile(
                "test/a/b.kt", listOf(
                    CoverageStatement(1, -1, -1, CoverageStatus.FULL, 0, 0),
                    CoverageStatement(2, -1, -1, CoverageStatus.PARTIAL, 2, 0)
                )
            ),
            CoverageFile(
                "test/b.kt", listOf(
                    CoverageStatement(1, -1, -1, CoverageStatus.NONE, 0, 1),
                    CoverageStatement(2, -1, -1, CoverageStatus.FULL, 0, 0)
                )
            )
        )
        val report = coverageFiles.toReport(repoFixture, "commitId")

        assertEquals("scm", report.scm)
        assertEquals("org/name", report.repo)
        assertEquals("commitId", report.commitId)
        assertEquals(coverageFiles, report.files)
    }

    @Test
    fun test_generateReport_should_error_when_there_are_duplicate_files() {
        assertThrows<RuntimeException> {
            listOf(
                CoverageFile("test/a/a", listOf()),
                CoverageFile("test/a/a", listOf())
            ).toReport(repoFixture, "commitId")
        }
    }
}