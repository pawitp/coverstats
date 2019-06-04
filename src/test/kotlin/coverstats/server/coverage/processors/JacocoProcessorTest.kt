package coverstats.server.coverage.processors

import coverstats.server.models.coverage.CoverageStatement
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
        assertEquals(4, scmModel.statements.size)
        for (line in scmModel.statements) {
            assertEquals(CoverageStatus.NONE, line.status)
            assertEquals(0, line.missedBranches)
            assertEquals(0, line.coveredBranches)
        }

        val randomUtil = coverageReport.getValue("src/main/kotlin/coverstats/server/utils/RandomUtils.kt")
        assertEquals(5, randomUtil.statements.size)
        assertEquals(listOf(
            CoverageStatement(6, -1, -1, CoverageStatus.PARTIAL, 1, 3),
            CoverageStatement(7, -1, -1, CoverageStatus.FULL, 0, 0),
            CoverageStatement(10, -1, -1, CoverageStatus.FULL, 0, 0),
            CoverageStatement(11, -1, -1, CoverageStatus.FULL, 0, 0),
            CoverageStatement(12, -1, -1, CoverageStatus.FULL, 0, 0)
        ), randomUtil.statements)
    }

}