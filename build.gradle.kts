plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("com.android.library") version "8.1.0" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

val targetCompatibility by extra(JavaVersion.VERSION_17)