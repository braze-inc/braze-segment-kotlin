val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = getVersionName()

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm")

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    id("io.github.gradle-nexus.publish-plugin")
    id("io.gitlab.arturbosch.detekt")
}

buildscript {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    dependencies {
        val KOTLIN_VERSION: String by project
        val GRADLE_VERSION: String by project
        val DETEKT_VERSION: String by project
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$KOTLIN_VERSION")
        classpath("com.android.tools.build:gradle:$GRADLE_VERSION")
        classpath("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:$DETEKT_VERSION")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

fun getVersionName() =
    if (hasProperty("release"))
        VERSION_NAME
    else
        "$VERSION_NAME-SNAPSHOT"
