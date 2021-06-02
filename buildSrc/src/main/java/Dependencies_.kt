object Versions {
    const val buildTools = "30.0.3"
    const val appCompat = "1.3.0"
    const val kotlin = "1.4.32"
    const val dagger = "2.36"
}

object Deps {
    const val kotlinExtensions = "androidx.core:core-ktx:1.5.0"
    const val appCompat = "androidx.appcompat:appcompat:${Versions.appCompat}"
    const val lifecycle = "androidx.lifecycle:lifecycle-runtime-ktx:2.4.0-alpha01"
    const val activityCompose = "androidx.activity:activity-compose:1.3.0-alpha08"
    const val oolong = "org.oolong-kt:oolong:2.1.0"
    const val material = "com.google.android.material:material:1.3.0"
    const val datastore = "androidx.datastore:datastore:1.0.0-beta01"
    const val protobuf = "com.google.protobuf:protobuf-javalite:3.11.0"

    object Compose {
        const val version = "1.0.0-beta07"

        const val ui = "androidx.compose.ui:ui:$version"
        const val runtime = "androidx.compose.runtime:runtime:$version"
        const val material = "androidx.compose.material:material:$version"
        const val foundation = "androidx.compose.foundation:foundation:$version"
        const val layout = "androidx.compose.foundation:foundation-layout:$version"
        const val tooling = "androidx.compose.ui:ui-tooling:$version"
        const val animation = "androidx.compose.animation:animation:$version"
        const val uiTest = "androidx.compose.ui:ui-test-junit4:$version"
    }

    object Dagger {
        const val version = "2.36"
        const val android = "com.google.dagger:dagger-android:$version"
        const val androidSupport = "com.google.dagger:dagger-android-support:$version"
        const val processor = "com.google.dagger:dagger-android-processor:$version"
    }

    object Hilt {
        private const val version = Dagger.version

        const val gradlePlugin = "com.google.dagger:hilt-android-gradle-plugin:$version"
        const val android = "com.google.dagger:hilt-android:$version"
        const val compiler = "com.google.dagger:hilt-compiler:$version"
        const val testing = "com.google.dagger:hilt-android-testing:$version"
    }

    object Espresso {
        const val version = "3.3.0"
        const val core = "androidx.test.espresso:espresso-core:$version"
    }

    object JUnit {
        const val junit = "junit:junit:4.13.2"
        const val androidExt = "androidx.test.ext:junit:1.1.2"
    }
}
