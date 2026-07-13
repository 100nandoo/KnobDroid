import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appVersionPropertiesFile = rootProject.file("app/version.properties")
val appVersionProperties =
  Properties().apply {
    check(appVersionPropertiesFile.exists()) {
      "Missing app version properties file: ${appVersionPropertiesFile.path}"
    }
    appVersionPropertiesFile.inputStream().use(::load)
  }

fun Properties.requiredInt(name: String): Int =
  getProperty(name)?.toIntOrNull() ?: error("Missing integer property '$name' in app/version.properties")

fun Properties.requiredString(name: String): String =
  getProperty(name)?.trim()?.takeIf(String::isNotBlank)
    ?: error("Missing string property '$name' in app/version.properties")

fun Project.findSigningPropertiesFile(): File? =
  listOf("../signing/keystore.properties", "../../signing/keystore.properties")
    .map(::file)
    .firstOrNull(File::exists)

fun File.resolveFromParent(path: String): File {
  val candidate = File(path)
  return if (candidate.isAbsolute) candidate else parentFile.resolve(path).normalize()
}

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "dev.halim.knobdroid"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  signingConfigs {
    val keystorePropertiesFile = project.findSigningPropertiesFile()
    if (keystorePropertiesFile != null) {
      val properties = Properties().apply { load(keystorePropertiesFile.inputStream()) }

      create("release") {
        storePassword = properties.getProperty("storePassword")
        keyPassword = properties.getProperty("keyPassword")
        keyAlias = properties.getProperty("keyAlias")
        storeFile = keystorePropertiesFile.resolveFromParent(properties.getProperty("storeFile"))
      }
    }
  }

  defaultConfig {
    applicationId = "dev.halim.knobdroid"
    minSdk = 29
    targetSdk = 36
    versionCode = appVersionProperties.requiredInt("VERSION_CODE")
    versionName = appVersionProperties.requiredString("VERSION_NAME")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      if (signingConfigs.names.contains("release")) {
        signingConfig = signingConfigs.getByName("release")
      }
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.acra.core) {
    exclude(group = "com.google.auto.service", module = "auto-service")
  }
  implementation(libs.acra.toast) {
    exclude(group = "com.google.auto.service", module = "auto-service")
  }
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }
