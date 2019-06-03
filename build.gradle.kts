import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    war
    jacoco
    kotlin("jvm") version "1.3.31"
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
    implementation("io.ktor", "ktor-server-cio", ktorVersion)
    implementation("io.ktor", "ktor-server-servlet", ktorVersion)
    implementation("io.ktor", "ktor-server-sessions", ktorVersion)
    implementation("io.ktor", "ktor-client-cio", ktorVersion)
    implementation("io.ktor", "ktor-client-gson", ktorVersion)
    implementation("io.ktor", "ktor-auth", ktorVersion)
    implementation("io.ktor", "ktor-freemarker", ktorVersion)
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    testCompile(kotlin("test-junit5"))
    testCompile("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testCompile("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
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