package com.hivemq.jacoco

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * Convention plugin that applies the Jacoco plugin and configures the jacocoTestReport task.
 * This centralizes the Jacoco configuration to avoid duplication across build files.
 */
class JacocoConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Apply the jacoco plugin
        project.plugins.apply(JacocoPlugin::class)
        
        // Configure the jacocoTestReport task
        project.tasks.withType(JacocoReport::class.java).configureEach {
            dependsOn(project.tasks.named("test"))
            reports {
                xml.required.set(true)
            }
        }
    }
}
