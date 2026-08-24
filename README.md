# MarsRankup

Fully configurable Paper rankup plugin with YAML GUIs, Vault economy, PlaceholderAPI requirements, playtime requirements, permission requirements, state-based rank items and custom rewards.

## Requirements

- Paper 1.21.x
- Java 21
- PlaceholderAPI (optional; required for placeholder requirements)
- Vault + economy provider (optional; required for money-required)

## Build

```bash
mvn -B -ntp clean package
```

or:

```bash
./build.sh
```

The jar is created as `target/MarsRankup-<version>.jar`.

## Automatic releases

Push a tag like:

```bash
git tag v1.0.0
git push origin v1.0.0
```

`.github/workflows/release.yml` builds with Java 21, creates/updates the GitHub Release and uploads the jar. Normal pushes and pull requests are compiled by `.github/workflows/build.yml`.

## Dynamic command

```yaml
command:
  name: "rankup"
  aliases:
    - "ranks"
    - "rank"
  permission: "marsrankup.use"
```

Commands:

```text
/rankup
/rankup claim [rank]
/rankup open [gui]
/rankup info
/rankup reload
/rankup set <player> <rank>
/rankup reset <player>
```

Admin permission: `marsrankup.admin`.

## Rank configuration

Players start at rank 0 by default. Rank 0 does not need to exist in `ranks.yml`.

Supported money suffixes: `k`, `m`, `b`, `t`.

Supported playtime suffixes: `s`, `m`, `h`, `d`, `w`; combinations like `1d12h` work.

Custom requirements support `>=`, `<=`, `>`, `<`, `==`, `!=` after PlaceholderAPI resolution:

```yaml
custom-requirements:
  - "%vault_eco_balance% >= 1k"
  - "%levelsplus_level% >= 2"
  - "%some_boolean_placeholder% == true"
```

Each rank can have `claimed`, `in-progress` and `claimable` item states.

## GUIs

Every `.yml` file under `plugins/MarsRankup/guis/` becomes a GUI.

Supported GUI sections: `title`, `size`, `border`, `panes`, `decorations`, `items`.

Both `slot` and `slots` are supported, including ranges such as `"0-8"`. Explicit `items:` override generated rank items.

## Item builder

Supported item keys include:

```yaml
material: DIAMOND
amount: 1
name: "&bExample"
display_name: "&bExample"
lore:
  - "&7Line"
custom-model-data: 123
glow: true
unbreakable: false
item-flags:
  - HIDE_ENCHANTS
enchantments:
  UNBREAKING: 1
actions:
  - "[SOUND] UI_BUTTON_CLICK 1 1"
```

## Actions / rewards

Supported prefixes:

```text
[CONSOLE]
[PLAYER]
[COMMAND]
[CMD]
[MESSAGE]
[BROADCAST]
[SOUND]
[CLOSE]
[GUI]
[MENU]
[OPEN_MENU]
```

Untagged reward strings are console commands by default.

## PlaceholderAPI

```text
%marsrankup_rank%
%marsrankup_rank_name%
%marsrankup_next_rank%
%marsrankup_next_rank_name%
%marsrankup_can_claim_next%
%marsrankup_can_claim_1%
%marsrankup_requirement_money_1%
%marsrankup_requirement_playtime_1%
%marsrankup_requirement_permissions_1%
%marsrankup_requirement_custom_1%
```

## Colors

Supports legacy `&` colors, `&#RRGGBB` and `<#RRGGBB:#RRGGBB>Gradient`.
