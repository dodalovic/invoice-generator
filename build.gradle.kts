import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    val kotlinVersion = "1.4.21"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "5.2.0"
    kotlin("plugin.serialization") version kotlinVersion
}

group = "com.odalovic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("info.picocli:picocli:4.5.2")
    annotationProcessor("info.picocli:picocli-codegen:4.5.2")
    kapt("info.picocli:picocli-codegen:4.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0")
    implementation("com.charleskorn.kaml:kaml:0.26.0")
    implementation("org.asciidoctor:asciidoctorj:2.4.2")
    implementation("org.asciidoctor:asciidoctorj-pdf:1.5.3")
}

kapt {
    arguments {
        arg("project", "${project.group}/${project.name}")
    }
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes["Main-Class"] = "com.odalovic.invoicegenerator.InvoiceGeneratorKt"
    }
}
