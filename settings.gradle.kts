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
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      setUrl("https://jitpack.io")
    }
    maven {
      setUrl("https://maven.mozilla.org/maven2/")
    }
    maven { setUrl("https://artifact.bytedance.com/repository/releases/") }
  }
}

rootProject.name = "DongYuTvWeb"
include(":app")
include(":easydanmaku")
