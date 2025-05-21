import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
  id("com.github.node-gradle.node")
}

group = "com.hivemq"
description = "Frontend for HiveMQ Edge"

node {
  download.set(true)
  version.set("18.20.6")
  pnpmVersion.set("9")
}

tasks.withType<PnpmTask>().configureEach {
  environment = mapOf(
    "VITE_HIVEMQ_EDGE_VERSION" to project.property("version").toString(),
  )
}

val buildFrontend by tasks.registering(PnpmTask::class) {
  environment = mapOf(
    "NODE_OPTIONS" to "--max-old-space-size=4096",
  )
  pnpmCommand.set(listOf("build", "--base=/app"))
  dependsOn(tasks.pnpmInstall)
  inputs.dir(project.fileTree("src"))
  inputs.dir(project.fileTree("public"))
  inputs.dir("node_modules")
  inputs.files("index.html", ".env", "vite.config.ts", "tsconfig.json", "tsconfig.node.json")
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
