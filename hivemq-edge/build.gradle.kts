import nl.javadude.gradle.plugins.license.DownloadLicensesExtension.license
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.shadow)
    alias(libs.plugins.utf8)
    alias(libs.plugins.metadata)
    alias(libs.plugins.javadocLinks)
    alias(libs.plugins.githubRelease)
    alias(libs.plugins.hierynomusLicense)
    alias(libs.plugins.dependencycheck)
    alias(libs.plugins.benManes)

    /* Code Quality Plugins */
    jacoco
    pmd
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.forbiddenapis)
    alias(libs.plugins.swagger)
}

group = "com.hivemq"
description = "HiveMQ Edge is a Gateway exposing MQTT and Edge services"

metadata {
    readableName.set("HiveMQ Edge")
    organization {
        name.set("HiveMQ GmbH")
        url.set("https://www.hivemq.com/")
    }
    license {
        apache2()
    }
    developers {
        register("cschaebe") {
            fullName.set("Christoph Schaebel")
            email.set("christoph.schaebel@hivemq.com")
        }
        register("simon622") {
            fullName.set("Simon Johnson")
            email.set("simon.johnson@hivemq.com")
        }
        register("lbrandl") {
            fullName.set("Lukas Brandl")
            email.set("lukas.brandl@hivemq.com")
        }
        register("flimpoeck") {
            fullName.set("Florian Limpoeck")
            email.set("florian.limpoeck@hivemq.com")
        }
        register("sauroter") {
            fullName.set("Georg Held")
            email.set("georg.held@hivemq.com")
        }
        register("SgtSilvio") {
            fullName.set("Silvio Giebl")
            email.set("silvio.giebl@hivemq.com")
        }
    }
    github {
        org.set("hivemq")
        repo.set("hivemq-edge")
        issues()
    }
}

/* ******************** java ******************** */

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
}


/* ******************** dependencies ******************** */

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
        }
    }
}


// Runtime stuffs
dependencies {

    api(libs.bundles.hivemq)
    implementation(libs.bundles.netty)
    implementation(libs.bundles.logs)
    implementation(libs.bundles.security)
    implementation(libs.bundles.metrics)
    runtimeOnly(libs.metrics.logback)

    // override transitive dependencies that have security vulnerabilities
    constraints {
        implementation(libs.kotlin.stdlib)
        implementation(libs.commons.compress)
    }

    // config
    implementation(libs.jakarta.xml.bind.api)
    runtimeOnly(libs.jaxb.impl)

    // dependency injection
    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)

    implementation(libs.annotation.api)

    // common
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.guava) {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.errorprone", "error_prone_annotations")
    }
    implementation(libs.future.converter.java8.guava)

    // com.google.code.findbugs:jsr305 (transitive dependency of com.google.guava:guava) is used in imports
    implementation(libs.zero.allocation.hashing)
    implementation(libs.jackson.databind)
    implementation(libs.jctools.core)

    //mqtt-sn codec
    implementation(libs.mqtt.sn.codec)

    //JAX-RS + Http Connector + Serializers
    implementation(libs.bundles.glassfish)
    implementation(libs.bundles.jackson)

    implementation(libs.hivemq.mqtt.client)

    //Open API
    implementation(libs.swagger.annotations)

    //json schema
    implementation(libs.jsonschema.validator)
    implementation(libs.jsonschema.generator)
    implementation(libs.jsonschema.module.jackson)
}

/* ******************** test ******************** */

dependencies {
    testAnnotationProcessor(libs.dagger.compiler)
    testImplementation(platform(libs.junit.jupiter.bom))
    testImplementation(libs.junit.classic)
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    testImplementation(libs.bundles.mokito)
    testImplementation(libs.equalsverifier)
    testImplementation(libs.concurrentunit)
    testImplementation(libs.bundles.shrinkwrap)
    testImplementation(libs.byte.buddy)
    testImplementation(libs.wiremock.jre8.standalone)
    testImplementation(libs.javassist)
    testImplementation(libs.awaitility)
    testImplementation(libs.assertj.core)
    testImplementation(libs.system.rules) {
        exclude("junit", "junit-dep")
    }
}

