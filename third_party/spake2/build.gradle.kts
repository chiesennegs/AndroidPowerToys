plugins { id("com.android.library") }
val rebuildNative = providers.gradleProperty("rebuildSpake").orNull == "true"
android {
 namespace = "io.github.muntashirakon.crypto.spake2"
 compileSdk { version = release(37) }
 defaultConfig { minSdk = 28 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 if(rebuildNative) {
  sourceSets.getByName("main").jniLibs.setSrcDirs(emptyList<String>())
  externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
 }
}
dependencies { implementation("androidx.annotation:annotation:1.9.1") }
