package coverstats.server.scm

import coverstats.server.models.datastore.Repository
import coverstats.server.models.scm.ScmCommit
import coverstats.server.models.scm.ScmFile
import coverstats.server.models.session.UserSession
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings
import kotlinx.coroutines.CoroutineScope

interface ScmProvider {

    val name: String
    val oAuthServerSettings: OAuthServerSettings

    suspend fun processOAuth(principal: OAuthAccessTokenResponse.OAuth2, scope: CoroutineScope): UserSession
    suspend fun getCommits(token: String, repository: String): List<ScmCommit>
    suspend fun getFiles(token: String, repository: String, commitId: String): List<ScmFile>
    suspend fun getAppToken(repo: Repository): String

}