# MiraRename

MiraRename adds consumable two-stage item name tags to the Mira Paper server suite.

## Download

[**Download MiraRename v0.1.0**](https://github.com/FiveSOCE/Mira-Rename/releases/download/v0.1.0/MiraRename-0.1.0.jar)

[View All Releases](https://github.com/FiveSOCE/Mira-Rename/releases)

## Requirements

- Paper 1.21.11
- Java 21
- MiraItems optional, but recommended when renaming protected/signed MiraItems

## How it works

1. Give a player an Item Name Tag with `/mirarename give <player> [amount]`.
2. The player right-clicks the tag.
3. MiraRename privately captures their next chat message as the requested item name. The message is not broadcast.
4. The NAME_TAG is renamed/prepared with that entered name.
5. The player places the prepared tag on their cursor and clicks it onto the item they want to rename.
6. One rename tag is consumed and the target item receives the selected display name.

Typing `cancel` instead of a name cancels the chat prompt.

Each issued rename tag carries its own hidden token ID and is intentionally non-stackable, preventing the preparation state from leaking across a stack of tags.

When MiraItems is installed, MiraRename calls MiraItems' identity-preserving rename bridge before falling back to ordinary ItemMeta renaming. This allows protected MiraItems to keep their signed identity instead of being broken by an external metadata edit.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/mirarename give <player> [amount]` | `mirarename.admin` | Gives one or more Item Name Tags. |
| `/mrename give <player> [amount]` | `mirarename.admin` | Alias. |

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `mirarename.admin` | OP | Administrative tag issuance. |
| `mirarename.use` | Everyone | Allows a player to prepare and apply Item Name Tags. |

## Configuration

`config.yml` controls:

- tag material
- tag display name/lore
- maximum entered name length
- prompt/cancel/success/error messages

## Verified release

- Version: **0.1.0**
- JAR: `MiraRename-0.1.0.jar`
- SHA-256: `986cd993ae855a0685710bb29b2f244f00b4664660414a6b29c40393b87e9bc6`
