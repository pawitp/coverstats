package coverstats.server.models.datastore

data class Repository (
    val scm: String,
    val name: String,
    val uploadToken: String,
    val installationId: String
)