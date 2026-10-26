# Chill Zone InvView 0.2.0 (Minecraft 26.3)

This **source project has not been compiled or server-tested** in this environment.

## What it does
- Bundles the unchanged, MIT-licensed InvView 1.4.22-26.3+ JAR as a nested Fabric mod, so the final artifact can replace the original InvView jar rather than requiring two installed files. The original author remains credited and the original license is included.
- Keeps the `/view inv <player>` and `/view echest <player>` commands and InvView's original LuckPerms permission nodes, including `invview.command.root`, `invview.command.inv`, `invview.command.echest`, `invview.can_modify`, and `invview.protected`.
- Adds case-insensitive prefix completion for remembered visitors to the existing `target` command argument. Suggestions shrink as you type and remain available after a player logs off.
- Keeps vanilla selector and online-name suggestions by merging rather than replacing the game's provider.
- Records each joining player's UUID and current name in `config/chillzone-invview/players.tsv` using an atomic file replacement where supported.
- Imports old `config/chill_zone_invview_players.json` from the Minecraft 26.2 Chill Zone fork and the vanilla `usercache.json` (if present). These original files are left untouched. Also saves every new join to the permanent registry.

## Building with GitHub
1. Create a new repository; upload *all* files and folders in this project, including `.github/workflows/build.yml` and `libs/original-invview.jar`.
2. Open Actions → Build Chill Zone InvView → Run workflow.
3. On success, download the artifact and use `chillzone-invview-0.1.0+26.3.jar` (the remapped/output release jar; avoid `-sources` or `-dev` variants).
4. Remove the standalone `InvView-1.4.22-26.3+.jar` from your server's mods folder **if the built Chill Zone JAR contains the nested original mod**, to avoid a duplicate mod ID. Keep Fabric API and LuckPerms installed.
5. Back up the server, start a test instance, and test `/view inv`, `/view echest`, selectors, online and offline names, and permissions before using in production.

If Gradle reports a dependency/remapping or mapping error, do not deploy the artifact until corrected.

Original InvView source: https://github.com/PotatoPresident/InvView
Original copyright: (c) 2020 Potatoboy9999, MIT (see LICENSE-INVVIEW)


## 0.2.1: Gradle plugin variant resolution update

This candidate updates Fabric Loom from 1.18.1 to 1.19.3, Fabric API from
0.160.5+26.3 to 0.163.0+26.3, and retains Java 25 / Minecraft 26.3.
This addresses the likely plugin variant mismatch shown in the partial GitHub log;
no Java gameplay or permission code has been altered. The build has NOT been
verified. If configuration still fails, capture the **first** error starting
with `Could not resolve`/`No matching variant`, not the Gradle help footer.

## 0.2.2 build fix
GitHub Actions now uses Gradle 9.8.0 instead of 9.2.1, meeting Fabric Loom 1.19.3's minimum Gradle requirement. This fixes the Gradle version mismatch reported in the user's GitHub Actions log. Compilation and runtime still need testing.

## 0.2.3 build configuration update
- Uses Fabric's documented Minecraft 26.3 Loom 1.18-SNAPSHOT and Gradle 9.7.1 combination.
- Keeps original MIT-licensed InvView nested as a jar and preserves player-history extension and LuckPerms nodes.
- GitHub build avoids huge stacktrace, making the first underlying error more visible.
- This package has not been compiled here. If it fails, send the *first* 'What went wrong' section, not the bottom of the stacktrace.
