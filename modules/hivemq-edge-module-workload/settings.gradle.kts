rootProject.name = "hivemq-edge-module-workload"

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

includeBuild("../../../hivemq-edge-adapter-sdk")
