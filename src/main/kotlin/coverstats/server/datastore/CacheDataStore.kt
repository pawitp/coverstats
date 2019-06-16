package coverstats.server.datastore

import coverstats.server.cache.Cache
import coverstats.server.models.coverage.CoverageReport
import coverstats.server.models.datastore.Repository
import coverstats.server.utils.deserializeJson
import coverstats.server.utils.serializeJson
import java.nio.charset.StandardCharsets.UTF_8

/**
 * Key-value cache data store for testing
 */
class CacheDataStore(val cache: Cache) : DataStore {

    override suspend fun getRepositoryByName(scm: String, name: String): Repository? {
        return cache.get("repo:$scm/$name")?.toString(UTF_8)?.deserializeJson()
    }

    override suspend fun getRepositoryByToken(token: String): Repository? {
        val repoName = cache.get("token:$token")?.toString(UTF_8)
        return repoName?.let { cache.get("repo:$it")?.toString(UTF_8)?.deserializeJson() }
    }

    override suspend fun saveRepository(repo: Repository) {
        cache.put("repo:${repo.scm}/${repo.name}", repo.serializeJson().toByteArray(UTF_8))
        cache.put("token:${repo.uploadToken}", "${repo.scm}/${repo.name}".toByteArray(UTF_8))
    }

    override suspend fun getReportByCommitId(scm: String, repo: String, commitId: String): CoverageReport? {
        return cache.get("report:$scm/$repo/$commitId")?.toString(UTF_8)?.deserializeJson()
    }

    override suspend fun saveReport(report: CoverageReport) {
        val key = "report:${report.scm}/${report.repo}/${report.commitId}"
        cache.put(key, report.serializeJson().toByteArray(UTF_8))
    }

}