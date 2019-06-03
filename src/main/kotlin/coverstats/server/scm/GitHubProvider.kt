package coverstats.server.scm

import coverstats.server.models.github.*
import coverstats.server.models.scm.ScmCommit
import coverstats.server.models.scm.ScmPermission
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

        val repositories = repositoryPromises
            .flatMap { it.await().repositories }
            .map { it.fullName to it.permissions.toScmPermission() }
            .toMap()

        val user = userPromise.await()

        return UserSession(name, principal.accessToken, user.login, user.name, repositories)
    }

    override suspend fun getCommits(token: String, repository: String): List<ScmCommit> {
        val ghCommits = httpClient.get<List<GitHubCommit>>("$baseApiPath/repos/$repository/commits") {
            header("Authorization", "Bearer $token")
        }

        return ghCommits.map { ScmCommit(it.sha, it.commit.author.name, it.commit.message) }
    }

}

private fun GitHubPermission.toScmPermission(): ScmPermission {
    if (admin) return ScmPermission.ADMIN
    else if (push) return ScmPermission.WRITE
    else return ScmPermission.READ
}