# Chill Zone InvView Autocomplete 0.2.5 (Minecraft 26.3)

This is **an add-on**, not a replacement for original `InvView-1.4.22-26.3+.jar`.
It remembers player UUID and names across joins, saves `config/chillzone-invview/players.tsv`, imports historical names from old cache and provides narrowed player-name suggestions for `/view inv <name>` and `/view echest <name>`.

## Build / diagnostics

GitHub Actions uses Java 25, Gradle 9.7.1, Fabric Loom 1.18-SNAPSHOT, Fabric API 0.163.0+26.3.
The workflow captures all logs under the `invview-gradle-build-log` artifact and, on build failure, prints likely errors directly in the Build step.
Run GitHub Actions → Build Chill Zone InvView Autocomplete → Run workflow, or push to main.
Successful build: use the non-sources JAR from `build/libs` with original InvView.

**Status:** Files and Java syntax structure inspected, but compilation and live testing have NOT succeeded in this environment (Java 25 + build downloads unavailable). Do not remove your working InvView until the new add-on build passes.

## Changes from 0.2.4
- Moved command-tree patching from command registration callback to `SERVER_STARTED`, after all commands are registered.
- Uses Fabric's documented Loom 1.18-SNAPSHOT line.
- Prints real errors on GitHub failures and always uploads a full log.
- Original mod, selectors, permissions, and inventory logic are not altered.

