plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.metromultindo.tirtapanrannuangku"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.metromultindo.tirtapanrannuangku"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                cppFlags("")
                arguments("-DANDROID_STL=c++_shared")
            }
        }

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    buildTypes {
        debug {
            ndk {
                abiFilters.add("armeabi-v7a")
                abiFilters.add("arm64-v8a")
                abiFilters.add("x86")
                abiFilters.add("x86_64")
            }
            // Enable in-app update testing
            manifestPlaceholders["enableInAppUpdateTesting"] = "true"
        }
        release {
            ndk {
                abiFilters.add("armeabi-v7a")
                abiFilters.add("arm64-v8a")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["enableInAppUpdateTesting"] = "false"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }
}

// Global excludes untuk menghilangkan advertising dependencies
configurations.all {
    exclude (group = "com.google.firebase", module = "firebase-analytics")
    exclude (group =  "com.google.firebase", module = "firebase-analytics-ktx")
    exclude (group = "com.google.firebase", module = "firebase-measurement-connector")
    exclude (group = "com.google.android.gms", module = "play-services-ads-identifier")
    exclude (group = "com.google.android.gms", module = "play-services-measurement")
    exclude (group = "com.google.android.gms", module = "play-services-measurement-api")
    exclude (group = "com.google.android.gms", module = "play-services-measurement-sdk")
    exclude (group = "com.google.android.gms", module = "play-services-measurement-impl")
    exclude (group = "com.google.android.gms", module = "play-services-measurement-base")
}

dependencies {
    // IMPROVED: Updated In-App Update dependencies
    implementation ("com.google.android.play:app-update:2.1.0")
    implementation ("com.google.android.play:app-update-ktx:2.1.0")

    // ADDED: Review API untuk meminta rating setelah update
    implementation ("com.google.android.play:review:2.0.1")
    implementation ("com.google.android.play:review-ktx:2.0.1")

    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Lifecycle and Coroutines
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // HANYA Firebase Messaging tanpa Analytics
    implementation("com.google.firebase:firebase-messaging-ktx")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Dagger Hilt - Core
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // Hilt Extensions
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Accompanist
    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // Compose testing
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.github.skydoves:landscapist-glide:2.2.10")
    implementation("com.github.skydoves:landscapist-transformation:2.2.10")

    // Google Play Services - dengan exclude advertising
    implementation("com.google.android.gms:play-services-location:21.0.1") {
        exclude(group = "com.google.android.gms", module = "play-services-ads-identifier")
        exclude(group = "com.google.android.gms", module = "play-services-measurement")
    }
    implementation("com.google.android.gms:play-services-maps:18.2.0") {
        exclude (group = "com.google.android.gms", module = "play-services-ads-identifier")
        exclude (group = "com.google.android.gms", module = "play-services-measurement")
    }
}

// Kapt configuration
kapt {
    correctErrorTypes = true
    javacOptions {
        option("-Adagger.hilt.android.internal.disableAndroidSuperclassValidation=true")
    }
}