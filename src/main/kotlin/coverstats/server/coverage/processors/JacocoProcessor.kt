package coverstats.server.coverage.processors

import coverstats.server.models.coverage.CoverageFile
import coverstats.server.models.coverage.CoverageStatement
import coverstats.server.models.coverage.CoverageStatus
import coverstats.server.models.scm.ScmFile
import coverstats.server.models.scm.ScmFileType
import coverstats.server.utils.xmlSecureUnmarshall
import mu.KotlinLogging
import org.apache.commons.text.similarity.LevenshteinDistance
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.*

@XmlRootElement(name = "report")
@XmlAccessorType(XmlAccessType.FIELD)
private data class JacocoReport(
    @field:XmlElement(name = "package")
    val packages: List<JacocoPackage>
) {
    constructor() : this(packages = mutableListOf())
}

@XmlAccessorType(XmlAccessType.FIELD)
private data class JacocoPackage(
    @field:XmlAttribute(name = "name")
    val name: String,
    @field:XmlElement(name = "sourcefile")
    val files: List<JacocoFile>
) {
    constructor() : this(name = "", files = mutableListOf())
}

@XmlAccessorType(XmlAccessType.FIELD)
private data class JacocoFile(
    @field:XmlAttribute(name = "name")
    val name: String,
    @field:XmlElement(name = "line")
    val lines: List<JacocoLine>
) {
    constructor() : this(name = "", lines = mutableListOf())
}

@XmlAccessorType(XmlAccessType.FIELD)
private data class JacocoLine(
    @field:XmlAttribute(name = "nr")
    val lineNumber: Int,
    @field:XmlAttribute(name = "mi")
    val missedInstructions: Int,
    @field:XmlAttribute(name = "ci")
    val coveredInstructions: Int,
    @field:XmlAttribute(name = "mb")
    val missedBranches: Int,
    @field:XmlAttribute(name = "cb")
    val coveredBranches: Int
) {
    constructor() : this(
        lineNumber = 0,
        missedInstructions = 0,
        coveredInstructions = 0,
        missedBranches = 0,
        coveredBranches = 0
    )
}

private val jaxbContext = JAXBContext.newInstance(JacocoReport::class.java)
private val lvDistance = LevenshteinDistance()
private val logger = KotlinLogging.logger {}

object JacocoProcessor : CoverageProcessor {

    override fun readCoverage(report: String, scmFiles: List<ScmFile>): List<CoverageFile>? {
        if (!report.contains("-//JACOCO//DTD Report 1.0//EN")) {
            // Not a JaCoCo report
            return null
        }

        val jacocoReport = jaxbContext.xmlSecureUnmarshall<JacocoReport>(report)
        val scmFilesExcludeDir = scmFiles.filter { it.type == ScmFileType.FILE }

        // For each file, try to match with file in SCM
        return jacocoReport.packages.flatMap { pkg ->
            pkg.files.mapNotNull { f ->
                val matchingScmFiles = scmFilesExcludeDir
                    .filter { it.path == f.name || it.path.endsWith("/${f.name}") }
                if (matchingScmFiles.isEmpty()) {
                    logger.warn { "Unable to find a match for ${pkg.name}/${f.name}" }
                    // Can't find any match
                    null
                } else {
                    val scmFile = matchingScmFiles
                        .sortedBy { lvDistance.apply(it.path, "${pkg.name}/${f.name}") }
                        .first()

                    // For JaCoCo, a statement is a line
                    val coverageLines = f.lines.map {
                        val status = when {
                            it.coveredInstructions == 0 -> CoverageStatus.NONE
                            it.missedInstructions == 0 -> CoverageStatus.FULL
                            else -> CoverageStatus.PARTIAL
                        }
                        CoverageStatement(it.lineNumber, -1, -1, status, it.missedBranches, it.coveredBranches)
                    }
                    CoverageFile(scmFile.path, coverageLines)
                }
            }
        }
    }

}