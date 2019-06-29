package coverstats.server.models.github

import java.util.*

data class GitHubUser(val login: String, val name: String?)

data class GitHubInstallations(val totalCount: Int, val installations: List<GitHubInstallation>)

data class GitHubInstallation(val id: Int)

data class GitHubRepositories(val totalCount: Int, val repositories: List<GitHubRepository>)

data class GitHubRepository(
    val fullName: String,
    val permissions: GitHubPermission,
    val private: Boolean
)

data class GitHubCommit(
    val sha: String,
    val commit: GitHubCommitDetail
)

data class GitHubCommitDetail(
    val author: GitHubAuthor,
    val message: String
)

data class GitHubAuthor(
    val name: String
)

data class GitHubPermission(
    val admin: Boolean,
    val push: Boolean,
    val pull: Boolean
)

data class GitHubTree(
    val sha: String,
    val tree: List<GitHubFile>,
    val truncated: Boolean
)

data class GitHubFile(
    val path: String,
    val type: String
)

data class GitHubToken(
    val token: String,
    val expiresAt: Date
)

data class GitHubCommitChanges(
    val files: List<GitHubFileChanges>
)

data class GitHubFileChanges(
    val filename: String,
    val patch: String
)

data class GitHubContent(
    val content: String
)

data class GitHubStatus(
    val state: String,
    val targetUrl: String,
    val description: String,
    val context: String
)
