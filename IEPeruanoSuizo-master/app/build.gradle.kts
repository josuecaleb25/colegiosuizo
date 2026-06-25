plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.ieperuanosuizoapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ieperuanosuizoapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // CameraX dependencies
    val cameraxVersion = "1.3.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // QR Scanning (Google ML Kit)
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // Retrofit para consumir API REST
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Gson para parsear JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Apache POI para generar Excel (.xlsx) con gráficos
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5") {
        exclude(group = "org.apache.poi", module = "poi-ooxml-lite")
        exclude(group = "stax")
        exclude(group = "xml-apis")
        exclude(group = "org.apache.logging.log4j")
    }
    implementation("org.apache.poi:poi-ooxml-full:5.2.5")

    // Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    
    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}