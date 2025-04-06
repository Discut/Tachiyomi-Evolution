pluginManagement {
    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

rootProject.name = "TachiyomiJ2K"
include(":app")
