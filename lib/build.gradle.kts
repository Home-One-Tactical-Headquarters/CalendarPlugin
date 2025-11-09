plugins {
    alias(libs.plugins.jvm)
    id("java-library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("org.jetbrains.kotlin.kapt") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("dk.holonet.plugin") version "0.0.1"
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly(compose.runtime)
    compileOnly(compose.foundation)
    compileOnly(compose.material)
    compileOnly(compose.ui)
    compileOnly(compose.components.resources)
    compileOnly(libs.serialization)
    implementation(libs.ical4j)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

holoNetPlugin {
    pluginId.set("calendar")
    pluginClass.set("dk.holonet.calendar.CalendarPlugin")
    pluginProvider.set("Holonet")
    pluginsDir.set(File("${rootProject.projectDir}/lib/build/plugins"))

    name.set("Calendar")
    author.set("Holonet")
    description.set("A simple Calendar plugin for Holonet" )

    config {
        field("url") {
            type.set("string")
            description.set("The URL to the calendar feed")
            default.set("")
            required.set(true)
        }

        /*field("refreshInterval") {
            type.set("number")
            description.set("Refresh interval in seconds")
            default.set("300")
            required.set(false)
        }

        field("theme") {
            type.set("string")
            description.set("Calendar theme")
            default.set("light")
            required.set(false)
            values.set(listOf("light", "dark", "auto"))
        }*/
    }

}
