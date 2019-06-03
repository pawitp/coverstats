package coverstats.server.models.scm

data class ScmCommit(
    val commitId: String,
    val author: String,
    val message: String
)