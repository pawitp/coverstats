package coverstats.server.datastore.memory

import coverstats.server.datastore.DataStore
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
        val nameWithScm = "${repo.scm}/${repo.name}"
        repoByName[nameWithScm] = repo
        repoByToken[nameWithScm] = repo
    }

}