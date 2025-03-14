plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    compileSdk = 34
    defaultConfig {
        applicationId = "app.serlanventas.mobile"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.17.14"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        namespace = "app.serlanventas.mobile"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    packagingOptions {
        resources {
            // Excluir todos los archivos META-INF
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.pro",
                "META-INF/ASL2.0",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.version",
                "META-INF/androidx.*",
                "META-INF/*.version",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt"
            )
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    configurations {
        all {
            // Excluir las versiones conflictivas de Bouncy Castle
            exclude(group = "org.bouncycastle", module = "bcpkix-jdk15on")
            exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
            exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
            exclude(group = "org.bouncycastle", module = "bcpkix-jdk15to18")
        }
    }
}


dependencies {
    implementation("me.aflak.libraries:bluetooth:1.3.9") {
        exclude(group = "com.android.support", module = "support-compat")
    }
    implementation("androidx.core:core:1.9.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.itextpdf:itext7-core:7.1.13")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.tom-roush:pdfbox-android:2.0.25.0")
    implementation ("org.mindrot:jbcrypt:0.4")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.androidx.compose)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.play.services.location)
    implementation(libs.androidx.core.animation)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

