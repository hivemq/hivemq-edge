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
    id("com.hivemq.repository-convention")
    id("com.hivemq.spotless-convention")
}

group = "com.hivemq"
description = "HiveMQ Edge Language Server — LSP server for YAML config files"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("com.hivemq:hivemq-edge-compiler")
    implementation("com.hivemq:hivemq-edge-compiler-lib")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.23.1")
    implementation(libs.slf4j.api)
    compileOnly(libs.jetbrains.annotations)
    runtimeOnly(libs.logback.classic)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(STARTED, PASSED, FAILED, SKIPPED, STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.shadowJar {
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "com.hivemq.edge.lsp.HiveMQEdgeLspLauncher"
    }
}
