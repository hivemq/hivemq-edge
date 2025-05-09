group = "com.hivemq"

plugins {
    idea
    id("com.hivemq.edge-version-updater")
    id("com.hivemq.repository-convention")
    id("io.github.sgtsilvio.gradle.oci") version "0.22.0"
    id("jacoco")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

// Repository settings are now applied by the repository-convention plugin

tasks.register("clean") {
    group = "build"
    //required since we don't apply the base plugin and otherwise the reports can't be cleaned out
    project.delete(files("${project.layout.buildDirectory.get()}"))
    dependsOn(gradle.includedBuilds.map { it.task(":clean") })
}

tasks.register("build") {
    group = "build"

    dependsOn(gradle.includedBuilds.map { it.task(":$name") })
}

tasks.register("license") {
    group = "license"

    dependsOn(gradle.includedBuilds.filter { it.name != "hivemq-edge-frontend" }.filter { it.name != "edge-plugins" }.map { it.task(":$name") })
}

tasks.register("check") {
    group = "verification"

    dependsOn(gradle.includedBuilds.map { it.task(":$name") })
}

tasks.register("test") {
    group = "verification"

    dependsOn(gradle.includedBuilds.filter { it.name != "hivemq-edge-frontend" }.map { it.task(":$name") })
}

tasks.register("jacocoTestReport") {
    group = "verification"

    dependsOn(gradle.includedBuilds.filter { it.name == "hivemq-edge" }.map { it.task(":$name") })
}

tasks.register("classes") {
    dependsOn(gradle.includedBuilds.filter { it.name != "hivemq-edge-frontend" }.map { it.task(":$name") })
}

tasks.register("testClasses") {
    dependsOn(gradle.includedBuilds.filter { it.name != "hivemq-edge-frontend" }.map { it.task(":$name") })
}


tasks.register<JacocoReport>("jacocoMergedReport") {
    dependsOn(gradle.includedBuilds.filter { it.name != "hivemq-edge-frontend" }.map { it.task(":test") }) // Run tests in included builds

    val executionDataFiles: FileCollection = files(gradle.includedBuilds.map { file(it.projectDir.absolutePath  + "/build/jacoco/test.exec") })
    val classFiles = files(gradle.includedBuilds.map { fileTree(it.projectDir.absolutePath  + "/build/classes/java/main") {
        include("**/*.class")
        exclude("com/hivemq/edge/api/model/**") // Exclude generated classes
    } })

    executionData.setFrom(executionDataFiles)
    classDirectories.setFrom(classFiles)

    sourceDirectories.setFrom(
        files(gradle.includedBuilds.map { file(it.projectDir.absolutePath  + "/src/main/java")})
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
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
    edgeModule("com.hivemq:hivemq-edge-module-mtconnect")
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
    "hivemq-edge-module-mtconnect",
    "hivemq-edge-module-opcua",
    "hivemq-edge-module-plc4x"
)

tasks.register("updateDependantVersions") {
    group = "other"
    dependsOn(":updateVersion")
    edgeProjectsToUpdate.forEach {
        dependsOn(gradle.includedBuild(it).task(":updateVersion"))
    }
    doLast {
        val prevVersion = project.findProperty("prevVersion") as String?
        val version = project.findProperty("version") as String?
        if(prevVersion != null && prevVersion.endsWith("-SNAPSHOT")) {
            file("ext/hivemq-edge-openapi-$prevVersion.yaml").renameTo(file("ext/hivemq-edge-openapi-$version.yaml"))
        } else {
            file("ext/hivemq-edge-openapi-$prevVersion.yaml").copyTo(file("ext/hivemq-edge-openapi-$version.yaml"))
        }
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
                runtime("library:eclipse-temurin:sha256!ea665210f431bd2da42fe40375d0f9dc500ce0d72ef7b13b5f4f1e02ba64f7e2") // eclipse-temurin:17.0.14_7-jre-noble
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
            layer("hivemq") {
                contents {
                    into("opt") {
                        filePermissions = 0b110_110_110
                        directoryPermissions = 0b111_111_111
                        permissions("**/*.sh", 0b111_111_111)
                        from("docker/docker-entrypoint.sh")
                        into("hivemq") {
                            from("./hivemq-edge/src/distribution") { filter { exclude("**/.gitkeep") } }
                            from("docker/config-k8s.xml") {
                                into("conf-k8s")
                                rename("config-k8s.xml", "config.xml")
                            }
                            from("docker/logback-k8s.xml") {
                                into("conf-k8s")
                                rename("logback-k8s.xml", "logback.xml")
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
                        filePermissions = 0b110_110_110
                        directoryPermissions = 0b111_111_111
                        into("hivemq/modules") {
                            // copy OSS modules
                            from(openSourceEdgeModuleBinaries.elements)
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
