plugins {
    `kotlin-dsl`
}

group = "com.hivemq"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.jackson.dataformat.xml)
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:${libs.versions.plugin.errorprone.get()}")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.2")
}

gradlePlugin {
    plugins {
        create("edge-version-updater") {
            id = "$group.$name"
            implementationClass = "$group.versionupdater.VersionUpdaterPlugin"
        }
        create("third-party-license-generator") {
            id = "$group.$name"
            implementationClass = "$group.licensethirdparty.ThirdPartyLicenseGeneratorPlugin"
        }
        create("repository-convention") {
            id = "$group.$name"
            implementationClass = "$group.repository.RepositoryConventionPlugin"
        }
        create("jacoco-convention") {
            id = "$group.$name"
            implementationClass = "$group.jacoco.JacocoConventionPlugin"
        }
        create("errorprone-convention") {
            id = "$group.$name"
            implementationClass = "$group.errorprone.ErrorProneConventionPlugin"
        }
        create("spotless-convention") {
            id = "$group.$name"
            implementationClass = "$group.spotless.SpotlessConventionPlugin"
        }
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
