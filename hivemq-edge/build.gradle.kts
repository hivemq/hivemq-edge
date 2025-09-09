import nl.javadude.gradle.plugins.license.DownloadLicensesExtension.license
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins.withId("com.hivemq.edge-version-updater") {
    project.ext.set(
        "versionUpdaterFiles",
        arrayOf("src/main/resources/hivemq-edge-configuration.json", "gradle.properties")
    )
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin")
    }
}

plugins {
    java
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.shadow)
    alias(libs.plugins.defaults)
    alias(libs.plugins.metadata)
    alias(libs.plugins.javadoclinks)
    alias(libs.plugins.githubrelease)
    alias(libs.plugins.license)
    alias(libs.plugins.versions)

    /* Code Quality Plugins */
//    alias(libs.plugins.spotbugs)
    alias(libs.plugins.forbiddenapis)

    alias(libs.plugins.openapi.generator)

    id("pmd")
    id("com.hivemq.edge-version-updater")
    id("com.hivemq.third-party-license-generator")
    id("com.hivemq.repository-convention")
    id("com.hivemq.jacoco-convention")
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
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withJavadocJar()
    withSourcesJar()
}

/* ******************** dependencies ******************** */

// Runtime stuffs
dependencies {

    //HiveMQ
    api(libs.hivemq.extensionsdk)
    api(libs.hivemq.edge.extensionsdk)
    api(libs.hivemq.edge.adaptersdk)

    // netty
    implementation(libs.netty.buffer)
    implementation(libs.netty.codec)
    implementation(libs.netty.codec.http)
    implementation(libs.netty.commons)
    implementation(libs.netty.handler)
    implementation(libs.netty.transport)

    // logging
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.jultoslf4j)
    implementation(libs.logback.classic)

    // security
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)

    // override transitive dependencies that have security vulnerabilities
    implementation(platform(libs.kotlin.bom))
    constraints {
        implementation(libs.apache.commons.compress)
    }


    // config
    implementation(libs.jaxb2.impl)
    implementation(libs.jaxb4.impl)
    implementation(libs.jaxb4.bind)

    // metrics
    implementation(libs.dropwizard.metrics)
    implementation(libs.dropwizard.metrics.jmx)
    runtimeOnly(libs.dropwizard.metrics.logback)
    implementation(libs.dropwizard.metrics.jvm)

    // dependency injection
    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)

    implementation(libs.jakarta.annotation.api)

    // common
    implementation(libs.apache.commons.io)
    implementation(libs.apache.commons.lang)
    implementation(libs.guava) {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.errorprone", "error_prone_annotations")
    }
    implementation(libs.javacrumbs.futureconverter)

    // com.google.code.findbugs:jsr305 (transitive dependency of com.google.guava:guava) is used in imports
    implementation(libs.zeroallocationhashing)
    implementation(libs.jctools)

    //mqtt-sn codec
    implementation(libs.mqtt.sn.codec)
    implementation(libs.hivemq.mqtt.client)

    //JAX-RS + Http Connector + Serializers
    implementation(libs.jersey.container.jdk.http)
    implementation(libs.jersey.hk2)
    implementation(libs.jersey.media.json.jackson)
    implementation(libs.jersey.media.multipart)

    // Jackson
    implementation(libs.jackson.jaxrs.json.provider)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.jackson.dataformat.xml)

    //Open API
    implementation(libs.swagger.annotations)
    implementation(libs.swagger.jaxrs)

    //JWT
    implementation(libs.jose4j)

    //json schema
    implementation(libs.json.schema.validator)
    implementation(libs.victools.jsonschema.generator)
    implementation(libs.victools.jsonschema.jackson)
    implementation(libs.json.schema.inferrer)

    // Edge modules
    compileOnly("com.hivemq:hivemq-edge-module-etherip")
    compileOnly("com.hivemq:hivemq-edge-module-plc4x")
    compileOnly("com.hivemq:hivemq-edge-module-http")
    compileOnly("com.hivemq:hivemq-edge-module-modbus")
    compileOnly("com.hivemq:hivemq-edge-module-mtconnect")
    compileOnly("com.hivemq:hivemq-edge-module-databases")
    compileOnly("com.hivemq:hivemq-edge-module-opcua")
    compileOnly("com.hivemq:hivemq-edge-module-file")
}

