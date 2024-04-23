plugins {
    java
    `java-library`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin")
    id("io.github.sgtsilvio.gradle.defaults")
    id("com.github.sgtsilvio.gradle.metadata")
    id("com.github.sgtsilvio.gradle.javadoc-links")
    id("com.github.hierynomus.license")
}
group = "com.hivemq"
description = "Library for the development of HiveMQ Edge adapters"

metadata {
    readableName.set("HiveMQ Edge SDK")
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
    }
    github {
        org.set("hivemq")
        repo.set("hivemq-edge")
        issues()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.hivemq:hivemq-edge-extension-sdk:${property("hivemq-edge-extension-sdk.version")}")

    compileOnly(libs.guava)
    compileOnly(libs.jackson.jaxrs.jsonProvider)
    compileOnly(libs.networkNT)
    compileOnly(libs.swagger.annotations)
    compileOnly(libs.apache.commonsLang)

    compileOnly("com.github.victools:jsonschema-generator:4.35.0")
    compileOnly("com.github.victools:jsonschema-module-jackson:4.35.0")

}


dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

/* ******************** java ******************** */

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Jar>().configureEach {
    manifest.attributes(
        "Implementation-Title" to project.name,
        "Implementation-Vendor" to metadata.organization.get().name.get(),
        "Implementation-Version" to project.version
    )
}

tasks.javadoc {
    title = "${metadata.readableName.get()} ${project.version} API"

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

/* ******************** publishing ******************** */

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

signing {
    val signKey: String? by project
    val signKeyPass: String? by project
    useInMemoryPgpKeys(signKey, signKeyPass)
    sign(publishing.publications["maven"])
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

/* ******************** checks ******************** */

license {
    header = file("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}
