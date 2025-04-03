plugins {
    `kotlin-dsl`
}

group = "com.hivemq"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
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
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
