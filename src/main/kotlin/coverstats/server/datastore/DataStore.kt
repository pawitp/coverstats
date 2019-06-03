package coverstats.server.datastore

import coverstats.server.models.datastore.Repository

interface DataStore {

    suspend fun getRepositoryByName(scm: String, name: String): Repository?
    suspend fun getRepositoryByToken(token: String): Repository?
    suspend fun saveRepository(repo: Repository)

}