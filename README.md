# Vantablack HeadDB

Fabric server-side port of [SilentDevelopment/HeadDB](https://github.com/SilentDevelopment/HeadDB) for the Vantablack Minecraft server.

This fork keeps the upstream Apache-2.0 API/core database engine and replaces the Paper platform with a Fabric command and server-side GUI layer. It does not include Vault economy integration, favorites, or admin edit menus.

## Features

- Verified remote HeadDB catalog downloads from `https://data.headsdb.com/manifest.json`.
- Local artifact cache under `config/mod_headdb/cache`.
- Server commands for status, GUI search, info, remote refresh, remote verification, remote head giving, and player head giving.
- Chest GUI for `/hdb`, `/hdb gui`, and `/hdb search <query>` with paginated remote head results.
- Modern Minecraft item components for textured `PLAYER_HEAD` stacks.

## Commands

```text
/hdb
/hdb gui [query]
/hdb status
/hdb search <query>
/hdb info <remote-id>
/hdb give <remote-id> [amount]
/hdb give <remote-id> <player> [amount]
/hdb player <name|uuid> [amount]
/hdb player <name|uuid> <player> [amount]
/hdb refresh
/hdb verify
```

`/headdb` is registered as an alias. Players open the paginated GUI with `/hdb`; console callers receive text help/search output. Clicking a head in the GUI gives one copy to the viewer when they have the configured admin permission level. Admin commands use the same configured permission level, defaulting to operator level `2`.

## Configuration

The mod writes `config/mod_headdb/config.properties` on first boot.

```properties
remote.manifest-url=https://data.headsdb.com/manifest.json
remote.preferred-mirror-id=primary
remote.cache-directory=cache
startup.load-cache=true
startup.refresh-remote=true
commands.search-result-limit=10
commands.admin-permission-level=2
```

## Build

```bash
./gradlew build
```

The jar is produced under `build/libs/`.
