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

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://jitpack.io")
            }
        }
        filter {
            includeGroup("com.github.simon622.mqtt-sn")
            includeGroup("com.github.simon622")
        }
    }
}

dependencies {
    implementation(libs.hikari)
    compileOnly(libs.hivemq.edge.adaptersdk)
    compileOnly(libs.apache.commons.io)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.slf4j.api)
    implementation(libs.postgresql)
    implementation(libs.mariadb)
    implementation(libs.mssql)
}

dependencies {
    testImplementation("com.hivemq:hivemq-edge")
    testImplementation(libs.jackson.databind)
    testImplementation(libs.hivemq.edge.adaptersdk)
    testImplementation(libs.apache.commons.io)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.milo.server)
    testImplementation(libs.assertj)
    testImplementation(libs.awaitility)
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
    from(configurations.runtimeClasspath)
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
    ignoredGroupPrefixes.add("org.mariadb.jdbc")
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations.shadowRuntimeElements.get()) {
    skip()
}
