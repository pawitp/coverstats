package coverstats.server.models.session

data class UserSession(
    val token: String,
    val username: String,
    val fullName: String,
    val repositories: List<String>
)