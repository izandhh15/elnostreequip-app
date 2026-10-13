pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application")
                useModule("com.android.tools.build:gradle:8.5.2")
            if (requested.id.id == "org.jetbrains.kotlin.android")
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
            if (requested.id.id == "org.jetbrains.kotlin.kapt")
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
            if (requested.id.id == "com.google.gms.google-services")
                useModule("com.google.gms:google-services:4.4.2")
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ElNostreEquip"
include(":app")
