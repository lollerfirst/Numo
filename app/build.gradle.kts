plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.shellshock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.shellshock"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // No instrumentation runner needed - only unit tests
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        viewBinding = true
        // Remove buildConfig = true since it's deprecated
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        lintConfig = file("src/main/res/xml/lint.xml")
        baseline = file("lint-baseline.xml")
        abortOnError = false  // We want to build even with lint warnings
        // Disable the specific NewApi checks for Optional and ignore classpath issues
        disable += "NewApi"
        disable += "InvalidPackage"  // For libs JAR files
        // Disable lint checking on JAR dependencies
        checkDependencies = false
    }

    useLibrary("org.apache.http.legacy")
}

dependencies {
    val composeBomVersion = "2024.01.00"
    val lifecycleVersion = "2.7.0"
    
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material:1.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    
    // AndroidX Libraries
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    
    // Testing - Unit and Integration tests only, NO UI tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-kotlin:5.2.1")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Debug tools (keeping only necessary ones)
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    // Project specific dependencies
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation(files("libs/cashu-java-sdk-1.0-SNAPSHOT.jar"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Jackson for JSON and CBOR processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.16.1")
    
    // CBOR library from Peter O. Upokecenter
    implementation("com.upokecenter:cbor:4.5.2")
    implementation("com.airbnb.android:lottie:5.2.0")
}
