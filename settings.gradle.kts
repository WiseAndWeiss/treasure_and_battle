pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        // 高德地图仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 权限库仓库
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 高德地图仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 权限库仓库
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "treasure_and_battle"
include(":app")
 