package com.hivemq.versionupdater

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class VersionUpdaterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register<UpdateVersionTask>(UPDATE_VERSION_TASK_NAME)
    }
}

const val UPDATE_VERSION_TASK_NAME: String = "updateVersion"
const val PROPERTIES_FILES_KEY = "versionUpdaterFiles"
