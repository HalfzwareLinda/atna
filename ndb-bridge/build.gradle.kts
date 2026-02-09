import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
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
        namespace = "com.atna.ndb"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":quartz"))
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        // Shared JVM+Android source set for LMDB storage via rust-nostr SDK
        val jvmAndroid =
            create("jvmAndroid") {
                dependsOn(commonMain.get())
                dependencies {
                    implementation(libs.rust.nostr.sdk.jvm.get().toString()) {
                        // Exclude JNA JAR — quartz already provides JNA AAR for Android and JAR for JVM
                        exclude(group = "net.java.dev.jna", module = "jna")
                    }
                }
            }

        jvmMain {
            dependsOn(jvmAndroid)
        }

        androidMain {
            dependsOn(jvmAndroid)
        }

        val jvmAndroidTest =
            create("jvmAndroidTest") {
                dependsOn(commonTest.get())
                dependencies {
                    implementation(libs.kotlinx.coroutines.test)
                }
            }

        jvmTest {
            dependsOn(jvmAndroidTest)
        }
    }
}
