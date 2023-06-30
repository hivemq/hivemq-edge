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
    plugins {
        id("com.github.johnrengelman.shadow") version "${extra["plugin.shadow.version"]}"
        id("com.github.hierynomus.license") version "${extra["plugin.license.version"]}"
        id("org.owasp.dependencycheck") version "${extra["plugin.dependencycheck.version"]}"
        id("com.github.spotbugs") version "${extra["plugin.spotbugs.version"]}"
        id("de.thetaphi.forbiddenapis") version "${extra["plugin.forbiddenapis.version"]}"
        id("com.github.sgtsilvio.gradle.utf8") version "${extra["plugin.utf8.version"]}"
        id("com.github.sgtsilvio.gradle.metadata") version "${extra["plugin.metadata.version"]}"
        id("com.github.sgtsilvio.gradle.javadoc-links") version "${extra["plugin.javadoc-links.version"]}"
        id("io.github.gradle-nexus.publish-plugin") version "${extra["plugin.nexus-publish.version"]}"
        id("com.github.breadmoirai.github-release") version "${extra["plugin.github-release.version"]}"
        id("com.github.ben-manes.versions") version "${extra["plugin.versions.version"]}"
        id("com.github.node-gradle.node") version "${extra["plugin.node.version"]}"
    }
}

includeBuild("./src/frontend"){
    name="hivemq-edge-frontend"
}

if (file("../../hivemq-extension-sdk").exists()) {
    includeBuild("../../hivemq-extension-sdk")
} else {
    logger.warn(
        """
        ######################################################################################################
        You can not use the latest changes of or modify the hivemq-extension-sdk.
        Please checkout the hivemq-extension-sdk repository next to the hivemq-edge repository.
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
