# InvView - Chill Zone Fork (Minecraft 26.2)

Version: `1.4.21-chillzone2`

Based on InvView 1.4.21 by Potatoboy9999 / PotatoPresident under the MIT License.

## Chill Zone change

The original inventory and ender-chest behavior is intentionally preserved.
This fork adds server-history autocomplete to:

- `/view inv <player>`
- `/view echest <player>`

### Autocomplete behavior

- Suggestions only contain players who have actually been seen on this server.
- Suggestions filter by what staff has typed (`pro` can suggest `ProGamer`).
- On first startup, the mod imports the existing vanilla `usercache.json` so old players are available immediately.
- Every joining player is then remembered by UUID + latest known name.
- The persistent list is saved in `config/chill_zone_invview_players.json`.
- Name changes update the remembered name for the same UUID.

### Manual username lookup is preserved

The command still uses Minecraft's original `GameProfileArgument`. This means the autocomplete list is server-only, but staff may still manually type a valid username that has never joined the server and InvView will attempt the same normal profile resolution as upstream 1.4.21.

## Installation

This is a replacement for the original InvView JAR, not an add-on. It keeps the same mod id (`invview`).

1. Build this project through the included GitHub Actions workflow.
2. Remove `InvView-1.4.21-26.2+.jar` from the server's `mods` folder.
3. Put the built `InvView-ChillZone-...jar` in the `mods` folder.
4. Start the server.

Do not run the original InvView JAR and this fork at the same time.

## Permissions preserved

- `invview.command.root`
- `invview.command.inv`
- `invview.command.echest`
- `invview.protected`
- `invview.can_modify`

## Upstream

Original project: https://github.com/PotatoPresident/InvView
Original 1.4.21 release: Minecraft 26.2, Fabric/Quilt.


## Chill Zone 2 startup fix

The remembered-player registry now initializes on `SERVER_STARTED` instead of `SERVER_STARTING`.
Minecraft 26.2 can still have a null `PlayerList` during `SERVER_STARTING`, which caused the
server-start crash seen in the first Chill Zone build. A defensive null guard is also included.
