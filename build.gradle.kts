import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    war
    jacoco
    kotlin("jvm") version "1.3.40"
}

group = "coverstats"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "1.2.1"
val junitVersion = "5.4.2"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-guava", "1.2.1")
    implementation("io.ktor", "ktor-server-cio", ktorVersion)
    implementation("io.ktor", "ktor-server-servlet", ktorVersion)
    implementation("io.ktor", "ktor-server-sessions", ktorVersion)
    implementation("io.ktor", "ktor-client-cio", ktorVersion)
    implementation("io.ktor", "ktor-client-gson", ktorVersion)
    implementation("io.ktor", "ktor-auth", ktorVersion)
    implementation("io.ktor", "ktor-freemarker", ktorVersion)
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("com.auth0", "java-jwt", "3.8.1")
    implementation("org.bouncycastle", "bcprov-jdk15on", "1.61")
    implementation("org.apache.commons", "commons-text", "1.6")
    implementation("io.github.microutils", "kotlin-logging", "1.6.26")
    implementation("com.github.ben-manes.caffeine", "caffeine", "2.7.0")
    implementation("net.spy", "spymemcached", "2.12.3")
    implementation("com.spotify", "async-datastore-client", "3.0.2")
    implementation("com.google.api-client", "google-api-client-appengine", "1.29.2")
    testCompile(kotlin("test-junit5"))
    testCompile("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testCompile("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
}

configurations.all {
    // Required for async-datastore-client
    resolutionStrategy.force("com.google.guava:guava:23.0")
}

application {
    mainClassName = "io.ktor.server.cio.EngineMain"
}

tasks.withType<Test> {
    useJUnitPlatform()
    jacoco
}

tasks.withType<JacocoReport> {
    reports {
        xml.isEnabled = true
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}