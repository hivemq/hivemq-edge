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

// exclude old transitive dependency versions that are provided by edge
configurations.runtimeClasspath {
    exclude("io.netty", "netty-buffer")
    exclude("io.netty", "netty-handler")
    exclude("io.netty", "netty-codec")
    exclude("io.netty", "netty-common")
    exclude("io.netty", "netty-transport")
}

dependencies {
    compileOnly(libs.hivemq.edge.adaptersdk)
    compileOnly(libs.apache.commons.io)

    implementation(libs.plc4j.s7)
    implementation(libs.plc4j.ads)
    implementation(libs.plc4j.api)
    implementation(libs.plc4j.transport.raw.socket)

    constraints {
        implementation(libs.org.json)
    }
}

dependencies {
    testImplementation("com.hivemq:hivemq-edge")
    // hivemq-edge config entities are JAXB annotated; javac needs the annotation types to read their class files
    testCompileOnly(libs.jaxb4.bind)
    testImplementation(libs.hivemq.edge.adaptersdk)
    testImplementation(libs.plc4j.api)

    testImplementation(libs.apache.commons.io)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.assertj)
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

tasks.shadowJar {
    // ShadowJar defaults its duplicatesStrategy to EXCLUDE, and that filtering runs before the
    // service-file merge, so without this override only the first META-INF/services file of a given
    // name reaches the jar and every other provider is dropped silently. That is what used to hide
    // the ADS driver behind the S7 one, and it is why this module carried hand-written copies of
    // the PlcDriver and Transport descriptors; those are gone now that the merge does its job. The
    // override is scoped to service files, so every other duplicated resource still lands in the
    // jar exactly once.
    filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    mergeServiceFiles()
}

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
    thirdPartyLicenseDirectory.set(layout.buildDirectory.dir("reports/third-party-licenses"))
}
