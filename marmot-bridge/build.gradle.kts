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
        namespace = "com.atna.marmot"
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

        // mdk-kotlin is Android-only (AAR with native .so libraries).
        // Desktop JVM uses StubMarmotManager instead.
        androidMain {
            dependencies {
                implementation(libs.mdk.kotlin.get().toString()) {
                    exclude(group = "net.java.dev.jna", module = "jna")
                }
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
