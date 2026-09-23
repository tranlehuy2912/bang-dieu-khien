import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Giong hai app kia: mat khau con dau nam ngoai ma nguon.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
val hasKeystore = keystorePropsFile.exists()

// Plugin google-services chi bat khi da co google-services.json.
//
// Bat vo dieu kien thi may nao chua tai file ve la build do ngay tu dau, ke ca khi
// chi muon xem code co bien dich duoc khong. Thieu file thi app van dung duoc -
// no bao "chua noi Firebase" o man ghep doi thay vi tat ngang.
val coFirebase = file("google-services.json").exists()
if (coFirebase) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn(
        "Chua co app/google-services.json - ban build nay khong noi duoc Firestore. " +
            "Xem CAI_DAT_FIREBASE.md."
    )
}

android {
    namespace = "vn.huytl.bangdieukhien"
    compileSdk = 37

    defaultConfig {
        applicationId = "vn.huytl.bangdieukhien"
        // Firestore doi toi thieu 23; de 26 cho bang app ben tablet.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
