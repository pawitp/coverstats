package coverstats.server.models.datastore

import java.util.*

data class Repository (
    val scm: String,
    val name: String,
    val uploadToken: String,
    val installationId: String,
    val createdAt: Date
)