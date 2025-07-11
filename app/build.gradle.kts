import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
    id("kotlin-kapt")
    kotlin("plugin.serialization") version "1.9.0"
    //id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.synngate.synnframe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.synngate.synnframe"
        minSdk = 28
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("releaseKeystorePath", "../keystores/synnframe-release.keystore"))
            storePassword = keystoreProperties.getProperty("releaseKeystorePassword", "")
            keyAlias = keystoreProperties.getProperty("releaseKeyAlias", "synnframe_release")
            keyPassword = keystoreProperties.getProperty("releaseKeyPassword", "")
        }

        create("testing") {
            storeFile = file(keystoreProperties.getProperty("testKeystorePath", "../keystores/synnframe-test.keystore"))
            storePassword = keystoreProperties.getProperty("testKeystorePassword", "")
            keyAlias = keystoreProperties.getProperty("testKeyAlias", "synnframe_test")
            keyPassword = keystoreProperties.getProperty("testKeyPassword", "")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }

        create("stagingRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("testing")
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/INDEX.LIST",
                    "META-INF/io.netty.versions.properties",
                    "META-INF/AL2.0",
                    "META-INF/LGPL2.1"
                )
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Core Android
    //implementation("androidx.core:core-ktx:1.12.0")
    //implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    //implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.window:window:1.2.0")

    // Compose
//    val composeBom = platform("androidx.compose:compose-bom:2023.08.00")
//    implementation(composeBom)
//    implementation("androidx.compose.ui:ui")
//    implementation("androidx.compose.ui:ui-graphics")
//    implementation("androidx.compose.ui:ui-tooling-preview")
//    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    kapt("androidx.room:room-compiler:2.6.1")
    //ksp("androidx.room:room-compiler:$roomVersion")

    // Paging 3
    val pagingVersion = "3.2.1"
    implementation("androidx.paging:paging-runtime:$pagingVersion")
    implementation("androidx.paging:paging-compose:$pagingVersion")

    // Ktor
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // Ktor Server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Camera
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // ZXing (Barcode scanning)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.2")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    implementation("androidx.work:work-runtime-ktx:2.8.1")

    implementation("org.mvel:mvel2:2.4.14.Final")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    //androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Загрузка конфигурации обновления
val updateConfigFile = rootProject.file("update-config.properties")
val updateConfig = Properties().apply {
    if (updateConfigFile.exists()) {
        load(updateConfigFile.inputStream())
    } else {
        // Значения по умолчанию
        setProperty("updateOutputDir", "outputs/update")
        setProperty("stagingUpdateOutputDir", "outputs/update/staging")

        // Создаем файл конфигурации с дефолтными значениями
        updateConfigFile.parentFile.mkdirs()
        store(updateConfigFile.outputStream(), "Update configuration")
    }
}

// Теперь полный код задач:
afterEvaluate {
    tasks.register("createUpdateFiles") {
        description = "Creates version-named APK and update info JSON"

        // Зависимость от задачи сборки
        dependsOn("assembleRelease")

        doLast {
            // Получаем версию из defaultConfig
            val versionName = android.defaultConfig.versionName

            // Получаем текущую дату
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
            val currentDate = dateFormatter.format(Date())

            // Используем настраиваемую директорию из конфигурации
            val updateOutputDir = updateConfig.getProperty("updateOutputDir", "outputs/update")

            val buildDirectory = layout.buildDirectory.get().asFile
            val releaseDir = buildDirectory.resolve("outputs/apk/release")
            val originalApk = releaseDir.resolve("app-release.apk")

            // Создаем директорию для обновлений с учетом настроек
            val updateDir = buildDirectory.resolve(updateOutputDir)
            updateDir.mkdirs()

            // Копируем APK с новым именем
            val versionedApk = updateDir.resolve("synnframe-${versionName}.apk")
            originalApk.copyTo(versionedApk, overwrite = true)

            // Создаем JSON файл с информацией о версии
            val updateJsonFile = updateDir.resolve("version.json")
            val jsonContent = """
                {
                  "lastVersion": "${versionName}",
                  "releaseDate": "${currentDate}"
                }
            """.trimIndent()

            updateJsonFile.writeText(jsonContent)

            // Выводим информацию об успешном создании файлов
            println("==================================================")
            println("Update files created successfully:")
            println("APK: ${versionedApk.absolutePath}")
            println("JSON: ${updateJsonFile.absolutePath}")
            println("==================================================")
        }
    }

    tasks.register("createStagingUpdateFiles") {
        description = "Creates version-named APK and update info JSON for staging build"

        dependsOn("assembleStagingRelease")

        doLast {
            val versionName = android.defaultConfig.versionName + "-staging"

            // Получаем текущую дату
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd")
            val currentDate = dateFormatter.format(Date())

            // Используем настраиваемую директорию из конфигурации
            val updateOutputDir = updateConfig.getProperty("stagingUpdateOutputDir", "outputs/update/staging")

            val buildDirectory = layout.buildDirectory.get().asFile
            val releaseDir = buildDirectory.resolve("outputs/apk/stagingRelease")
            val originalApk = releaseDir.resolve("app-stagingRelease.apk")

            // Создаем директорию для обновлений с учетом настроек
            val updateDir = buildDirectory.resolve(updateOutputDir)
            updateDir.mkdirs()

            val versionedApk = updateDir.resolve("synnframe-${versionName}.apk")
            originalApk.copyTo(versionedApk, overwrite = true)

            val updateJsonFile = updateDir.resolve("version.json")
            val jsonContent = """
                {
                  "lastVersion": "${versionName}",
                  "releaseDate": "${currentDate}"
                }
            """.trimIndent()

            updateJsonFile.writeText(jsonContent)

            println("==================================================")
            println("Staging update files created successfully:")
            println("APK: ${versionedApk.absolutePath}")
            println("JSON: ${updateJsonFile.absolutePath}")
            println("==================================================")
        }
    }

    // Связываем задачи сборки с нашими задачами
    tasks.findByName("assembleRelease")?.finalizedBy("createUpdateFiles")
    tasks.findByName("assembleStagingRelease")?.finalizedBy("createStagingUpdateFiles")
}