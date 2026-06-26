import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED


plugins {
    java
    alias(libs.plugins.defaults)
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

// The ChaosProtocolAdapter is a hidden, test-only SDK v2 adapter: it is NOT part of any shipped distribution's module
// set and carries no production-distribution machinery (no shadow jar, no license processing). It is consumed two ways
// by the tests that drive it: as a plain library (the deterministic scenario matrix and the simulator's own unit tests
// construct it directly on a FakeClock), and — through the test-only `releaseBinary` below — as a module-loader binary,
// so the embedded end-to-end suite boots a real Edge runtime that loads it through the standard module loader. The
// chaos adapter bundles no runtime dependencies (the host provides the adapter SDK and Jackson), so its plain library
// jar already carries the classes and the v2 ProtocolAdapterFactory service file and is loadable as-is — no shadow
// distribution is needed or produced. The deterministic scenario matrix, the harness, and the simulator's own unit
// tests all live in this module's test sources.

dependencies {
    compileOnly(libs.hivemq.edge.adaptersdk)
    // Jackson annotations (transitively, compile-only): ChaosNode carries a @JsonCreator so the framework's own
    // ObjectMapper deserializes it from its node-string when a runtime loads a configured chaos adapter. Jackson is
    // provided by the host at runtime, so it is never bundled into the module jar.
    compileOnly(libs.jackson.databind)

    testImplementation(libs.hivemq.edge.adaptersdk)
    testImplementation("com.hivemq:hivemq-edge")
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

// ******************** test-only module-loader binary ********************

// The loadable module the embedded end-to-end suite consumes is the plain library jar (chaos bundles no runtime
// dependencies, so it needs no shadow distribution). This consumable variant is resolved only by `hivemq-edge-test`'s
// `chaosAdapterBinary` configuration, so the jar is packaged on demand for that test run and is never part of a
// production distribution.
val releaseBinary: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
}

artifacts {
    add(releaseBinary.name, tasks.jar)
}
