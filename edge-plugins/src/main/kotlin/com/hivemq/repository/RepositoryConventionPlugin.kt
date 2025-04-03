package com.hivemq.repository

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import java.net.URI

/**
 * Convention plugin that applies common repository settings to all projects.
 * This centralizes the repository configuration to avoid duplication across build files.
 */
class RepositoryConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.repositories.applyRepositorySettings()
    }

    private fun RepositoryHandler.applyRepositorySettings() {
        mavenCentral()
        maven { url = URI.create("https://jitpack.io") }
        exclusiveContent {
            forRepository {
                maven {
                    url = URI.create("https://jitpack.io")
                }
            }
            filter {
                includeGroup("com.github.simon622.mqtt-sn")
                includeGroup("com.github.simon622")
            }
        }
    }
}
