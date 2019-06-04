package coverstats.server.datastore.memory

import coverstats.server.datastore.DataStore
import coverstats.server.models.coverage.CoverageReport
import coverstats.server.models.datastore.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * In memory data store for testing
 */
class MemoryDataStore : DataStore {

    private val repoByName = ConcurrentHashMap<String, Repository>()
    private val repoByToken = ConcurrentHashMap<String, Repository>()

    override suspend fun getRepositoryByName(scm: String, name: String): Repository? = repoByName["$scm/$name"]

    override suspend fun getRepositoryByToken(token: String): Repository? = repoByToken[token]

    override suspend fun saveRepository(repo: Repository) {
        repoByName["${repo.scm}/${repo.name}"] = repo
        repoByToken[repo.uploadToken] = repo
    }

    private val reportByName = ConcurrentHashMap<String, CoverageReport>()

    override suspend fun getReportByCommitId(scm: String, repo: String, commitId: String): CoverageReport? =
        reportByName["${scm}/${repo}/${commitId}"]

    override suspend fun saveReport(report: CoverageReport) {
        reportByName["${report.scm}/${report.repo}/${report.commitId}"] = report
    }

}