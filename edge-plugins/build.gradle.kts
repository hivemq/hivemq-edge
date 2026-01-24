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
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
