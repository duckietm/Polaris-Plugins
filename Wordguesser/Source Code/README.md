# WordGuesser Plugin

Announces a scrambled word to the hotel on a schedule; the first user to guess
it with `:rw <word>` wins a prize (points/credits and optionally a badge).

## Requirements

- Polaris emulator 4.2.60+ (the `Gameserver/Emulator` in this repository)
- JDK 25 (same as the emulator)

## Database setup

All SQL lives in the Flyway migration
`Gameserver/Emulator/src/main/resources/db/migration/V20260721000000__wordguesser_plugin.sql`
and is applied automatically the next time the emulator starts. The plugin
itself performs **no** DDL and **no** writes to texts/settings/permission
tables at runtime. If you use the plugin against a database not managed by
this emulator's migrations, run that .sql file manually once.

The migration provides:

- `random_words` table (with a starter word list) — add your own words there
  and run `:update_words` to reload without restarting.
- `emulator_texts` entries (`randomword.*`, `commands.keys.cmd_randomword`,
  `commands.keys.cmd_update_words`) — existing values are never overwritten.
- `emulator_settings` entries:
  - `randomword.prize.currency_type` — `-1` = credits, otherwise a points type (default `5`)
  - `randomword.prize.currency_amount` — default `1`
  - `randomword.prize.badge` — badge code, empty = no badge
  - `randomword.interval_seconds` — time between words (default `1800`, min 60)
  - `randomword.guess_window_seconds` — time to guess (default `300`, min 30)
- `permission_definitions` entries (normalized schema — the legacy
  `permissions` table is not touched):
  - `cmd_randomword` — enabled for ranks 1–7
  - `cmd_update_words` — rank 7 only

## Build & install

```sh
cd Gameserver/Emulator && mvn install -DskipTests   # once, provides the Polaris artifact
cd ../../Plugins/Wordguesser && mvn package
cp target/wordguesser-2.0.jar <emulator working dir>/plugins/
```

## Commands

| Command | Permission | Default access |
|---|---|---|
| `:rw <word>` | `cmd_randomword` | ranks 1–7 |
| `:update_words` | `cmd_update_words` | rank 7 |
