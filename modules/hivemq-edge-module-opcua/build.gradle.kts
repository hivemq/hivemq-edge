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
    compileOnly("com.hivemq:hivemq-edge")
    compileOnly(libs.hivemq.edge.adaptersdk)
    compileOnly(libs.apache.commons.io)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.jackson.databind)

    compileOnly(libs.apache.commons.lang)

    implementation(libs.milo.encoding.json)
    implementation(libs.milo.encoding.xml)
    implementation(libs.milo.client)
    implementation(libs.milo.stack.core)
    implementation(libs.milo.dtd.reader)
    implementation(libs.milo.dtd.manager)
}

dependencies {
    testImplementation("com.hivemq:hivemq-edge")
    testImplementation(libs.jackson.databind)
    testImplementation(libs.hivemq.edge.adaptersdk)
    testImplementation(libs.apache.commons.io)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.mockito.junit.jupiter)

    testImplementation(libs.milo.server)
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
}

configurations {
    runtimeClasspath {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "io.netty")
        exclude(group = "org.bouncycastle")
        exclude(group = "org.slf4j")
        exclude(group = "org.glassfish.jaxb")
        exclude(group = "com.sun.activation")
    }
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
    add(
        thirdPartyLicenses.name,
        tasks.updateThirdPartyLicenses.flatMap { it.outputDirectory }
    )
}
// ******************** compliance ********************

hivemqLicense {
    projectName.set(project.name)
    thirdPartyLicenseDirectory.set(layout.projectDirectory.dir("src/distribution/third-party-licenses"))
}
