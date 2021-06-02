import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.compiler.plugin.parsePluginOption

plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("kapt")
    id("com.google.protobuf") version "0.8.12"
}

dependencies {
    implementation(Deps.Compose.animation)
    implementation(Deps.Compose.foundation)
    implementation(Deps.Compose.layout)
    implementation(Deps.Compose.material)
    implementation(Deps.Compose.material)
    implementation(Deps.Compose.tooling)
    implementation(Deps.Compose.ui)
    implementation(Deps.activityCompose)
    implementation(Deps.appCompat)
    implementation(Deps.kotlinExtensions)
    implementation(Deps.lifecycle)
    implementation(Deps.material)
    implementation(Deps.oolong)
    implementation(Deps.datastore)
    implementation(Deps.protobuf)


    implementation(Deps.Dagger.android)
    implementation(Deps.Dagger.androidSupport)
    kapt(Deps.Dagger.processor)

    implementation(Deps.Hilt.android)
    kapt(Deps.Hilt.compiler)

    // For instrumentation tests
    androidTestImplementation(Deps.Hilt.testing)
    kaptAndroidTest(Deps.Hilt.compiler)

    // For local unit tests
    testImplementation(Deps.Hilt.testing)
    kaptTest(Deps.Hilt.compiler)

    testImplementation(Deps.JUnit.junit)
    androidTestImplementation(Deps.JUnit.androidExt)
    androidTestImplementation(Deps.Espresso.core)
    androidTestImplementation(Deps.Compose.uiTest)
}

android {
    compileSdk = 30
    buildToolsVersion = Versions.buildTools

    defaultConfig {
        applicationId = "com.dallasgutauckis.vane"
        minSdk = 24
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        useIR = true
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Deps.Compose.version
    }
}

kapt {
    correctErrorTypes = true
}

protobuf.protobuf.run {
    protoc {
        artifact = "com.google.protobuf:protoc:3.10.0"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
