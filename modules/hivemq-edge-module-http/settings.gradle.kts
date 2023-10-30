rootProject.name = "hivemq-edge-module-http"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../libs.versions.toml"))
        }
    }
}

includeBuild("../../hivemq-edge")
