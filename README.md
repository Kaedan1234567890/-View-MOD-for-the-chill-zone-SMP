# Chill Zone InvView 0.2.7 – targeted Gradle fix

This package is **uncompiled and untested**. The actual previous log reported `Could not find method modImplementation()` at `build.gradle:10`. This update replaces both `modImplementation` dependencies with standard Gradle `implementation` dependencies, which are adequate for compile-time use with non-obfuscated Minecraft 26.3, and explicitly adds Fabric Maven to dependency resolution. The original InvView 1.4.22 26.3 JAR in `reference-original` is unchanged. The addon is installed **alongside**, not instead of, the original InvView JAR.

GitHub Actions runs Java 25, Gradle 9.8.0, Loom 1.19.3, and uploads `invview-gradle-build-log` even on failure. If the next error involves `minecraft(...)`, that means Loom itself did not create its configurations; do not change Java features to work around it.

IMPORTANT: Old autocomplete code has not been live tested. This package does not claim to be a working compiled JAR.
