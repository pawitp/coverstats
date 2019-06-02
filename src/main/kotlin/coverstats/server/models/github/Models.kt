package coverstats.server.models.github

data class GitHubUser(val login: String, val name: String)

data class GitHubInstallations(val totalCount: Int, val installations: List<GitHubInstallation>)

data class GitHubInstallation(val id: Int)

data class GitHubRepositories(val totalCount: Int, val repositories: List<GitHubRepository>)

data class GitHubRepository(val fullName: String)
