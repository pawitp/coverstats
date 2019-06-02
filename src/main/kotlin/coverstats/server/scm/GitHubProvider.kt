package coverstats.server.scm

import coverstats.server.models.github.GitHubInstallations
import coverstats.server.models.github.GitHubRepositories
import coverstats.server.models.github.GitHubUser
import coverstats.server.models.session.UserSession
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class GitHubProvider(
    override val name: String,
    val basePath: String,
    val baseApiPath: String,
    val clientId: String,
    val clientSecret: String,
    val httpClient: HttpClient
) : ScmProvider {

    override val oAuthServerSettings: OAuthServerSettings = OAuthServerSettings.OAuth2ServerSettings(
        name = name,
        authorizeUrl = "$basePath/login/oauth/authorize",
        accessTokenUrl = "$basePath/login/oauth/access_token",
        clientId = clientId,
        clientSecret = clientSecret
    )

    override suspend fun processOAuth(principal: OAuthAccessTokenResponse.OAuth2, scope: CoroutineScope): UserSession {
        val accessToken = principal.accessToken

        // Fetch username
        val userPromise = scope.async {
            httpClient.get<GitHubUser>("$baseApiPath/user") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/vnd.github.machine-man-preview+json")
            }
        }

        // Fetch repos
        val installations =
            httpClient.get<GitHubInstallations>("$baseApiPath/user/installations") {
                header("Authorization", "Bearer $accessToken")
                header("Accept", "application/vnd.github.machine-man-preview+json")
            }

        val repositoryPromises = installations.installations.map {
            scope.async {
                httpClient.get<GitHubRepositories>("$baseApiPath/user/installations/${it.id}/repositories") {
                    header("Authorization", "Bearer $accessToken")
                    header("Accept", "application/vnd.github.machine-man-preview+json")
                }
            }
        }

        val repositories = repositoryPromises.flatMap { it.await().repositories }.map { it.fullName }

        val user = userPromise.await()

        return UserSession(name, principal.accessToken, user.login, user.name, repositories)
    }

}