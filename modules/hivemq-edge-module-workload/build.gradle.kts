plugins {
    java
    alias(libs.plugins.defaults)
    id("com.hivemq.repository-convention")
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
}
