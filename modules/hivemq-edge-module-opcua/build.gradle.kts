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
    // Test-only, and for one purpose: OpcUaSessionActivityListenerTest's race loop turns down the listener's
    // own logger for the duration of the loop. See that test for why -- half a million INFO lines is a
    // hundred-megabyte JUnit XML that GitHub's result parser refuses. Nothing in main uses Logback directly;
    // the adapter logs through SLF4J and the binding is the runtime's business.
    testImplementation(libs.logback.classic)
    // Test-only: the same validator Edge puts in front of a southbound write, so a published schema can be
    // checked by running a payload through the gate rather than by reading the rendered type. hivemq-edge has
    // it as an `implementation` dependency, so it reaches this module's test runtime but not its compile
    // classpath. See ConditionSchemaNullableFieldsTest for the finding that needed it.
    testImplementation(libs.json.schema.validator)
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

tasks.shadowJar {
    // ShadowJar defaults its duplicatesStrategy to EXCLUDE, and that filtering runs before the
    // service-file merge, so without this override only the first META-INF/services file of a given
    // name reaches the jar and every other provider is dropped silently. The override is scoped to
    // service files, so every other duplicated resource still lands in the jar exactly once.
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
