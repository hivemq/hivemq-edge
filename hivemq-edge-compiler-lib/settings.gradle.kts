rootProject.name = "hivemq-edge-compiler-lib"

pluginManagement {
    includeBuild("../edge-plugins")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
