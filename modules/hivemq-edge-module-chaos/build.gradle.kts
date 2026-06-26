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

// The ChaosProtocolAdapter is a hidden, test-only SDK v2 adapter: it is NOT shipped and not
// loaded via the module loader, so this module produces a plain library jar — no shadow distribution. It is
// consumed as a plain library by the tests that drive it, which inject the factory into the constructor-injected
// ProtocolAdapterFactoryRegistry as a hidden type (the production registry stays empty, D8). The deterministic
// scenario matrix, the harness, and the simulator's own unit tests all live in this module's test sources.

dependencies {
    compileOnly(libs.hivemq.edge.adaptersdk)

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
