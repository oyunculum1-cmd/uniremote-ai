// Root build file. Plugin versions are applied in app/build.gradle.kts.
// Check https://maven.google.com and https://search.maven.org for the
// latest stable versions before building — versions below are a known-good
// baseline at time of writing, not guaranteed to be the newest release.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
