# AdvancementPlus

[![Build](https://github.com/sqware-gg/AdvancementPlus/actions/workflows/build.yml/badge.svg)](https://github.com/sqware-gg/AdvancementPlus/actions/workflows/build.yml)

AdvancementPlus is a Minecraft advancement message plugin for modern Paper servers. It replaces the default advancement chat output with clean, configurable messages for SMPs, survival servers, quest servers, challenge events, and custom datapack progression.

Use it when you want advancement completion messages, advancement progress messages, MiniMessage formatting, hover text, filters, and broadcast controls without editing datapacks or server internals.

## Links

- Website: https://sqware.gg
- Support and plugin updates: https://discord.sqware.gg

## Compatibility

- Server software: Paper `26.1.2+`
- Java: `25+`
- Build tool: Maven
- Server internals: no NMS

Paper 26.1+ requires Java 25. If your host still runs Java 21 or older, upgrade the server runtime before installing this plugin.

## Why Server Owners Use It

- Make advancement announcements look like part of your server branding.
- Show useful progress for multi-step advancements and custom quests.
- Reduce recipe unlock spam while keeping meaningful achievements visible.
- Control who receives messages: everyone, same world, permission group, or only the player.
- Support vanilla, datapack, and custom advancement namespaces.

## Features

- Custom advancement completion messages.
- Optional criterion progress messages.
- MiniMessage and legacy color formatting.
- Hover text with title, description, key, criterion, and progress.
- Configurable progress bars.
- Namespace, advancement key, hidden, no-display, and root advancement filters.
- Optional sounds for progress and completion.
- Console mirroring for staff visibility.
- Admin commands for status, reload, listing, and inspecting advancement keys.

## Installation

1. Download the latest jar from the GitHub Releases page.
2. Stop your Paper server.
3. Put the jar in the server `plugins` folder.
4. Start the server once to generate `plugins/AdvancementPlus/config.yml`.
5. Review message templates, filters, and broadcast audience.
6. Restart the server, or run `/advancementplus reload`.

By default, AdvancementPlus disables vanilla advancement chat output for loaded worlds so players do not see duplicate messages.

## Commands

```text
/advancementplus
/advancementplus status
/advancementplus reload
/advancementplus list [namespace] [page]
/advancementplus inspect <namespace:path>
```

Aliases:

```text
/advplus
/advancementsplus
```

## Permissions

```text
advancementplus.admin  - use admin commands, default op
advancementplus.see    - receive broadcasts when audience is permission, default true
```

## Configuration Highlights

Broadcast audience:

```yaml
broadcast:
  audience: "all" # all, world, permission, self
```

Filtering:

```yaml
filter:
  include-namespaces:
    - "*"
  exclude-advancements:
    - "minecraft:recipes/*"
```

Progress messages:

```yaml
progress:
  enabled: true
  announce-single-criterion: false
  announce-final-criterion: false
```

Template placeholders:

```text
<player>, <display_name>, <world>, <title>, <description>, <key>,
<namespace>, <path>, <frame>, <criterion>, <completed>, <total>,
<remaining>, <percent>, <bar>
```

## Recommended Defaults

- Keep recipe advancements excluded on public survival servers.
- Keep `announce-single-criterion: false`; completion messages already cover one-step advancements.
- Use `audience: "world"` when players are split across survival, nether, event, or resource worlds.
- Use `audience: "permission"` for staff-only, event-only, or donor-only advancement feeds.

## Updating

Keep your existing `plugins/AdvancementPlus/config.yml`. When updating, compare it with the latest default config if new placeholders, filters, or message options are added.

Release history is tracked in [CHANGELOG.md](CHANGELOG.md).

## Build From Source

```powershell
./mvnw.cmd package
```

The compiled jar is written to:

```text
target/AdvancementPlus-0.1.0.jar
```

## Troubleshooting

- Duplicate messages: run `/advancementplus status` and confirm `showAdvancementMessages=false` in each world.
- Datapack advancement missing: inspect it with `/advancementplus inspect namespace:path` and check namespace/key filters.
- Too much progress chat: increase `progress.cooldown-millis` or disable progress messages.
- No players see messages: check `broadcast.audience` and `advancementplus.see` if using permission mode.

## Support

For setup help, compatibility questions, and plugin updates, use https://discord.sqware.gg.
