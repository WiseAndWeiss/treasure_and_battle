import java.util.Properties
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    alias(libs.plugins.android.application)
    jacoco
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val amapApiKey: String = localProperties.getProperty("AMAP_API_KEY") ?: "MISSING_API_KEY"

android {
    namespace = "com.example.treasure_and_battle"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.treasure_and_battle"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["amapApiKey"] = amapApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 高德3D地图 SDK
    implementation("com.amap.api:3dmap:latest.release")
    implementation("com.google.code.gson:gson:2.10.1")
    // Mock Android 环境
    testImplementation("org.robolectric:robolectric:4.10.3")
    // Mockito 用于模拟对象
    testImplementation("org.mockito:mockito-core:5.3.1")
}

// ====================== JaCoCo 测试覆盖率配置 ======================

// 配置所有测试类型启用 JaCoCo
tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
    finalizedBy("jacocoTestReport")
}

tasks.register("jacocoTestReport", JacocoReport::class) {
    group = "verification"
    description = "生成单元测试覆盖率报告"

    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(file("${layout.buildDirectory.get()}/reports/coverage/html"))
        xml.outputLocation.set(file("${layout.buildDirectory.get()}/reports/coverage/xml/report.xml"))
    }

    // 排除不需要覆盖的类
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/*Test.*",
        "android/**/*.*",
        "**/android/**",
        "**/databinding/**",
        "**/generated/**",
        "**/.*\\\$\\\$robo\\\$\\\$.*",
        "**/.*Robolectric.*",
        "**/.*\\\$Shadow.*"
    )

    // Java 编译的 class
    val javaTree = fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes").get()) {
        exclude(fileFilter)
    }

    // Kotlin 编译的 class（如果有）
    val kotlinTree = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug").get()) {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(javaTree, kotlinTree))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    executionData.setFrom(fileTree(layout.buildDirectory.dir("outputs/unit_test_code_coverage/debugUnitTest").get()) {
        include("*.exec")
    })
}