/* ******************** test ******************** */

dependencies {
    testAnnotationProcessor(libs.dagger.compiler)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.vintage:junit-vintage-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation(libs.mockito.junit.jupiter)

    testImplementation(libs.equalsverifier)
    testImplementation(libs.concurrentunit)
    testImplementation(libs.shrinkwrap.api)
    testRuntimeOnly(libs.shrinkwrap.impl)
    testImplementation(libs.bytebuddy)
    testImplementation(libs.wiremock.jre8.standalone)
    testImplementation(libs.javassist)
    testImplementation(libs.awaitility)
    testImplementation(libs.assertj)
    testImplementation(libs.stefanbirkner.systemrules) {
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
        "java.base/java.util=ALL-UNNAMED",
        "--add-opens",
        "java.xml/javax.xml.namespace=ALL-UNNAMED",
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
val buildDirectory = layout.buildDirectory.get()
tasks.register<GenerateTask>("genJaxRs") {
    inputSpec.set("${projectDir}/../ext/hivemq-edge-openapi-${project.version}.yaml")
    outputDir.set("${buildDirectory}/generated/openapi")
    generatorName.set("jaxrs-spec")
    apiPackage.set("com.hivemq.edge.api")
    modelPackage.set("com.hivemq.edge.api.model")
    invokerPackage.set("com.hivemq.edge.api")
    generateApiTests.set(false)
    configOptions.set(
        hashMapOf(
            "useJakartaEe" to "true",
            "dateLibrary" to "java8",
            "generateBuilders" to "true",
            "generatePom" to "false",
            "interfaceOnly" to "true",
            "useTags" to "true",
            "returnResponse" to "true",
            "openApiNullable" to  "false"
        )
    )
}

sourceSets {
    main {
        java {
            srcDirs("${buildDirectory}/generated/openapi/src/gen/java")
            srcDirs("${buildDirectory}/generated/openapi/src/main/java")
        }
    }
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

tasks.compileJava {
    dependsOn(tasks.named("genJaxRs"))
}

tasks.named("sourcesJar") {
    dependsOn(tasks.named("genJaxRs"))
}

tasks.named("licenseMain") {
    dependsOn(tasks.named("genJaxRs"))
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

val cleanJavadoc by tasks.registering(JavaExec::class) {
    classpath("gradle/tools/javadoc-cleaner-1.0.jar")
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("-html5")

    include("com/hivemq/embedded/*")

    doLast {
        cleanJavadoc.get().exec()
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

pmd {
    toolVersion = libs.versions.pmd.get()
    sourceSets = listOf(project.sourceSets.main.get())
    isIgnoreFailures = true
    rulesMinimumPriority.set(3)
}

//spotbugs {
//    toolVersion.set(libs.versions.spotBugs.get())
//    ignoreFailures.set(true)
//    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
//}

//dependencies {
//    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")
//}

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
    exclude("*.json")
    exclude("**/*.xml")
    exclude("**/RollingList.java")
    exclude("**/api/**/*.java")
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
}

tasks.updateThirdPartyLicenses {
    dependsOn(tasks.downloadLicenses)
    projectName.set("HiveMQ Edge")
    group = "license"
    dependencyLicense.set(tasks.downloadLicenses.get().xmlDestination.resolve("dependency-license.xml"))
    outputDirectory.set(layout.buildDirectory.dir("distribution/third-party-licenses"))
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

val releaseJar: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("jar"))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("release"))
    }
}

val thirdPartyLicenses: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("third-party-licenses"))
    }
}

artifacts {
    add(releaseBinary.name, hivemqZip)
    add(releaseJar.name, tasks.shadowJar)
    add(thirdPartyLicenses.name, tasks.updateThirdPartyLicenses.flatMap { it.outputDirectory })
}
