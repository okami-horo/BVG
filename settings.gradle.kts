pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo1.maven.org/maven2/")
        maven("https://androidx.dev/storage/compose-compiler/repository/")
        // VLC Android库的Maven仓库
        maven("https://download.videolan.org/pub/maven2/")
        //maven("https://artifact.bytedance.com/repository/releases/")
    }
    versionCatalogs {
        create("androidx") { from(files("gradle/androidx.versions.toml")) }
        create("gradleLibs") { from(files("gradle/gradle.versions.toml")) }
    }
}
rootProject.name = "BV"
include(":app")
include(":bili-api")
include(":bili-subtitle")
include(":bv-player")
include(":libs:av1Decoder")
include(":libs:ffmpegDecoder")
include(":libs:libVLC")
include(":bili-api-grpc")
