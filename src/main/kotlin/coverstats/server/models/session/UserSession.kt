package coverstats.server.models.session

import coverstats.server.models.scm.ScmPermission

data class UserSession(
    val scm: String,
    val token: String,
    val username: String,
    val fullName: String,

    // We need these two maps because session does not support objects
    val repositories: Map<String, ScmPermission>,
    val installationIds: Map<String, String>
)