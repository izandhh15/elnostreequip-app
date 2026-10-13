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

// Branding: els PNGs (icones + logo) es pugen al repo com a .b64 perquè
// l'eina de publicació només accepta text pla. Es descodifiquen aquí,
// a l'settings script (s'executa abans de configurar cap mòdul),
// així els recursos ja existeixen quan AGP els compila.
val brandingSrc = file("app/branding-b64")
val brandingRes = file("app/src/main/res")
if (brandingSrc.isDirectory) {
    brandingSrc.walkTopDown()
        .filter { f -> f.isFile && f.extension == "b64" }
        .forEach { f ->
            val out = brandingRes.resolve(f.relativeTo(brandingSrc).path.removeSuffix(".b64"))
            out.parentFile.mkdirs()
            val bytes = java.util.Base64.getDecoder().decode(f.readText().trim())
            if (!out.exists() || !out.readBytes().contentEquals(bytes)) {
                out.writeBytes(bytes)
            }
        }
}
