plugins {
    `kotlin-dsl`
}

group = "com.hivemq"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jackson.dataformat.xml)
}

gradlePlugin {
    plugins {
        create("edge-version-updater") {
            id = "$group.$name"
            implementationClass = "$group.versionupdater.VersionUpdaterPlugin"
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
