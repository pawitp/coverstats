package coverstats.server.datastore

import coverstats.server.cache.Cache
import coverstats.server.models.coverage.CoverageReport
import coverstats.server.models.datastore.Repository
import java.net.URI

interface DataStore {

    suspend fun getRepositoryByName(scm: String, name: String): Repository?
    suspend fun getRepositoryByToken(token: String): Repository?
    suspend fun saveRepository(repo: Repository)

    suspend fun getReportByCommitId(scm: String, repo: String, commitId: String): CoverageReport?
    suspend fun saveReport(report: CoverageReport)

}

fun createDataStoreFromUri(uri: String, cache: Cache): DataStore {
    val parsedUri = URI(uri)

    return when (parsedUri.scheme) {
        "cache" -> CacheDataStore(cache)
        "gclouddatastore" -> GoogleCloudDataStore()
        else -> throw Exception("Unknown cache uri: $uri")
    }
}
