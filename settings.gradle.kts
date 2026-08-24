// 镜像开关 controlme.useMirror:
//   - 本机(国内)默认 true:走阿里云镜像提速;
//   - CI(海外 runner)在 .github/ci-gradle.properties 设 false:走官方源,
//     避免个别包在镜像缺同步/不同步导致解析失败。

pluginManagement {
    repositories {
        val useAliyunMirror: Boolean =
            providers.gradleProperty("controlme.useMirror").map { it.toBoolean() }.getOrElse(true)
        if (useAliyunMirror) {
            maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val useAliyunMirror: Boolean =
            providers.gradleProperty("controlme.useMirror").map { it.toBoolean() }.getOrElse(true)
        if (useAliyunMirror) {
            maven(url = "https://maven.aliyun.com/repository/google")
            maven(url = "https://maven.aliyun.com/repository/central")
            maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "ControlMe"
include(":app")