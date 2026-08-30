plugins {
    id("com.android.application")
}

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

/** Robolectric / GUI classes — used by optional backendTestDebugUnitTest only. */
val robolectricUnitTestClasses = listOf(
    "org.girino.frac.android.foss.BottomSheetPickerHelperTest",
    "org.girino.frac.android.foss.FormulaPreviewTest",
    "org.girino.frac.android.foss.JuliaParamsStoreTest",
    "org.girino.frac.android.foss.MandelbrotViewGestureTest",
    "org.girino.frac.android.foss.PaletteSwatchTest",
    "org.girino.frac.android.foss.PhoenixParamsStoreTest",
    "org.girino.frac.android.foss.SessionStoreTest",
    "org.girino.frac.android.foss.ViewportExportTest",
    "org.girino.frac.android.foss.ViewportSessionTest",
)

val releaseKeystoreFile = providers.environmentVariable("ANDROID_KEYSTORE_FILE").orNull
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseKeystoreFile,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "org.girino.frac.android.foss"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.girino.frac.android.foss"
        minSdk = 21
        targetSdk = 36
        versionCode = 11
        versionName = "1.2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            // Local dev builds: append compile timestamp (yyyyMMddHHmmss) to versionName.
            val devVersionStamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now())
            versionNameSuffix = "-$devVersionStamp"
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = false
            all {
                it.useJUnitPlatform()
                it.maxHeapSize = "512m"
                it.maxParallelForks = 1
                it.timeout.set(Duration.ofMinutes(4))
            }
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity:1.9.3")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.12.2")
}

afterEvaluate {
    val backendOnlyRequested =
        project.hasProperty("backendTests")
            || gradle.startParameter.taskNames.any { it.contains("backendTestDebugUnitTest") }

    tasks.named<Test>("testDebugUnitTest").configure {
        if (backendOnlyRequested) {
            // Backend classes shut down their own ExecutorServices in @After.
            // forkEvery=1 made CI pay ~4s JVM boot per class and hit the 3 min timeout.
            forkEvery = 0L
            filter {
                robolectricUnitTestClasses.forEach { excludeTestsMatching(it) }
            }
        } else {
            // Full local suite includes MandelbrotView Robolectric tests; isolate JVMs
            // so a leftover render thread cannot hang the rest of the suite.
            forkEvery = 1L
        }
    }

    tasks.register("backendTestDebugUnitTest") {
        group = "verification"
        description =
            "Backend unit tests only (no Robolectric). CI uses this task; local full suite: testDebugUnitTest."
        dependsOn("testDebugUnitTest")
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR,
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        showStandardStreams = true
    }
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) = Unit

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                logger.lifecycle(
                    "UNIT TEST SUITE FINISHED: ${suite.displayName} -> ${result.resultType}"
                )
            }
        }

        override fun beforeTest(testDescriptor: TestDescriptor) {
            logger.lifecycle("UNIT TEST START: ${testDescriptor.displayName}")
        }

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            logger.lifecycle(
                "UNIT TEST END: ${testDescriptor.displayName} -> ${result.resultType}"
            )
        }
    })
}
