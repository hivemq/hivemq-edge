rootProject.name = "hivemq-edge-module-opcua"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../libs.versions.toml"))
        }
    }
}

includeBuild("../../hivemq-edge")
