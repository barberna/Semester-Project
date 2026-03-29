// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Maps Plugins
    alias(libs.plugins.google.maps.secrets) apply false

    // DB Plugins
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
    id("androidx.room") version "2.8.1" apply false
}