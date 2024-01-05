group = "com.hivemq"

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
	edgeModule("com.hivemq:hivemq-edge-module-http")
    // ** module-deps ** //
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

