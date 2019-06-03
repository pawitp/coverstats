package coverstats.server.models.scm

enum class ScmPermission {
    ADMIN, WRITE, READ
}

data class ScmCommit(
    val commitId: String,
    val author: String,
    val message: String
)

data class ScmFile(
    val path: String,
    val isDirectory: Boolean
)

data class ScmRepository(
    val name: String,
    val permission: ScmPermission,
    val installationId: String
)