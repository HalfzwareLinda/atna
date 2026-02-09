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

        // mdk-kotlin dependencies will be added when MDK integration is implemented.
        // For now, the MdkMarmotManager stubs don't call MDK, so no dependency needed.
        // When ready, add per-platform:
        //   androidMain: implementation("com.github.marmot-protocol:mdk-kotlin:0.5.2@aar")
        //   jvmMain: implementation("com.github.marmot-protocol:mdk-kotlin:0.5.2")
    }
}