tasks.test {
    useJUnitPlatform()
    minHeapSize = "128m"
    maxHeapSize = "2048m"
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Duser.language=en",
        "-Duser.region=US",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.nio=ALL-UNNAMED",
        "--add-opens",
        "java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens",
        "jdk.management/com.sun.management.internal=ALL-UNNAMED",
        "--add-exports",
        "java.base/jdk.internal.misc=ALL-UNNAMED"
    )

    val inclusions = rootDir.resolve("inclusions.txt")
    val exclusions = rootDir.resolve("exclusions.txt")
    if (inclusions.exists()) {
        include(inclusions.readLines())
    } else if (exclusions.exists()) {
        exclude(exclusions.readLines())
    }

    testLogging {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

/* ******************** OpenAPI ******************** */

tasks.resolve {
    outputDir = temporaryDir
    outputFileName = "hivemq-edge-openapi"
    outputFormat = io.swagger.v3.plugins.gradle.tasks.ResolveTask.Format.YAML
    classpath = sourceSets.main.get().runtimeClasspath
    resourcePackages = setOf("com.hivemq.api.resources")
    openApiFile = file("src/openapi/openapi-base.yaml")
    sortOutput = true

    doFirst {
        delete(outputDir)
        outputDir.mkdir()
    }
}

val openApiSpec by tasks.registering(Sync::class) {
    group = "openapi"
    description = "Generates the OpenAPI yaml specification"

    from(tasks.resolve)
    into(layout.buildDirectory.dir("openapi"))
    filter { line -> line.replace("PLACEHOLDER_HIVEMQ_VERSION", "\"${project.version}\"") }
}


val openApiDoc by tasks.registering(Sync::class) {
    group = "openapi"
    description = "Generates the OpenAPI html documentation"

    from(openApiSpec)
    from("src/openapi/index.html")
    into(layout.buildDirectory.dir("docs/openapi"))
    filter { line -> line.replace("PLACEHOLDER_HIVEMQ_VERSION", "\"${project.version}\"") }
}

/* ******************** distribution ******************** */

tasks.jar {
    manifest.attributes(
        "Implementation-Title" to "HiveMQ",
        "Implementation-Vendor" to metadata.organization.get().name.get(),
        "Implementation-Version" to project.version,
        "HiveMQ-Version" to project.version,
        "HiveMQ-Edge-Version" to project.version,
        "Main-Class" to "com.hivemq.HiveMQEdgeMain"
    )
}

tasks.shadowJar {
    mergeServiceFiles()
    from(frontendBinary) {
        into("httpd")
    }
}


val hivemqZip by tasks.registering(Zip::class) {
    group = "distribution"

    val name = "hivemq-edge-${project.version}"

    archiveFileName.set("$name.zip")

    from("src/distribution") { exclude("**/.gitkeep") }
    from("src/main/resources/config.xml") { into("conf") }
    from(tasks.shadowJar) { into("bin").rename { "hivemq.jar" } }
    into(name)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("-html5")

    include("com/hivemq/embedded/*")

    doLast {
        javaexec {
            classpath("gradle/tools/javadoc-cleaner-1.0.jar")
        }
    }

    doLast { // javadoc search fix for jdk 11 https://bugs.openjdk.java.net/browse/JDK-8215291
        copy {
            from(destinationDir!!.resolve("search.js"))
            into(temporaryDir)
            filter { line -> line.replace("if (ui.item.p == item.l) {", "if (item.m && ui.item.p == item.l) {") }
        }
        delete(destinationDir!!.resolve("search.js"))
        copy {
            from(temporaryDir.resolve("search.js"))
            into(destinationDir!!)
        }
    }
}


/* ******************** checks ******************** */

jacoco {
    toolVersion = "${property("jacoco.version")}"
}

pmd {
    toolVersion = "${property("pmd.version")}"
    sourceSets = listOf(project.sourceSets.main.get())
    isIgnoreFailures = true
    rulesMinimumPriority.set(3)
}

spotbugs {
    toolVersion.set("${property("spotbugs.version")}")
    ignoreFailures.set(true)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
}

dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.8.0")
}

dependencyCheck {
    analyzers.apply {
        centralEnabled = false
    }
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL
    scanConfigurations = listOf("runtimeClasspath")
    suppressionFile = "$projectDir/gradle/dependency-check/suppress.xml"
}

tasks.check { dependsOn(tasks.dependencyCheckAnalyze) }

forbiddenApis {
    bundledSignatures = setOf("jdk-system-out")
}

tasks.forbiddenApisMain {
    exclude("**/BatchedException.class")
    exclude("**/LoggingBootstrap.class")
    exclude("**/CreateAdapterBlueprint.class")
}

tasks.forbiddenApisTest { enabled = false }


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

/*** artifacts ***/

val frontend: Configuration by configurations.creating { isCanBeConsumed = false; isCanBeResolved = false }

val frontendBinary: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
    extendsFrom(frontend)
}

dependencies {
    frontend("com.hivemq:hivemq-edge-frontend")
}


val releaseBinary: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("binary"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
}

artifacts {
    add(releaseBinary.name, hivemqZip)
}



