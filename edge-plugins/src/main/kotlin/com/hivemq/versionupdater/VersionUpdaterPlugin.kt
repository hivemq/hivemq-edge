package com.hivemq.versionupdater

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class VersionUpdaterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register<UpdateVersionTask>(UPDATE_VERSION_TASK_NAME)

        // Workaround for a CI build-cache issue: cyclonedxDirectBom must not be served from the cache.
        //
        // The task is registered per-project by com.hivemq.tools.license, which applies the CycloneDX plugin
        // and then configures cyclonedxDirectBom against that project's runtimeClasspath. Seventeen projects
        // across six repositories apply it, and the composite root does not, so there is no single build
        // script to put this in.
        //
        // It lives here, in a plugin otherwise concerned only with version bumping, because of the six
        // com.hivemq.* convention plugins this is the only one every affected project applies --
        // repository-convention and jacoco-convention miss four of them, errorprone and nullaway three,
        // spotless two. The withId guard means this is inert wherever the license plugin is absent, which
        // includes the roots that apply this plugin without ever registering the task.
        //
        // Remove once the caching issue itself is fixed; nothing else here depends on it.
        project.plugins.withId("com.hivemq.tools.license") {
            project.tasks.named("cyclonedxDirectBom") {
                outputs.cacheIf { false }
            }
        }
    }
}

const val UPDATE_VERSION_TASK_NAME: String = "updateVersion"
const val PROPERTIES_FILES_KEY = "versionUpdaterFiles"
