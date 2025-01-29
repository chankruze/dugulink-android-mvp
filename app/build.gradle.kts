plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.geekofia.bluelink_mvp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.geekofia.bluelink_mvp"
        minSdk = 25
        targetSdk = 34
        versionCode = 105
        versionName = "1.0.5"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }

    applicationVariants.all {
        outputs.all {
            val appName = project.name
            val versionCode = this@all.versionCode
            val variantName = name
            val fileExtension = if (outputFile.name.endsWith(".apk")) "apk" else "aab"

            val newFileName = "${appName}-${variantName}-${versionCode}.${fileExtension}"
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                newFileName
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.org.eclipse.paho.client.mqttv3)
    implementation(libs.org.eclipse.paho.android.service)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
