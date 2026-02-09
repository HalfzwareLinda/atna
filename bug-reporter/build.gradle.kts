import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    androidLibrary {
        namespace = "com.atna.bugreport"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":quartz"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        // Must be defined before androidMain and jvmMain
        val jvmAndroid = create("jvmAndroid") {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.okhttp)
            }
        }

        jvmMain {
            dependsOn(jvmAndroid)
        }

        androidMain {
            dependsOn(jvmAndroid)
        }
    }
}
