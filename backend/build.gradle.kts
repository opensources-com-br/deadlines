plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.deadlines"
version = "0.1.0"

kotlin {
    jvmToolchain(21)

    sourceSets.test {
        kotlin.srcDir("../tests/integration")
    }
}

application {
    mainClass = "deadlines.application.ApplicationKt"
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.database)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.test {
    useJUnitPlatform()
}
