plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "it.vfsfitvnm.compose.persist"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        base.archivesName.set("compose-persist")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    sourceSets {
        named("main") {
            kotlin.srcDirs("src/main/kotlin")
        }
        named("release") {
            kotlin.srcDirs("src/release/kotlin")
        }
        named("debug") {
            kotlin.srcDirs("src/debug/kotlin")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(libs.compose.foundation)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}