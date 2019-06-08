package coverstats.server.datastore

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.protobuf.ByteString
import com.spotify.asyncdatastoreclient.*
import com.spotify.asyncdatastoreclient.QueryBuilder.eq
import coverstats.server.models.coverage.CoverageReport
import coverstats.server.models.datastore.Repository
import coverstats.server.utils.deserializeJson
import coverstats.server.utils.gunzip
import coverstats.server.utils.gzip
import coverstats.server.utils.serializeJson
import kotlinx.coroutines.guava.await
import java.nio.charset.StandardCharsets.UTF_8

private const val KIND_REPO = "Repository"
private const val KIND_REPORT = "Report"

/**
 * Data Store backed by Google Cloud Datastore
 */
class GoogleCloudDataStore : DataStore {

    val datastore: Datastore = Datastore.create(datastoreConfig())

    override suspend fun getRepositoryByName(scm: String, name: String): Repository? {
        val query = QueryBuilder.query(KIND_REPO, "$scm|$name")
        val result: Entity? = datastore.executeAsync(query).await().entity
        return result?.toRepository()
    }

    override suspend fun getRepositoryByToken(token: String): Repository? {
        val query = QueryBuilder.query()
            .kindOf(KIND_REPO)
            .filterBy(eq("uploadToken", token))
        val result: Entity? = datastore.executeAsync(query).await().entity
        return result?.toRepository()
    }

    override suspend fun saveRepository(repo: Repository) {
        val query = QueryBuilder.update(KIND_REPO, "${repo.scm}|${repo.name}")
            .value("scm", repo.scm, true)
            .value("name", repo.name, false)
            .value("uploadToken", repo.uploadToken, true)
            .value("installationId", repo.installationId, false)
            .value("createdAt", repo.createdAt, true)
            .upsert()
        datastore.executeAsync(query).await()
    }

    override suspend fun getReportByCommitId(scm: String, repo: String, commitId: String): CoverageReport? {
        val query = QueryBuilder.query(reportKey(scm, repo, commitId))
        val result: Entity? = datastore.executeAsync(query).await().entity
        return result?.toCoverageReport()
    }

    override suspend fun saveReport(report: CoverageReport) {
        // Store entire report as gzipped JSON. Other fields are just for reference/indexing.
        val query = QueryBuilder.update(reportKey(report.scm, report.repo, report.commitId))
            .value("scm", report.scm, true)
            .value("repo", report.repo, true)
            .value("commitId", report.commitId, false)
            .value("report", ByteString.copyFrom(report.serializeJson().toByteArray(UTF_8).gzip()), false)
            .value("createdAt", report.createdAt, true)
            .upsert()
        datastore.executeAsync(query).await()
    }

    private fun reportKey(scm: String, repo: String, commitId: String): Key {
        val parentKey = Key.builder(KIND_REPO, "$scm|${repo.replace('/', '|')}").build()
        return Key.builder(KIND_REPORT, commitId, parentKey).build()
    }

    private fun datastoreConfig(): DatastoreConfig {
        val emulatorHost = System.getenv("DATASTORE_EMULATOR_HOST")
        return if (emulatorHost != null) {
            DatastoreConfig.builder()
                .host("http://$emulatorHost")
                .build()
        } else {
            DatastoreConfig.builder()
                .credential(GoogleCredential.getApplicationDefault())
                .build()
        }
    }

}

private fun Entity.toRepository(): Repository {
    return Repository(
        getString("scm"),
        getString("name"),
        getString("uploadToken"),
        getString("installationId"),
        getDate("createdAt")
    )
}

private fun Entity.toCoverageReport(): CoverageReport =
    getBlob("report").toByteArray().gunzip().toString(UTF_8).deserializeJson(CoverageReport::class)
