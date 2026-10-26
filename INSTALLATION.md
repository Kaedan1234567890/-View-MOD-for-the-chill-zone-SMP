# Chill Zone InvView 26.3 (0.2.4 source candidate)

**This is source only and is NOT verified compiled.** Original InvView 1.4.22 is already compatible with Minecraft 26.3. The custom JAR built by this project is a companion autocomplete add-on, NOT a replacement for original InvView.

## Install two JARs
1. Keep `InvView-1.4.22-26.3+.jar` in the test server mods folder (a copy is in `reference-original/` in this ZIP).
2. Build this add-on using GitHub Actions (Java 25, Gradle 9.7.1, Loom 1.18.4).
3. Put the built `chillzone-invview-autocomplete-0.2.4+26.3.jar` in mods alongside original InvView and required Fabric API. Remove older Chill Zone InvView copies to avoid duplicate command registration.
4. Preserve server data and config. Test online/offline player autocomplete with `/view inv P` and `/view echest P`.
5. Original InvView is still responsible for `/view` commands, permissions, offline inventory loading and GUI. This add-on only augments suggestions.

## If build fails
Go to the GitHub Actions run, download the artifact `invview-gradle-build-log`. The build step intentionally writes the full log **even on failure**. This solves the missing-error-information problem, but doesn't itself guarantee a compile.

## License
The bundled original InvView JAR is an unchanged reference copy under its MIT license; original author copyright retained. Do not install the `reference-original` directory itself.
