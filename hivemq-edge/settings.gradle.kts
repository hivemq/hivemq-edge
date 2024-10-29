/*
 *
 * Copyright 2022-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

rootProject.name = "hivemq-edge"

pluginManagement {
    includeBuild("../edge-plugins")
}

includeBuild("./src/frontend") {
    name = "hivemq-edge-frontend"
}

if (file("../../hivemq-extension-sdk").exists()) {
    includeBuild("../../hivemq-extension-sdk")
} else {
    logger.warn(
        """
        ######################################################################################################
        You can not use the latest changes of or modify the hivemq-extension-sdk.
        Please checkout the hivemq-extension-sdk repository next to the hivemq-community-edition repository.
        Execute the following command from your project directory:
        git clone https://github.com/hivemq/hivemq-extension-sdk.git ../hivemq-extension-sdk
        You can also clone your fork:
        git clone https://github.com/<replace-with-your-fork>/hivemq-extension-sdk.git ../hivemq-extension-sdk
        ######################################################################################################
        """.trimIndent()
    )
}

if (file("../../hivemq-edge-extension-sdk").exists()) {
    includeBuild("../../hivemq-edge-extension-sdk")
} else {
    logger.warn(
        """
        ######################################################################################################
        You can not use the latest changes of or modify the hivemq-edge-extension-sdk.
        Please checkout the hivemq-edge-extension-sdk repository next to the hivemq-edge repository.
        Execute the following command from your project directory:
        git clone https://github.com/hivemq/hivemq-edge-extension-sdk.git ../hivemq-edge-extension-sdk
        You can also clone your fork:
        git clone https://github.com/<replace-with-your-fork>/hivemq-edge-extension-sdk.git ../hivemq-edge-extension-sdk
        ######################################################################################################
        """.trimIndent()
    )
}

if (file("../../hivemq-edge-adapter-sdk").exists()) {
    includeBuild("../../hivemq-edge-adapter-sdk")
} else {
    logger.warn(
        """
        ######################################################################################################
        You can not use the latest changes of or modify the hivemq-edge-adapter-sdk.
        Please checkout the hivemq-edge-adapter-sdk repository next to the hivemq-edge repository.
        Execute the following command from your project directory:
        git clone https://github.com/hivemq/hivemq-edge-adapter-sdk.git ../hivemq-edge-adapter-sdk
        You can also clone your fork:
        git clone https://github.com/<replace-with-your-fork>/hivemq-edge-adapter-sdk.git ../hivemq-edge-adapter-sdk
        ######################################################################################################
        """.trimIndent()
    )
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
