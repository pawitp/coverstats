package coverstats.server.models.github

data class GitHubUser(val login: String, val name: String)

data class GitHubInstallations(val totalCount: Int, val installations: List<GitHubInstallation>)

data class GitHubInstallation(val id: Int)

data class GitHubRepositories(val totalCount: Int, val repositories: List<GitHubRepository>)

data class GitHubRepository(val fullName: String)

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