package coverstats.server.scm

import coverstats.server.cache.Cache
import coverstats.server.cache.cached
import coverstats.server.models.datastore.Repository
import coverstats.server.models.scm.ScmCommit
import coverstats.server.models.scm.ScmTree
import coverstats.server.models.session.UserSession
import io.ktor.auth.OAuthAccessTokenResponse
import io.ktor.auth.OAuthServerSettings

class CachingScmProvider(private val underlying: ScmProvider, private val cache: Cache) : ScmProvider {
    override val name: String get() = underlying.name

    override val oAuthServerSettings: OAuthServerSettings get() = underlying.oAuthServerSettings

    override suspend fun processOAuth(principal: OAuthAccessTokenResponse.OAuth2): UserSession {
        return underlying.processOAuth(principal)
    }

    override suspend fun getCommits(repo: Repository): List<ScmCommit> {
        return underlying.getCommits(repo)
    }

    override suspend fun getFiles(repo: Repository, commitId: String): ScmTree {
        return cache.cached("s:$name:files:$commitId", 60 * 60 * 1000) {
            underlying.getFiles(repo, commitId)
        }
    }

    override suspend fun getContent(repo: Repository, commitId: String, path: String): ByteArray {
        return underlying.getContent(repo, commitId, path)
    }

    override suspend fun isPublic(repo: Repository): Boolean {
        return cache.cached("s:$name:public:${repo.name}", 5 * 60 * 1000) {
            underlying.isPublic(repo)
        }
    }

}