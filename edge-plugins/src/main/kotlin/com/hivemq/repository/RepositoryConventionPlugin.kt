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
        project.repositories.applyRepositorySettings(project)
    }

    private fun RepositoryHandler.applyRepositorySettings(project: Project) {
        mavenCentral()
        maven { url = URI.create("https://jitpack.io") }
        exclusiveContent {
            forRepository {
                maven {
                    name = "hivemqEdgeMqttSn"
                    url = URI.create("https://maven.pkg.github.com/hivemq/hivemq-edge-mqtt-sn")
                    credentials {
                        // findProperty -> null when absent, so configuration never fails for
                        // projects that don't resolve mqtt-sn; resolution fails clearly only when
                        // an org.slj / org.mqtt-sn artifact is actually requested without creds.
                        username = project.findProperty("hivemqCommonsUsername") as String?
                        password = project.findProperty("hivemqCommonsPassword") as String?
                    }
                }
            }
            filter {
                includeGroup("org.mqtt-sn")
                includeGroup("org.slj")
            }
        }
    }
}
