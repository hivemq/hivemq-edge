import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED


plugins {
    java
    alias(libs.plugins.defaults)
    alias(libs.plugins.shadow)
    alias(libs.plugins.hivemq.license)
    id("com.hivemq.edge-version-updater")
    id("com.hivemq.repository-convention")
    id("com.hivemq.jacoco-convention")
    id("com.hivemq.errorprone-convention")
    id("com.hivemq.nullaway-convention")
    id("com.hivemq.spotless-convention")
}

group = "com.hivemq"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// The v2 Databases adapter is a production, listable SDK v2 adapter. Unlike the File and HTTP v2 adapters, its shadow
// jar DOES bundle runtime dependencies: the PostgreSQL, MariaDB, and MSSQL JDBC drivers plus the HikariCP connection
// pool are shaded into the module jar, so the isolated module classloader can register the drivers at connect time.
// The host still provides the adapter SDK and Jackson.

dependencies {
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.mariadb)
    implementation(libs.mssql)
    compileOnly(libs.hivemq.edge.adaptersdk)
    // Jackson: DatabaseNode carries a @JsonCreator so the framework's own ObjectMapper deserializes it from its
    // node-string, and the poll path converts result-set rows to JSON nodes. Jackson is provided by the host at
    // runtime, so it is never bundled into the module jar.
    compileOnly(libs.jackson.databind)
    compileOnly(libs.slf4j.api)

    testImplementation(libs.hivemq.edge.adaptersdk)
    testImplementation(libs.jackson.databind)
    testImplementation("com.hivemq:hivemq-edge")
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
    testImplementation(libs.mockito.junit.jupiter)
    // Plain Testcontainers only: the engine-specific modules (postgresql/mysql/mssqlserver) live in the
    // hivemq-edge-test catalog, not here — the wrapper tests run the engines as GenericContainers and gate
    // readiness through the same JDBC drivers the adapter bundles.
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(STARTED, PASSED, FAILED, SKIPPED, STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.register<Copy>("copyAllDependencies") {
    shouldRunAfter("assemble")
    from(provider { configurations.runtimeClasspath.get() })
    into("${layout.buildDirectory}/deps/libs")
}

tasks.named("assemble") { finalizedBy("copyAllDependencies") }

// ******************** artifacts ********************

val releaseBinary: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
}

val thirdPartyLicenses: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("third-party-licenses"))
    }
}

artifacts {
    add(releaseBinary.name, tasks.shadowJar)
    add(thirdPartyLicenses.name, tasks.updateThirdPartyLicenses.flatMap { it.outputDirectory })
}

// ******************** compliance ********************

hivemqLicense {
    projectName.set(project.name)
    thirdPartyLicenseDirectory.set(layout.buildDirectory.dir("reports/third-party-licenses"))
    ignoredGroupPrefixes.add("org.mariadb.jdbc")
}
