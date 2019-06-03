package coverstats.server.models.scm

enum class ScmPermission {
    ADMIN, WRITE, READ
}

data class ScmCommit(
    val commitId: String,
    val author: String,
    val message: String
)