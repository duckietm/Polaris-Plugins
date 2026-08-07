# BuildHeight

Lets a user override the furniture stacking height in rooms they have build
rights in, through a small in-client popup — like the radio/soundboard widget.

When the plugin is loaded, a **build-height button appears on the room toolbar**
(and only then — the client is told the feature is available on room entry).
Clicking it opens a draggable card with a slider and −/+ buttons to set the
height live. **Closing the popup resets the height to 0** (clears the override,
back to normal stacking).

The button only appears to users who can **build in the current room** (owner or
given rights) — so it is hidden for visitors, in rooms you can't build in, and
outside rooms (hotel view etc.), and it appears/disappears live as room rights
are given or taken. On top of that it is gated by the `cmd_buildheight` rank
permission (`0` = denied, `1` = allowed) — see [Database setup](#database-setup).
Build rights are always required, so the button never shows where the user can't
place furniture, whatever the permission value.

The override is per-session and per-room: it lives in the user's stats cache and
is cleared automatically when they close the popup, lose rights, or leave the
room. Decimals are supported (0.1 steps); the default range is **-40 to 40**,
configurable via `building.cmd_buildheight.min_height` / `max_height` in emulator
settings.

This is a modernized rebuild of a 2018-era plugin. The old `:buildheight` / `:bh`
chat command has been removed in favour of the popup, and it runs **no schema or
permission SQL at boot**.

## Client requirement

The popup needs the matching **client** changes that ship in this monorepo:

- `Nitro-Renderer` — two custom packets (`BuildHeightAvailableEvent` in,
  `SetBuildHeightComposer` out; headers 9350 / 9351).
- `Nitro-UI` — the `useBuildHeight` hook, the `BuildHeightWidgetView` popup, and
  the toolbar button.

So after updating this plugin, **rebuild/redeploy your Nitro client** (renderer +
UI) for the popup to show. The packet header ids (9350 / 9351) are defined in
[`network/BuildHeightProtocol.java`](src/main/java/com/furnibuilder/buildheight/network/BuildHeightProtocol.java)
and mirrored in the renderer's header tables — keep them in sync if you change
them. **No core Polaris changes are needed**: the plugin sends the outgoing
packet itself and registers the incoming handler at runtime.

## Requirements

- JDK 25 and Maven 3.9+
- The Polaris Gameserver artifact (`com.eu.habbo:Polaris`) in your local Maven
  repository. Build/install the Gameserver once so the `provided` dependency
  resolves:

  ```
  cd /path/to/Polaris && mvn install -DskipTests
  ```

  If your Gameserver version differs, update `<version>` for the `Polaris`
  dependency in `pom.xml` to match.

## Build

```
cd plugins/BuildHeight
mvn package
```

This produces `target/BuildHeight.jar`.

## Install

1. Copy the jar into the server's plugins directory:

   ```
   cp target/BuildHeight.jar /path/to/Polaris/plugins/
   ```

2. Run the database setup (see below).
3. Restart (or start) the Gameserver.
4. Rebuild/redeploy your Nitro client (see [Client requirement](#client-requirement)).

On enable the plugin seeds its height-range config defaults (insert-if-absent, so
a hotel's customised values are never overwritten) and registers the popup's
packet handler.

## Database setup

Run [`sql/buildheight.sql`](sql/buildheight.sql) once against your Polaris
database:

```
mysql -u <user> -p <database> < sql/buildheight.sql
```

It grants the `cmd_buildheight` permission (via `permission_definitions`). **This
is required** — the popup is gated by that permission, so without it the toolbar
button appears for nobody. The default grants every rank (`1`); set a rank to `0`
to deny it. Note the button is *also* always gated on room build rights, so a
granted rank still only sees it where they can actually place furniture. If your
hotel still uses the legacy wide `permissions` table, uncomment the
`ALTER TABLE permissions` alternative in that file instead.

The same file also carries the height-range `emulator_settings` — those are
optional (the plugin seeds them at boot).

## Security

The one attacker-controlled entry point is the client→server set packet
(`9351`); its payload is treated as fully untrusted. Layers of protection:

- **Authentication** — the emulator only dispatches the packet to an
  authenticated session (the handler is not `@NoAuthMessage`).
- **Permission + build rights** — rejected unless `cmd_buildheight` allows the
  user for this room (value 2 requires room build rights). The client-side
  "available" flag is cosmetic; the server re-checks on every packet, so a
  patched/spoofed client gains nothing.
- **In-room + range** — ignored if the user isn't in a room, or if the height is
  outside the configured min/max (no overflow / absurd values reach the cache).
- **Rate limit** — `SetBuildHeightEvent` enforces a per-client cooldown
  (100 ms); the client throttles sends so normal use never trips it.
- **Blast radius** — an override only affects furniture the user *places
  themselves*, which already requires build rights in that room. It cannot touch
  other users' furniture or other rooms.
- **Crash-safe** — malformed/truncated packets are caught by the dispatcher and
  by the underflow-safe wire reads; the worst case is the override being cleared.
- The server→client availability packet (`9350`) has no server handler, so a
  client cannot spoof it to the server.

## What changed from the 2018 version

- Replaced the `:buildheight` / `:bh` chat command with an in-client popup
  (toolbar button + draggable card), available only while the plugin is loaded.
- Standard Maven layout (`src/main/java`, `src/main/resources`) and a real
  `pom.xml`; the jar is built, not hand-assembled.
- All runtime DDL (`CREATE` / `ALTER TABLE`) removed from the plugin; the popup
  is gated by the `cmd_buildheight` permission, granted via `sql/buildheight.sql`.
- No core Polaris changes: custom packets are sent by the plugin and the incoming
  handler is registered at runtime.
- Java 25 idioms (pattern-matching `instanceof`, no decompiler artifacts), SLF4J
  logging, idempotent one-shot registration, and cache cleanup on room exit.
