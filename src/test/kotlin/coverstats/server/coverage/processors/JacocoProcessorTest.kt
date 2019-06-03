package coverstats.server.coverage.processors

import coverstats.server.models.coverage.CoverageLine
import coverstats.server.models.coverage.CoverageStatus
import coverstats.server.models.scm.ScmFile
import coverstats.server.models.scm.ScmFileType.DIRECTORY
import coverstats.server.models.scm.ScmFileType.FILE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JacocoProcessorTest {

    @Test
    fun test_JacocoProcessor_return_null_for_non_jacoco_files() {
        assertNull(JacocoProcessor.readCoverage("asdf", listOf()))
    }

    @Test
    fun test_JacocoProcessor_parse_jacoco_file() {
        val coverageReport = JacocoProcessor.readCoverage(
            javaClass.getResource("/exampleJacocoReport.xml").readText(), listOf(
                ScmFile(".gitignore", FILE),
                ScmFile("build.gradle.kts", FILE),
                ScmFile("gradle.properties", FILE),
                ScmFile("gradle", DIRECTORY),
                ScmFile("gradle/wrapper", DIRECTORY),
                ScmFile("gradle/wrapper/gradle-wrapper.jar", FILE),
                ScmFile("gradle/wrapper/gradle-wrapper.properties", FILE),
                ScmFile("gradlew", FILE),
                ScmFile("gradlew.bat", FILE),
                ScmFile("settings.gradle", FILE),
                ScmFile("src", DIRECTORY),
                ScmFile("src/main", DIRECTORY),
                ScmFile("src/main/kotlin", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server/Main.kt", FILE),
                ScmFile("src/main/kotlin/coverstats/server/models", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server/models/coverage", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server/models/coverage/Models.kt", FILE),
                ScmFile("src/main/kotlin/coverstats/server/models/datastore", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server/models/datastore/Models.kt", FILE),
                ScmFile("src/main/kotlin/coverstats/server/models/github", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server/models/github/Models.kt", FILE),
                ScmFile("src/main/kotlin/coverstats/server/models/scm", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server/models/scm/Models.kt", FILE),
                ScmFile("src/main/kotlin/coverstats/server/models/session", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server/models/session/UserSession.kt", FILE),
                ScmFile("src/main/kotlin/coverstats/server/scm", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server/scm/GitHubProvider.kt", FILE),
                ScmFile("src/main/kotlin/coverstats/server/scm/ScmProvider.kt", FILE),
                ScmFile("src/main/kotlin/coverstats/server/utils", DIRECTORY),
                ScmFile("src/main/kotlin/coverstats/server/utils/IOUtils.kt", FILE),
                ScmFile("src/main/kotlin/coverstats/server/utils/RandomUtils.kt", FILE),
                ScmFile("src/main/resources", DIRECTORY),
                ScmFile("src/main/resources/application.conf", FILE)
            )
        )!!.map { it.path to it }.toMap()

        assertEquals(7, coverageReport.size)

        val scmModel = coverageReport.getValue("src/main/kotlin/coverstats/server/models/scm/Models.kt")
        assertEquals(FILE, scmModel.type)
        assertEquals(4, scmModel.lines.size)
        for (line in scmModel.lines) {
            assertEquals(CoverageStatus.NONE, line.status)
        }
        assertEquals(4, scmModel.missedLines)
        assertEquals(0, scmModel.coveredLines)
        assertEquals(21, scmModel.missedInstructions)
        assertEquals(0, scmModel.coveredInstructions)
        assertEquals(0, scmModel.missedBranches)
        assertEquals(0, scmModel.coveredBranches)

        val randomUtil = coverageReport.getValue("src/main/kotlin/coverstats/server/utils/RandomUtils.kt")
        assertEquals(FILE, randomUtil.type)
        assertEquals(5, randomUtil.lines.size)
        assertEquals(0, randomUtil.missedLines)
        assertEquals(5, randomUtil.coveredLines)
        assertEquals(1, randomUtil.missedInstructions)
        assertEquals(21, randomUtil.coveredInstructions)
        assertEquals(0, randomUtil.missedBranches)
        assertEquals(0, randomUtil.coveredBranches)
        assertEquals(listOf(
            CoverageLine(6, CoverageStatus.PARTIAL),
            CoverageLine(7, CoverageStatus.FULL),
            CoverageLine(10, CoverageStatus.FULL),
            CoverageLine(11, CoverageStatus.FULL),
            CoverageLine(12, CoverageStatus.FULL)
        ), randomUtil.lines)
    }

}