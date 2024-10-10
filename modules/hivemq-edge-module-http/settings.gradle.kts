rootProject.name = "hivemq-edge-module-http"

pluginManagement {
    includeBuild("../../edge-plugins")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
