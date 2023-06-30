import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
    id("com.github.node-gradle.node")
}

group = "com.hivemq"
description = "Frontend for HiveMQ Edge"

val buildFrontend by tasks.registering(PnpmTask::class) {
    pnpmCommand.set(listOf("build", "--base=/app"))
    dependsOn(tasks.pnpmInstall)
    inputs.dir(project.fileTree("src"))
    inputs.dir(project.fileTree("public"))
    inputs.dir("node_modules")
    inputs.files("index.html", ".env", "vite.config.ts", ".browserslistrc", "tsconfig.json", "tsconfig.node.json")
    outputs.dir("${project.projectDir}/dist")
}

/* ******************** artifacts ******************** */

val releaseBinary: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
}

artifacts {
    add(releaseBinary.name, buildFrontend)
}

node {
    download.set(true)
}
