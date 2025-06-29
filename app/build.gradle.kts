@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(gradleLibs.plugins.android.application)
    alias(gradleLibs.plugins.compose.compiler)
    alias(gradleLibs.plugins.google.ksp)
    alias(gradleLibs.plugins.kotlin.android)
    alias(gradleLibs.plugins.kotlin.serialization)
}

android {
    signingConfigs {
        create("key") {
            // Priority to command line properties, fallback to signing.properties file
            val props = Properties().apply {
                val signingProp = project.rootProject.file("signing.properties")
                if (signingProp.exists()) {
                    load(FileInputStream(signingProp))
                }
                // Overwrite with project properties from command line if they exist
                project.properties.forEach { (key, value) ->
                    if (key.startsWith("keystore.")) {
                        this[key] = value
                    }
                }
            }

            val keystorePath = props.getProperty("keystore.path")
            if (!keystorePath.isNullOrEmpty()) {
                storeFile = rootProject.file(keystorePath)
                storePassword = props.getProperty("keystore.pwd")
                keyAlias = props.getProperty("keystore.alias")
                keyPassword = props.getProperty("keystore.alias_pwd")
            }
        }
    }

    namespace = AppConfiguration.appId
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        applicationId = AppConfiguration.appId
        minSdk = AppConfiguration.minSdk
        targetSdk = AppConfiguration.targetSdk
        versionCode = AppConfiguration.versionCode
        versionName = AppConfiguration.versionName
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // 设置支持的SO库架构
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
        
        // 从环境变量或Gradle属性中读取Bugly AppID
        val buglyAppId = System.getenv("BUGLY_APP_ID") ?: project.findProperty("bugly.app.id")?.toString() ?: ""
        buildConfigField("String", "BUGLY_APP_ID", "\"${buglyAppId}\"")
    }

    flavorDimensions.add("channel")

    productFlavors {
        create("lite") {
            dimension = "channel"
        }
        create("default") {
            dimension = "channel"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("key")
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".debug"
        }
        create("r8Test") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".r8test"
            signingConfig = signingConfigs.findByName("key")
        }
        create("alpha") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("key")
        }
    }
    // https://issuetracker.google.com/issues/260059413
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/*.proto"
        }

        if (gradle.startParameter.taskNames.find { it.startsWith("assembleLite") } != null) {
            jniLibs {
                val vlcLibs = listOf("libvlc", "libc++_shared", "libvlcjni")
                val abis = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
                vlcLibs.forEach { vlcLibName -> abis.forEach { abi -> excludes.add("lib/$abi/$vlcLibName.so") } }
            }
        }
    }

    /*splits {
        if (gradle.startParameter.taskNames.find { it.startsWith("assembleDefault") } != null) {
            abi {
                isEnable = true
                reset()
                include("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
                isUniversalApk = true
            }
        }
    }*/

    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            (this as ApkVariantOutputImpl).apply {
                val abi = this.filters.find { it.filterType == "ABI" }?.identifier ?: "universal"
                outputFileName =
                    "BV_${AppConfiguration.versionCode}_${AppConfiguration.versionName}.${variant.buildType.name}_${variant.flavorName}_$abi.apk"
                versionNameOverride =
                    "${variant.versionName}.${variant.buildType.name}"
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    annotationProcessor(androidx.room.compiler)
    ksp(androidx.room.compiler)
    implementation(androidx.activity.compose)
    implementation(androidx.core.ktx)
    implementation(androidx.core.splashscreen)
    implementation(androidx.compose.constraintlayout)
    implementation(androidx.compose.ui)
    implementation(androidx.compose.ui.util)
    implementation(androidx.compose.ui.tooling.preview)
    implementation(androidx.compose.material.icons)
    implementation(androidx.compose.material3)
    implementation(androidx.compose.tv.foundation)
    implementation(androidx.compose.tv.material)
    implementation(androidx.datastore.typed)
    implementation(androidx.datastore.preferences)
    implementation(androidx.lifecycle.runtime.ktx)
    implementation(androidx.media3.common)
    implementation(androidx.media3.decoder)
    implementation(androidx.media3.exoplayer)
    implementation(androidx.media3.ui)
    implementation(androidx.room.ktx)
    implementation(androidx.room.runtime)
    implementation(androidx.webkit)
    implementation(libs.akdanmaku)
    implementation(libs.androidSvg)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.geetest.sensebot)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.serialization)
    implementation(libs.ktor.client.cio)
    implementation(libs.koin.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.serialization.kotlinx)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.core)
    implementation(libs.logging)
    implementation(libs.lottie)
    implementation(libs.material)
    implementation(libs.qrcode)
    implementation(libs.slf4j.android.mvysny)
    
    // OkHttp 限制连接池大小，解决okio.Segment内存问题
    implementation("com.squareup.okhttp3:okhttp:4.11.0") {
        // 强制使用这个版本，避免其他依赖引入不同版本
        isForce = true
    }
    
    // 腾讯Bugly SDK
    implementation("com.tencent.bugly:crashreport:latest.release")
    implementation("com.tencent.bugly:nativecrashreport:latest.release")
    
    implementation(project(mapOf("path" to ":bili-api")))
    implementation(project(mapOf("path" to ":bili-subtitle")))
    implementation(project(mapOf("path" to ":bv-player")))
    testImplementation(androidx.room.testing)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(androidx.compose.ui.test.junit4)
    debugImplementation(androidx.compose.ui.test.manifest)
    debugImplementation(androidx.compose.ui.tooling)
}

tasks.withType<Test> {
    useJUnitPlatform()
}