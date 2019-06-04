package coverstats.server.models.scm

enum class ScmPermission {
    ADMIN, WRITE, READ
}

data class ScmCommit(
    val commitId: String,
    val author: String,
    val message: String
)

enum class ScmFileType {
    FILE, DIRECTORY
}

data class ScmTree(
    val commitId: String,
    val files: List<ScmFile>
)

data class ScmFile(
    val path: String,
    val type: ScmFileType
)

data class ScmRepository(
    val name: String,
    val permission: ScmPermission,
    val installationId: String
)