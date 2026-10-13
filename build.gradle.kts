// Top-level build file.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false
    // TODO: aplicar `com.google.gms.google-services` cuando Izan cree el proyecto
    // Firebase y tengamos `app/google-services.json`. El SDK de FCM compila sin él.
}
