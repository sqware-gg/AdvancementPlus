# AdvancementPlus

[![Build](https://github.com/sqware-gg/AdvancementPlus/actions/workflows/build.yml/badge.svg)](https://github.com/sqware-gg/AdvancementPlus/actions/workflows/build.yml)

AdvancementPlus is a Paper advancement message plugin for Minecraft servers. It replaces vanilla advancement chat with configurable completion and progress messages for survival servers, SMPs, quest servers, events, and datapack-heavy worlds.

Use it when you want custom advancement messages, MiniMessage formatting, hover text, progress bars, and broadcast control without editing datapacks or relying on NMS.

## Features

- Custom advancement completion messages.
- Optional criterion progress messages.
- MiniMessage and legacy color support.
- Hover text with title, description, key, criterion, and progress.
- Configurable progress bars and sounds.
- Filters for namespaces, advancement keys, hidden advancements, no-display advancements, root advancements, and recipes.
- Broadcast audiences: all players, same world, permission, or only the player.
- Console mirroring and admin inspection commands.

## Requirements

- Paper `26.1.2+`
- Java `25+`
- Maven wrapper included
- No NMS

Paper 26.1+ requires Java 25. Hosts still on Java 21 need a runtime upgrade before running this plugin.

## Commands

```text
/advancementplus
/advancementplus status
/advancementplus reload
/advancementplus list [namespace] [page]
/advancementplus inspect <namespace:path>
```

Aliases: `/advplus`, `/advancementsplus`

## Permissions

```text
advancementplus.admin  - admin commands, default op
advancementplus.see    - receive broadcasts when audience is permission, default true
```

## Configuration Notes

By default, AdvancementPlus disables vanilla advancement chat output for loaded worlds so players do not see duplicate messages.

Common settings:

```yaml
broadcast:
  audience: "all" # all, world, permission, self

filter:
  include-namespaces:
    - "*"
  exclude-advancements:
    - "minecraft:recipes/*"

progress:
  enabled: true
  announce-single-criterion: false
  announce-final-criterion: false
```

Useful placeholders:

```text
<player>, <display_name>, <world>, <title>, <description>, <key>,
<namespace>, <path>, <frame>, <criterion>, <completed>, <total>,
<remaining>, <percent>, <bar>
```

Recommended defaults for public survival servers:

- Keep recipe advancements excluded.
- Keep `announce-single-criterion: false`.
- Use `audience: "world"` when players are split across survival, nether, resource, or event worlds.
- Use `audience: "permission"` for staff-only or event-only advancement feeds.

## Build

```powershell
.\mvnw.cmd package
```

The jar is written to `target/AdvancementPlus-0.1.0.jar`.

## Support

- Website: https://sqware.gg
- Discord: https://discord.sqware.gg

AdvancementPlus is licensed under the Apache License, Version 2.0.
