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
/hdb search <query>
/hdb info <remote-id>
/agivehead <remote-id> <target|self> <amount> [reason]
/agiveplayerhead <name|uuid> <target|self> <amount> [reason]
/arefreshheaddb [reason]
/averifyheaddb [reason]
/aheaddbstatus [reason]
```

`/headdb` is registered as an alias. Players open the paginated GUI with `/hdb`;
console callers receive text help/search output. Quoted command targets resolve
against online character names only. Each admin root declares its own native
Fabric Permission API v1 node, evaluated by LuckPerms:

```text
vantablack.command.agivehead
vantablack.command.agiveplayerhead
vantablack.command.arefreshheaddb
vantablack.command.averifyheaddb
vantablack.command.aheaddbstatus
```

Grant these nodes directly to LuckPerms groups. The old `/hdb give`, `/hdb
player`, `/hdb refresh`, `/hdb verify`, and `/hdb status` paths use the matching
node and remain transition aliases through release `0.3.0`. Canonical commands
continue to accept an optional trailing reason for command-line compatibility,
but HeadDB does not persist or forward it. Console and RCON retain access.

## Configuration

The mod writes `config/mod_headdb/config.properties` on first boot.

```properties
remote.manifest-url=https://data.headsdb.com/manifest.json
remote.preferred-mirror-id=primary
remote.cache-directory=cache
startup.load-cache=true
startup.refresh-remote=true
commands.search-result-limit=10
```

## Build

```bash
./gradlew build
```

The jar is produced under `build/libs/`.

When a sibling `mod-characters` checkout is available, Gradle substitutes that
source build automatically. Standalone and CI builds resolve the pinned
immutable `mod-characters` release from `cdn.vantablack4.com`; they do not need
credentials for another source repository.
