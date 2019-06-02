package coverstats.server.scm

import coverstats.server.models.session.UserSession
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings
import kotlinx.coroutines.CoroutineScope

interface ScmProvider {

    val name: String
    val oAuthServerSettings: OAuthServerSettings
    suspend fun processOAuth(principal: OAuthAccessTokenResponse.OAuth2, scope: CoroutineScope): UserSession

}