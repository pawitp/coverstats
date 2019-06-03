package coverstats.server.models.session

import coverstats.server.models.scm.ScmPermission

data class UserSession(
    val scm: String,
    val token: String,
    val username: String,
    val fullName: String,
    val repositories: Map<String, ScmPermission>
)