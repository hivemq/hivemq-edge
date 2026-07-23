plugins {
    java
    alias(libs.plugins.defaults)
    id("com.hivemq.repository-convention")
    id("com.hivemq.spotless-convention")
}

group = "com.hivemq"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// The Workload Testing Adapter is a QA-owned, self-driving SDK-v2 device simulator: it generates realistic data
// streams (waveforms) and runs an autonomous fault timeline on the real wall clock, driven entirely by its own
// <adapter-configuration> at boot (no hot-reload, no in-JVM harness). Like the chaos module it bundles no runtime
// dependencies — the host provides the adapter SDK and Jackson — so its plain library jar is loadable as-is.
dependencies {
    compileOnly(libs.hivemq.edge.adaptersdk)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.slf4j.api)

    // Unit layer (the chaos-module pattern): fast, engine-free tests of the module's own machinery — scenario parsing
    // (incl. the whole-object "*" shadowing rule), verb→callback mapping, wave math, and control-channel gate
    // semantics. The wired e2e (hivemq-edge-test) and the black-box QA suite (hivemq-testsuite) sit above this layer.
    testImplementation(libs.hivemq.edge.adaptersdk)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.slf4j.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj)
}

tasks.test {
    useJUnitPlatform()
}

// Test-only consumable variant (the chaos-module pattern): exposes the plain library jar so `hivemq-edge-test`'s
// embedded end-to-end suite can boot a real Edge runtime that loads this module through the standard module loader.
// The workload adapter bundles no runtime dependencies (the host provides the adapter SDK, Jackson, and slf4j), so the
// plain jar already carries the classes and the v2 ProtocolAdapterFactory service file and is loadable as-is. Never
// part of a production distribution.
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
