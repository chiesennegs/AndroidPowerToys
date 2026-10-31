plugins { id("com.android.library") }
android {
 namespace = "io.github.muntashirakon.adb"
 compileSdk { version = release(37) }
 defaultConfig { minSdk = 28 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 lint { checkReleaseBuilds = false }
}
dependencies {
 implementation(project(":spake2"))
 implementation("androidx.annotation:annotation:1.9.1")
 implementation("org.bouncycastle:bcprov-jdk15to18:1.81")
 implementation("org.conscrypt:conscrypt-android:2.8.0")
 testImplementation("junit:junit:4.13.2")
}
