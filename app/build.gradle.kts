import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 上架用的簽署金鑰資訊。⚠️ keystore.properties 與金鑰檔絕不能進版控（已列入 .gitignore）。
// 範本見專案根目錄的 keystore.properties.example。沒有這個檔時 release 版不簽署，只能自己測。
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.example.relatestats"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // ⚠️ 上架後永遠不能改。namespace（Kotlin 套件路徑）維持 com.example.relatestats 沒關係，Play 只看這個。
        applicationId = "com.relatestats.app"
        minSdk = 24
        targetSdk = 36
        /**
         * ⚠️ versionCode 只能往上加，而且是跨所有軌道（內部／封閉／正式）
         *    共用同一個池子 —— 上傳過的數字永遠不能重複或回頭。
         *    改 versionName 的編碼方式不影響它，它就是一路 +1。
         * ⚠️ versionName 是純顯示字串，Play 不檢查。
         *    1.0.0 起為公開發行版；在那之前的 3.x 是上架前的開發版本，
         *    歷史保留在 docs/CHANGELOG.md，沒有刪掉。
         */
        versionCode = 103
        versionName = "1.0.25"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps.containsKey("storeFile")) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // ⚠️ 開發版一定要跟 Play 版不同套件名稱。同一個套件名稱、不同簽章時，
            //    手機上裝著開發版就無法從 Play 安裝測試版（Play 只會顯示「伺服器端發生錯誤」）。
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
            // ⚠️ 服務名稱也要跟正式版分得開。兩版都裝著的時候，
            //    系統的無障礙清單裡會出現兩個一模一樣的
            //    「RelateStats 停留時間監測」，使用者根本看不出該開哪一個，
            //    開錯的那個不會讓正式版記到任何資料。
            manifestPlaceholders["labelSuffix"] = "（開發版）"
        }
        release {
            manifestPlaceholders["labelSuffix"] = ""
            if (keystoreProps.containsKey("storeFile")) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    /**
     * %s **強制把 fragment 拉到新版。**
     *    `com.google.android.play:app-update:2.1.0` 相依的是
     *    `androidx.fragment:fragment:1.0.0`（2018 年），
     *    Play Console 會把它列進「下次發布版本前應處理的問題 → 技術品質」。
     *    這一行只是用來覆蓋傳遞相依的版本，程式碼本身沒有用到 fragment。
     *    升級 app-update 之後可以回來確認還需不需要。
     */
    implementation(libs.androidx.fragment)

    /**
     * Play 的「應用程式內更新」。
     *
     * ⚠️ 這個套件**不會加 INTERNET 權限** —— 它是透過 IPC 去問手機上
     *    已安裝的 Play 商店 App，查詢與下載都由 Play 商店做。
     *    實測合併後的 manifest 只多一個自家的
     *    DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION（signature 層級，不對外）。
     * ⚠️ **升級這個套件之後一定要重驗**：
     *      gradlew processDebugMainManifest
     *      grep uses-permission app/build/intermediates/merged_manifest/debug/.../AndroidManifest.xml
     *    一旦看到 INTERNET 就立刻退版 —— 那是整個隱私主張的根。
     */
    implementation("com.google.android.play:app-update:2.1.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
