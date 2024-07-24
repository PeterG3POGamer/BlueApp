plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.serlanventas.blueapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.8.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        namespace = "com.example.blueapp"
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
    }

    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/ASL2.0",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation("com.google.android.material:material:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.itextpdf:itext7-core:7.1.13")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.tom-roush:pdfbox-android:2.0.25.0")
    //    implementation("com.github.javiersantos:AppUpdater:2.7")
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

