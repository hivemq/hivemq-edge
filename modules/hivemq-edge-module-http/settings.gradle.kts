rootProject.name = "hivemq-edge-module-http"

pluginManagement {
    plugins {
        id("com.github.johnrengelman.shadow") version "${extra["plugin.shadow.version"]}"
        id("com.github.sgtsilvio.gradle.utf8") version "${extra["plugin.utf8.version"]}"
        id("com.github.hierynomus.license") version "${extra["plugin.license.version"]}"
        id("org.owasp.dependencycheck") version "${extra["plugin.dependencycheck.version"]}"
    }
}

includeBuild("../../hivemq-edge")
