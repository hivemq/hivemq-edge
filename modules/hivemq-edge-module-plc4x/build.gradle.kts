import nl.javadude.gradle.plugins.license.DownloadLicensesExtension.license
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    id("java")
    id("com.github.sgtsilvio.gradle.utf8")
    id("com.github.johnrengelman.shadow")
    id("com.github.hierynomus.license")
    id("org.owasp.dependencycheck")
}

group = "com.hivemq"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://jitpack.io")
            }
        }
        filter {
            includeGroup("com.github.simon622.mqtt-sn")
            includeGroup("com.github.simon622")
        }
    }
}

dependencies {
    compileOnly("com.hivemq:hivemq-edge")
    compileOnly("com.hivemq:hivemq-extension-sdk")
    implementation("org.apache.plc4x:plc4j-api:${property("org.apache.plc4x.version")}")
    implementation("org.apache.plc4x:plc4j-driver-s7:${property("org.apache.plc4x.version")}")
    implementation("org.apache.plc4x:plc4j-driver-ads:${property("org.apache.plc4x.version")}")
    implementation("org.apache.plc4x:plc4j-driver-ads:${property("org.apache.plc4x.version")}")
    implementation("org.apache.plc4x:plc4j-driver-eip:${property("org.apache.plc4x.version")}")
    implementation("org.apache.plc4x:plc4j-driver-ab-eth:${property("org.apache.plc4x.version")}")
    implementation("org.apache.plc4x:plc4j-transport-raw-socket:${property("org.apache.plc4x.version")}")

//    implementation("org.apache.plc4x:plc4j-driver-bacnet:${property("org.apache.plc4x.version")}")
//    implementation("org.apache.plc4x:plc4j-driver-profinet:${property("org.apache.plc4x.version")}")

    runtimeOnly("com.google.guava:guava:${property("guava.version")}") {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.errorprone", "error_prone_annotations")
    }
}

configurations {
    runtimeClasspath {
        exclude(group = "com.google.guava")
        exclude(group = "io.netty")
        exclude(group = "io.dropwizard.metrics")
        exclude(group = "org.slf4j")
    }
}

dependencies {
    testImplementation("com.hivemq:hivemq-edge")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit.jupiter.version")}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${property("junit.jupiter.version")}")
    testImplementation("org.junit.platform:junit-platform-launcher:${property("junit.jupiter.platform.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit.jupiter.version")}")
    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
    testImplementation("org.mockito:mockito-junit-jupiter:${property("mockito.version")}")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(STARTED, PASSED, FAILED, SKIPPED, STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.register<Copy>("copyAllDependencies") {
    shouldRunAfter("assemble")
    from(configurations.runtimeClasspath)
    into("${buildDir}/deps/libs")
}

tasks.named("assemble") { finalizedBy("copyAllDependencies") }

/* ******************** artifacts ******************** */

val releaseBinary: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
}

artifacts {
    add(releaseBinary.name, tasks.shadowJar)
}

/* ******************** compliance ******************** */

license {
    header = file("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}

downloadLicenses {
    aliases = mapOf(
        license("Apache License, Version 2.0", "https://opensource.org/licenses/Apache-2.0") to listOf(
            "Apache 2",
            "Apache 2.0",
            "Apache-2.0",
            "Apache License 2.0",
            "Apache License, 2.0",
            "Apache License v2.0",
            "Apache License, Version 2",
            "Apache License Version 2.0",
            "Apache License, Version 2.0",
            "Apache License, version 2.0",
            "The Apache License, Version 2.0",
            "Apache Software License - Version 2.0",
            "Apache Software License, version 2.0",
            "The Apache Software License, Version 2.0"
        ),
        license("MIT License", "https://opensource.org/licenses/MIT") to listOf(
            "MIT License",
            "MIT license",
            "The MIT License",
            "The MIT License (MIT)"
        ),
        license("CDDL, Version 1.0", "https://opensource.org/licenses/CDDL-1.0") to listOf(
            "CDDL, Version 1.0",
            "Common Development and Distribution License 1.0",
            "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0",
            license("CDDL", "https://glassfish.dev.java.net/public/CDDLv1.0.html")
        ),
        license("CDDL, Version 1.1", "https://oss.oracle.com/licenses/CDDL+GPL-1.1") to listOf(
            "CDDL 1.1",
            "CDDL, Version 1.1",
            "Common Development And Distribution License 1.1",
            "CDDL+GPL License",
            "CDDL + GPLv2 with classpath exception",
            "Dual license consisting of the CDDL v1.1 and GPL v2",
            "CDDL or GPLv2 with exceptions",
            "CDDL/GPLv2+CE"
        ),
        license("LGPL, Version 2.0", "https://opensource.org/licenses/LGPL-2.0") to listOf(
            "LGPL, Version 2.0",
            "GNU General Public License, version 2"
        ),
        license("LGPL, Version 2.1", "https://opensource.org/licenses/LGPL-2.1") to listOf(
            "LGPL, Version 2.1",
            "LGPL, version 2.1",
            "GNU Lesser General Public License version 2.1 (LGPLv2.1)",
            license("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
        ),
        license("LGPL, Version 3.0", "https://opensource.org/licenses/LGPL-3.0") to listOf(
            "LGPL, Version 3.0",
            "Lesser General Public License, version 3 or greater"
        ),
        license("EPL, Version 1.0", "https://opensource.org/licenses/EPL-1.0") to listOf(
            "EPL, Version 1.0",
            "Eclipse Public License - v 1.0",
            "Eclipse Public License - Version 1.0",
            license("Eclipse Public License", "http://www.eclipse.org/legal/epl-v10.html")
        ),
        license("EPL, Version 2.0", "https://opensource.org/licenses/EPL-2.0") to listOf(
            "EPL 2.0",
            "EPL, Version 2.0"
        ),
        license("EDL, Version 1.0", "https://www.eclipse.org/org/documents/edl-v10.php") to listOf(
            "EDL 1.0",
            "EDL, Version 1.0",
            "Eclipse Distribution License - v 1.0"
        ),
        license("BSD 3-Clause License", "https://opensource.org/licenses/BSD-3-Clause") to listOf(
            "BSD 3-clause",
            "BSD-3-Clause",
            "BSD 3-Clause License",
            "3-Clause BSD License",
            "New BSD License",
            license("BSD", "http://asm.ow2.org/license.html"),
            license("BSD", "http://asm.objectweb.org/license.html"),
            license("BSD", "LICENSE.txt")
        ),
        license("Bouncy Castle License", "https://www.bouncycastle.org/licence.html") to listOf(
            "Bouncy Castle Licence"
        ),
        license("W3C License", "https://opensource.org/licenses/W3C") to listOf(
            "W3C License",
            "W3C Software Copyright Notice and License",
            "The W3C Software License"
        ),
        license("CC0", "https://creativecommons.org/publicdomain/zero/1.0/") to listOf(
            "CC0",
            "Public Domain"
        )
    )

    dependencyConfiguration = "runtimeClasspath"
    excludeDependencies = listOf("com.hivemq:hivemq-extension-sdk:${property("hivemq-extension-sdk.version")}")
}

val updateThirdPartyLicenses by tasks.registering {
    group = "license"
    dependsOn(tasks.downloadLicenses)
    doLast {
        javaexec {
            classpath("gradle/tools/license-third-party-tool-2.0.jar")
            args(
                "$buildDir/reports/license/dependency-license.xml",
                "$projectDir/src/distribution/third-party-licenses/licenses",
                "$projectDir/src/distribution/third-party-licenses/licenses.html"
            )
        }
    }
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations.shadowRuntimeElements.get()) {
    skip()
}
