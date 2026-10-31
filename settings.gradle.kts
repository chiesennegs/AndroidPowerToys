pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "AndroidPowerToys"
include(":app")

include(":libadb", ":spake2")
project(":libadb").projectDir = file("third_party/libadb")
project(":spake2").projectDir = file("third_party/spake2")
