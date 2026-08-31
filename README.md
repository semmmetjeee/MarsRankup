# MarsRankup

MarsRankup is a configurable Paper rankup plugin with YAML GUIs, Vault economy support, PlaceholderAPI requirements, playtime and permission requirements, state-based rank items and custom rewards.

## Requirements

- Paper 1.21.x
- Java 21
- PlaceholderAPI (optional; required for placeholder-based requirements)
- Vault + an economy provider (optional; required for `money-required`)
- A valid Mars Development license for the `mars-rankup` product

## Licensing

On the first start MarsRankup creates only:

```text
plugins/MarsRankup/license.yml
```

Paste your license key into:

```yaml
license-key: "MARS-..."
```

and restart the server. Normal configuration files are only created after successful license validation.

The plugin validates against:

```text
https://mars-license-api.vanderlandsem8.workers.dev/api/license/validate
```

and re-checks the license every five minutes while running. A revoked, renewed, blocked, mismatched or globally disabled license disables MarsRankup on its next check. After three consecutive network verification failures the plugin also disables as a safety measure.

Standard licenses are intended for two active server instances by default; the actual limit is controlled by the Mars Development license platform and can be changed by an administrator.

> No distributed Java JAR can be literally impossible to patch. The authoritative license, instance and revocation state is kept server-side.

## Build

```bash
mvn -B -ntp clean package
```

or:

```bash
./build.sh
```

The jar is created as `target/MarsRankup-<version>.jar`.

## Commands

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

## Configuration

The bundled defaults are fully English and use an orange + silver theme. Main behavior is configured in `config.yml`; rank definitions live in `ranks.yml`; GUI files live under `guis/`.

Players start at rank `0` by default. Rank `0` does not need to exist in `ranks.yml`.

Supported money suffixes: `k`, `m`, `b`, `t`.

Supported playtime suffixes: `s`, `m`, `h`, `d`, `w`; combinations such as `1d12h` work.

Custom requirements support `>=`, `<=`, `>`, `<`, `==`, `!=` after PlaceholderAPI resolution:

```yaml
custom-requirements:
  - "%vault_eco_balance% >= 1k"
  - "%levelsplus_level% >= 2"
  - "%some_boolean_placeholder% == true"
```

## GUIs

Every `.yml` file under `plugins/MarsRankup/guis/` becomes a GUI.

Supported sections include `title`, `size`, `border`, `panes`, `decorations` and `items`. Both `slot` and `slots` are supported, including ranges such as `"0-8"`.

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

MarsRankup supports legacy `&` colors, `&#RRGGBB` hex colors and `<#RRGGBB:#RRGGBB>Gradient` syntax.
