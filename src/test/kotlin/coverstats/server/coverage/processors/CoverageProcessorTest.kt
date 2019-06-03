package coverstats.server.coverage.processors

import coverstats.server.models.coverage.CoverageFile
import coverstats.server.models.scm.ScmFileType
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class CoverageProcessorTest {

    @Test
    fun test_generateReport_should_generate_directory_reports() {
        val coverageFiles = listOf(
            CoverageFile("test/a/a.kt", ScmFileType.FILE, listOf(), 1, 2, 3, 4, 5, 6),
            CoverageFile("test/a/b.kt", ScmFileType.FILE, listOf(), 10, 20, 30, 40, 50, 60),
            CoverageFile("test/b.kt", ScmFileType.FILE, listOf(), 100, 200, 300, 400, 500, 600)
        )
        val report = generateReport(coverageFiles).files.map { it.path to it }.toMap()

        // Should contain all the existing files
        assertEquals(coverageFiles[0], report.getValue("test/a/a.kt"))
        assertEquals(coverageFiles[1], report.getValue("test/a/b.kt"))
        assertEquals(coverageFiles[2], report.getValue("test/b.kt"))

        // Should generate directory reports
        assertEquals(
            CoverageFile("test/a", ScmFileType.DIRECTORY, listOf(), 11, 22, 33, 44, 55, 66),
            report.getValue("test/a")
        )
        assertEquals(
            CoverageFile("test", ScmFileType.DIRECTORY, listOf(), 111, 222, 333, 444, 555, 666),
            report.getValue("test")
        )
        assertEquals(
            CoverageFile("", ScmFileType.DIRECTORY, listOf(), 111, 222, 333, 444, 555, 666),
            report.getValue("")
        )
    }

    @Test
    fun test_generateReport_should_error_when_there_are_duplicate_files() {
        assertThrows<RuntimeException> {
            generateReport(
                listOf(
                    CoverageFile("test/a/a", ScmFileType.FILE, listOf(), 1, 2, 3, 4, 5, 6),
                    CoverageFile("test/a/a", ScmFileType.FILE, listOf(), 10, 20, 30, 40, 50, 60)
                )
            )
        }
    }
}