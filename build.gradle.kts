import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    application
    kotlin("jvm") version "1.3.31"
}

group = "coverstats"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "1.2.1"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor", "ktor-server-cio", ktorVersion)
    implementation("io.ktor", "ktor-server-sessions", ktorVersion)
    implementation("io.ktor", "ktor-client-cio", ktorVersion)
    implementation("io.ktor", "ktor-client-gson", ktorVersion)
    implementation("io.ktor", "ktor-auth", ktorVersion)
    implementation("io.ktor", "ktor-freemarker", ktorVersion)
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    testCompile("junit", "junit", "4.12")
}

application {
    mainClassName = "io.ktor.server.cio.EngineMain"
}


configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}