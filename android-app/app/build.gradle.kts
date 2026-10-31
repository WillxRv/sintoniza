import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Config do Firebase da instância (ver firebase.properties.example).
// Opcional: sem ela o app espera uma config em tempo de execução.
val firebaseProps = Properties().apply {
    val file = rootProject.file("firebase.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun firebaseField(key: String): String = "\"${firebaseProps.getProperty("firebase.$key", "")}\""

android {
    namespace = "app.sintoniza"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.sintoniza"
        minSdk = 28 // Android 9 (Mi TV Stick original)
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "FIREBASE_PROJECT_ID", firebaseField("projectId"))
        buildConfigField("String", "FIREBASE_APPLICATION_ID", firebaseField("applicationId"))
        buildConfigField("String", "FIREBASE_API_KEY", firebaseField("apiKey"))
        buildConfigField("String", "FIREBASE_GCM_SENDER_ID", firebaseField("gcmSenderId"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/{AL2.0,LGPL2.1}", "META-INF/versions/9/previous-compilation-data.bin")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.dadb)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
