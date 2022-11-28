rootProject.name = "analytics-kotlin-braze"
include(":lib")
include(":testapp")

pluginManagement {
    val KOTLIN_VERSION: String by settings
    val DETEKT_VERSION: String by settings
    val NEXUS_PLUGIN_VERSION: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version KOTLIN_VERSION
        id("io.github.gradle-nexus.publish-plugin") version NEXUS_PLUGIN_VERSION
        id("io.gitlab.arturbosch.detekt") version DETEKT_VERSION
    }
}
