import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.isseikz"
version = "1.0.2-SNAPSHOT"

kotlin {
    androidLibrary {
        namespace = "io.github.isseikz.kmpinput"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(
                    JvmTarget.JVM_11
                )
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.runtime)
        }
    }
}

mavenPublishing {
    // publishToMavenCentral() // Disabled for local testing

    // signAllPublications() // Disabled for local testing

    coordinates(group.toString(), "kmp-terminal-input", version.toString())

    pom {
        name = "KMP Terminal Input"
        description = "A Kotlin Multiplatform library for abstracting terminal input on Android and iOS."
        inceptionYear = "2025"
        url = "https://github.com/isseikz/kmp-terminal-input"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "isseikz"
                name = "Issei Kuzumaki"
                url = "https://github.com/isseikz"
            }
        }
        scm {
            url = "https://github.com/isseikz/kmp-terminal-input"
            connection = "scm:git:git://github.com/isseikz/kmp-terminal-input.git"
            developerConnection = "scm:git:ssh://git@github.com/isseikz/kmp-terminal-input.git"
        }
    }
}
