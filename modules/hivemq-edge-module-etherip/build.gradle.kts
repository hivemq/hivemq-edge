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
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly(libs.hivemq.edge.adaptersdk)
    compileOnly(libs.apache.commons.io)
    compileOnly(libs.apache.commons.lang)
    implementation(files("libs/etherip-1.0.0.jar"))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
}

dependencies {
    testImplementation("com.hivemq:hivemq-edge")
    testImplementation(libs.apache.commons.io)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("requiresVpn")
    }
    testLogging {
        events = setOf(STARTED, PASSED, FAILED, SKIPPED, STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.register<Test>("vpnTests") {
    useJUnitPlatform {
        includeTags("requiresVpn")
    }
    shouldRunAfter("test")
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
    thirdPartyLicenseDirectory.set(layout.projectDirectory.dir("src/distribution/third-party-licenses"))
}
