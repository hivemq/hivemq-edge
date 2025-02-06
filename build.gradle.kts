group = "com.hivemq"

plugins {
    id("com.hivemq.edge-version-updater")
    id("io.github.sgtsilvio.gradle.oci") version "0.20.2"
}

tasks.register("clean") {
    group = "build"

    gradle.includedBuilds.forEach {
        dependsOn(it.task(":$name"))
    }
}

tasks.register("build") {
    group = "build"

    gradle.includedBuilds.forEach {
        dependsOn(it.task(":$name"))
    }
}

tasks.register("check") {
    group = "verification"

    gradle.includedBuilds.forEach {
        dependsOn(it.task(":$name"))
    }
}

tasks.register("test") {
    group = "verification"

    gradle.includedBuilds.forEach {
        dependsOn(it.task(":$name"))
    }
}

tasks.register("classes") {
    gradle.includedBuilds.forEach {
        dependsOn(it.task(":$name"))
    }
}

tasks.register("testClasses") {
    gradle.includedBuilds.forEach {
        dependsOn(it.task(":$name"))
    }
}

/* ******************** release tasks ******************** */

val hivemq: Configuration by configurations.creating { isCanBeConsumed = false; isCanBeResolved = false }

val hivemqReleaseBinary: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
    extendsFrom(hivemq)
}

val edgeModule: Configuration by configurations.creating { isCanBeConsumed = false; isCanBeResolved = false }

val moduleReleaseBinaries: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
    extendsFrom(edgeModule)
}

dependencies {
    hivemq("com.hivemq:hivemq-edge")
    // ** module-deps ** //
    edgeModule("com.hivemq:hivemq-edge-module-etherip")
    edgeModule("com.hivemq:hivemq-edge-module-file")
    edgeModule("com.hivemq:hivemq-edge-module-http")
    edgeModule("com.hivemq:hivemq-edge-module-plc4x")
    edgeModule("com.hivemq:hivemq-edge-module-opcua")
    edgeModule("com.hivemq:hivemq-edge-module-modbus")
}

val hivemqEdgeZip by tasks.registering(Zip::class) {

    group = "distribution"
    description = "Bundles the complete distribution including modules"
    archiveFileName.set("hivemq-edge-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(hivemqReleaseBinary.elements.map { zipTree(it.first()) })

    into("hivemq-edge-${project.version}") {
        into("modules") {
            from(moduleReleaseBinaries.elements)
        }
    }
}

val edgeProjectsToUpdate = setOf(
    "hivemq-edge",
    "hivemq-edge-module-etherip",
    "hivemq-edge-module-file",
    "hivemq-edge-module-http",
    "hivemq-edge-module-modbus",
    "hivemq-edge-module-opcua",
    "hivemq-edge-module-plc4x"
)

tasks.register("updateDependantVersions") {
    group = "other"
    dependsOn(":updateVersion")
    edgeProjectsToUpdate.forEach {
        dependsOn(gradle.includedBuild(it).task(":updateVersion"))
    }
}


val openSourceEdgeModuleBinaries: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
    extendsFrom(edgeModule)
}

val hivemqEdgeJarRelease: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("jar"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
    extendsFrom(hivemq)
}

/* ******************** Docker ******************** */

oci {
    registries {
        dockerHub {
            optionalCredentials()
        }
    }
    imageDefinitions.register("main") {
        imageName.set("hivemq/hivemq-edge")
        allPlatforms {
            dependencies {
                runtime("library:eclipse-temurin:sha256!ebeb51a2a147be42b7d42342fecbeb2d9cb764f7742054024ac9a17bc1c8a21b") // 21.0.5_11-jre-jammy
            }
            config {
                user = "10000"
                ports = setOf("1883", "2442", "8080")
                environment = mapOf(
                    "JAVA_OPTS" to "-XX:+UnlockExperimentalVMOptions -XX:+UseNUMA",
                    "HIVEMQ_ALLOW_ALL_CLIENTS" to "true",
                    "LANG" to "en_US.UTF-8",
                )
                entryPoint = listOf("/opt/docker-entrypoint.sh")
                arguments = listOf("/opt/hivemq/bin/run.sh")
                volumes = setOf("/opt/hivemq/data", "/opt/hivemq/log")
                workingDirectory = "/opt/hivemq"
            }
            layers {
                layer("hivemq") {
                    contents {
                        into("opt") {
                            filePermissions = 0b110_110_000
                            directoryPermissions = 0b111_111_000
                            permissions("**/*.sh", 0b111_111_000)
                            from("docker/docker-entrypoint.sh")
                            into("hivemq") {
                                from("./hivemq-edge/src/distribution") { filter { exclude("**/.gitkeep") } }
                                from("docker/config-k8s.xml") {
                                    into("conf-k8s")
                                    rename("config-k8s.xml", "config.xml")
                                }
                                from("./docker/config.xml") { into("conf") }
                                from("./hivemq-edge/src/main/resources/config.xsd") { into("conf") }
                                from(hivemqEdgeJarRelease) { into("bin").rename(".*", "hivemq.jar") }
                            }
                        }
                    }
                }
                layer("open-source-modules") {
                    contents {
                        into("opt") {
                            filePermissions = 0b110_110_000
                            directoryPermissions = 0b111_111_000
                            into("hivemq/modules") {
                                // copy OSS modules
                                from(openSourceEdgeModuleBinaries.elements)
                            }
                        }
                    }
                }

            }
        }
        specificPlatform(platform("linux", "amd64"))
        specificPlatform(platform("linux", "arm64", "v8"))
        specificPlatform(platform("linux", "arm", "v7"))
    }
}



