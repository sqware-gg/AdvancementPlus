# AdvancementPlus

[![Build](https://github.com/sqware-gg/AdvancementPlus/actions/workflows/build.yml/badge.svg)](https://github.com/sqware-gg/AdvancementPlus/actions/workflows/build.yml)

AdvancementPlus replaces vanilla advancement chat announcements with configurable Paper messages. It can announce completed advancements and criterion progress, which is useful for SMPs, challenge servers, event servers, and custom datapack progression.

It is built for modern Paper server owners who want advancement messages that look intentional instead of noisy or inconsistent.

## Links

- Website: https://sqware.gg
- Plugin information and support: https://discord.sqware.gg

## Compatibility

- Server software: Paper `26.1.2+`
- Java: `25+`
- Build tool: Maven
- Server internals: no NMS

Paper 26.1+ requires Java 25. If your host still runs Java 21 or older, upgrade the server runtime before installing this plugin.

## Features

- Replaces vanilla advancement completion messages.
- Optional criterion progress messages for multi-step advancements.
- MiniMessage or legacy formatting.
- Hover text with advancement details and progress.
- Configurable broadcast audience: all players, same world, permission, or self.
- Namespace and advancement filters for vanilla, datapack, and custom advancements.
- Recipe advancement filtering by default to avoid unlock spam.
- Optional sounds for progress and completion.
- Admin inspection tools for diagnosing custom advancement keys and criteria.

## Installation

1. Download the latest AdvancementPlus jar from GitHub Releases.
2. Stop your Paper server.
3. Put the jar in your server `plugins` folder.
4. Start the server once to generate `plugins/AdvancementPlus/config.yml`.
5. Review the filters, templates, and broadcast audience.
6. Restart the server, or run `/advancementplus reload`.

By default, AdvancementPlus sets `showAdvancementMessages=false` for loaded worlds so vanilla chat messages do not duplicate the custom messages.

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

Format templates use placeholders such as:

```text
<player>, <display_name>, <world>, <title>, <description>, <key>,
<namespace>, <path>, <frame>, <criterion>, <completed>, <total>,
<remaining>, <percent>, <bar>
```

## Recommended Server Owner Defaults

- Keep recipe advancements excluded unless you intentionally want every recipe unlock.
- Keep `announce-single-criterion: false`; the completion message is enough for one-step advancements.
- Use `audience: "world"` for survival networks where world context matters.
- Use `audience: "permission"` if only staff, spectators, or event viewers should see advancement messages.

## Updating

AdvancementPlus keeps configuration in `plugins/AdvancementPlus/config.yml`. Review new default configs when updating between versions, especially if new placeholders or filter options are added.

## Build From Source

```powershell
./mvnw.cmd package
```

The compiled jar is written to:

```text
target/AdvancementPlus-0.1.0-SNAPSHOT.jar
```

## Troubleshooting

- If players see duplicate advancement messages, confirm `gamerule.auto-disable-announce-advancements` is enabled and check each world's `showAdvancementMessages` value with `/advancementplus status`.
- If a datapack advancement does not show, inspect it with `/advancementplus inspect namespace:path` and check namespace/path filters.
- If progress is too noisy, increase `progress.cooldown-millis` or disable progress messages.
- If nothing broadcasts to players, check `broadcast.audience` and the `advancementplus.see` permission when using permission mode.

## Support

For setup help, compatibility questions, and plugin information, use https://discord.sqware.gg.
