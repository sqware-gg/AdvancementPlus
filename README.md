# AdvancementPlus

[![Build](https://github.com/sqware-gg/AdvancementPlus/actions/workflows/build.yml/badge.svg)](https://github.com/sqware-gg/AdvancementPlus/actions/workflows/build.yml)

**Join the SQWARE Discord: [discord.sqware.gg](https://discord.sqware.gg).**

AdvancementPlus is a Paper advancement message plugin for Minecraft servers. It replaces vanilla advancement chat with configurable completion and progress messages for survival servers, SMPs, quest servers, events, and datapack-heavy worlds.

Use it when you want custom advancement messages, MiniMessage formatting, hover text, progress bars, and broadcast control without editing datapacks or relying on NMS.

## Features

- Custom advancement completion messages.
- Optional criterion progress messages.
- MiniMessage and legacy color support.
- Hover text with title, description, key, criterion, and progress.
- Configurable progress bars and sounds.
- Filters for namespaces, advancement keys, hidden advancements, no-display advancements, root advancements, and recipes.
- Optional completion and milestone reward commands with one-time claim history.
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
/advancementplus rewards
/advancementplus rewards progress [player]
/advancementplus rewards clear <player> [namespace:path|reward-id|*]
```

Aliases: `/advplus`, `/advancementsplus`

## Permissions

```text
advancementplus.admin  - admin commands, default op
advancementplus.see    - receive broadcasts when audience is permission, default true
advancementplus.reward.exempt - skip configured reward commands, default false
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

rewards:
  enabled: false
  first-time-only: true
  allowed-game-modes:
    - "SURVIVAL"
  exclude-advancements:
    - "minecraft:recipes/*"
    - "minecraft:story/root"
  frame-defaults:
    task:
      commands: []
    goal:
      commands: []
    challenge:
      commands: []
  advancements:
    minecraft:end/kill_dragon:
      commands:
        - "eco give <player> 5000"
  milestones:
    enabled: true
    selection:
      include-namespaces:
        - "minecraft"
      exclude-advancements:
        - "minecraft:recipes/*"
    completed-counts:
      25:
        commands:
          - "eco give <player> 1000"
    tab-completions:
      minecraft:end/root:
        commands:
          - "eco give <player> 10000"
    all-frames:
      challenge:
        commands:
          - "lp user <player> permission set sqware.title.challenge_hunter true"
```

Useful placeholders:

```text
<player>, <display_name>, <world>, <title>, <description>, <key>,
<namespace>, <path>, <frame>, <criterion>, <completed>, <total>,
<remaining>, <percent>, <bar>
```

Reward command placeholders:

```text
<player>, <uuid>, <world>, <key>, <namespace>, <path>, <frame>,
<criterion>, <completed>, <total>, <remaining>, <percent>, <x>, <y>, <z>
<milestone>, <milestone_value>, <milestone_completed>, <milestone_total>,
<milestone_remaining>, <milestone_percent>
```

Reward strategy:

- Exact advancement rewards are best for known high-value achievements such as the dragon, wither, beacon, netherite, Trial Chamber, or collection challenges.
- Count milestones are safer than a global reward for every easy advancement because they reward long-term progression instead of early-game spam.
- Tab completion rewards work well for Nether, End, Adventure, and Husbandry arcs.
- All-frame and all-selected rewards should usually be prestige rewards: titles, cosmetics, roles, tags, particles, or rare keys.
- Custom milestone groups can represent server-defined arcs without pricing every advancement individually.

Recommended defaults for public survival servers:

- Keep recipe advancements excluded.
- Keep `announce-single-criterion: false`.
- Use `audience: "world"` when players are split across survival, nether, resource, or event worlds.
- Use `audience: "permission"` for staff-only or event-only advancement feeds.
- Keep reward commands disabled until exact advancement rewards are chosen.
- Prefer exact and milestone rewards for hard goals, challenges, and completion arcs over global task rewards.
- Leave no-display and root advancement rewards excluded unless a datapack requires them.
- Use `advancementplus.reward.exempt` for staff, test accounts, or automation accounts.

Reward history is stored in `reward-history.yml` under the plugin data folder. With `first-time-only: true`, AdvancementPlus records each paid reward before dispatching commands so duplicate completion events cannot pay twice.

## Build

```powershell
.\mvnw.cmd package
```

The jar is written to `target/AdvancementPlus-0.1.0.jar`.

## License

AdvancementPlus is licensed under the Apache License, Version 2.0.
