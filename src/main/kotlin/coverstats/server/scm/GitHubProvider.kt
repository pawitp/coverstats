package coverstats.server.scm

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.benmanes.caffeine.cache.Caffeine
import coverstats.server.models.datastore.Repository
import coverstats.server.models.github.*
import coverstats.server.models.scm.*
import coverstats.server.models.session.UserSession
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.RSAPrivateKeySpec
import java.util.*
import java.util.concurrent.TimeUnit

private val decoder = Base64.getMimeDecoder()

class GitHubProvider(
    override val name: String,
    val basePath: String,
    val baseApiPath: String,
    val clientId: String,
    val clientSecret: String,
    val appId: String,
    val privateKey: String,
    val httpClient: HttpClient
) : ScmProvider {

    private val jwtAlgorithm = loadPrivateKey(privateKey)

    private val tokenCache = Caffeine.newBuilder()
        .expireAfterWrite(50, TimeUnit.MINUTES) // GitHub tokens are valid for 1 hour
        .build<String, String>()

    override val oAuthServerSettings: OAuthServerSettings = OAuthServerSettings.OAuth2ServerSettings(
        name = name,
        authorizeUrl = "$basePath/login/oauth/authorize",
        accessTokenUrl = "$basePath/login/oauth/access_token",
        clientId = clientId,
        clientSecret = clientSecret
    )

    override suspend fun processOAuth(principal: OAuthAccessTokenResponse.OAuth2): UserSession {
        return coroutineScope {
            val accessToken = principal.accessToken

            // Fetch username
            val userPromise = async {
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
                async {
                    val installationId = it.id.toString()
                    httpClient.get<GitHubRepositories>("$baseApiPath/user/installations/$installationId/repositories") {
                        header("Authorization", "Bearer $accessToken")
                        header("Accept", "application/vnd.github.machine-man-preview+json")
                    }.repositories.map { ScmRepository(it.fullName, it.permissions.toScmPermission(), installationId) }
                }
            }

            val repositories = repositoryPromises.flatMap { it.await() }
            val repoPermission = repositories.map { it.name to it.permission }.toMap()
            val installationIds = repositories.map { it.name to it.installationId }.toMap()

            val user = userPromise.await()

            UserSession(name, principal.accessToken, user.name, repoPermission, installationIds)
        }
    }

    override suspend fun getCommits(repo: Repository): List<ScmCommit> {
        val token = getAppToken(repo)
        val ghCommits = httpClient.get<List<GitHubCommit>>("$baseApiPath/repos/${repo.name}/commits") {
            header("Authorization", "Bearer $token")
        }

        return ghCommits.map { ScmCommit(it.sha, it.commit.author.name, it.commit.message) }
    }

    override suspend fun getFiles(repo: Repository, commitId: String): ScmTree {
        val token = getAppToken(repo)
        val ghTree = httpClient.get<GitHubTree>("$baseApiPath/repos/${repo.name}/git/trees/$commitId?recursive=1") {
            header("Authorization", "Bearer $token")
        }
        if (ghTree.truncated) {
            throw RuntimeException("Truncated responses not supported")
        }

        return ScmTree(ghTree.sha, ghTree.tree.map { ScmFile(it.path, it.type.toScmFileType()) })
    }

    override suspend fun getContent(repo: Repository, commitId: String, path: String): ByteArray {
        val token = getAppToken(repo)
        val content =
            httpClient.get<GitHubContent>("$baseApiPath/repos/${repo.name}/contents/$path?ref=$commitId") {
                header("Authorization", "Bearer $token")
            }

        return decoder.decode(content.content)
    }

    override suspend fun isPublic(repo: Repository): Boolean {
        val token = getAppToken(repo)
        val ghRepo =
            httpClient.get<GitHubRepository>("$baseApiPath/repos/${repo.name}") {
                header("Authorization", "Bearer $token")
            }
        return !ghRepo.private
    }

    private suspend fun getAppToken(repo: Repository): String {
        var cache = tokenCache.getIfPresent(repo.name)

        if (cache == null) {
            cache = getAppTokenInternal(repo)
            tokenCache.put(repo.name, cache)
        }

        return cache
    }

    private suspend fun getAppTokenInternal(repo: Repository): String {
        val now = Date()
        val token = JWT.create()
            .withIssuer(appId)
            .withIssuedAt(now)
            .withExpiresAt(Date(now.time + 60000)) // 10 minutes
            .sign(jwtAlgorithm)

        val installationToken =
            httpClient.post<GitHubToken>("$baseApiPath/app/installations/${repo.installationId}/access_tokens") {
                header("Authorization", "Bearer $token")
                header("Accept", "application/vnd.github.machine-man-preview+json")
            }

        return installationToken.token
    }

}

private fun loadPrivateKey(pemKey: String): Algorithm {
    val pemReader = PemReader(StringReader(pemKey))
    val rsaPk = org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(pemReader.readPemObject().content)
    val kf = KeyFactory.getInstance("RSA")
    val keySpec = RSAPrivateKeySpec(rsaPk.modulus, rsaPk.privateExponent)
    val privateKey = kf.generatePrivate(keySpec) as RSAPrivateKey

    return Algorithm.RSA256(null, privateKey)
}

private fun GitHubPermission.toScmPermission(): ScmPermission {
    return when {
        admin -> ScmPermission.ADMIN
        push -> ScmPermission.WRITE
        else -> ScmPermission.READ
    }
}

private fun String.toScmFileType(): ScmFileType {
    return when (this) {
        "tree" -> ScmFileType.DIRECTORY
        else -> ScmFileType.FILE
    }
}