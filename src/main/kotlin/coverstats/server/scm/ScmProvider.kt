package coverstats.server.scm

import coverstats.server.models.datastore.Repository
import coverstats.server.models.scm.ScmCommit
import coverstats.server.models.scm.ScmTree
import coverstats.server.models.session.UserSession
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings

interface ScmProvider {

    val name: String
    val oAuthServerSettings: OAuthServerSettings

    suspend fun processOAuth(principal: OAuthAccessTokenResponse.OAuth2): UserSession

    suspend fun getCommits(repo: Repository): List<ScmCommit>
    suspend fun getFiles(repo: Repository, commitId: String): ScmTree
    suspend fun getContent(repo: Repository, commitId: String, path: String): ByteArray
    suspend fun isPublic(repo: Repository): Boolean
    suspend fun addStatus(
        repo: Repository,
        commitId: String,
        passed: Boolean,
        url: String,
        description: String,
        context: String
    )

}