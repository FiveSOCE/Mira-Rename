# MiraRename

## v0.1.1 fixes

- Right-clicking a rename tag in **air** reliably starts the naming prompt, even when another plugin has already cancelled the underlying interaction event.
- Right-clicking a block with the tag still starts the same flow.
- Entered item names support legacy ampersand color/format codes such as `&aGreen`, `&6&lLegendary`, `&nUnderlined` and `&r`.
- Signed MiraItems keep their identity-preserving rename path while rendering the same legacy formatting.


MiraRename adds consumable two-stage item name tags to the Mira Paper server suite.

## Download

[**Download MiraRename v0.1.1**](https://github.com/FiveSOCE/Mira-Rename/releases/download/v0.1.2/MiraRename-0.1.2.jar)

[View All Releases](https://github.com/FiveSOCE/Mira-Rename/releases)

## Requirements

- Paper 1.21.11
- Java 21
- MiraItems optional, but recommended when renaming protected/signed MiraItems

## How it works

1. Give a player an Item Name Tag with `/mirarename give <player> [amount]`.
2. The player right-clicks the tag in the air or on a block.
3. MiraRename privately captures their next chat message as the requested item name. The message is not broadcast.
4. Legacy ampersand color/format codes are supported in the entered name, including `&0-&9`, `&a-&f`, `&k-&o` and `&r`.
5. The NAME_TAG is renamed/prepared with the rendered colored/formatted name.
6. The player places the prepared tag on their cursor and clicks it onto the item they want to rename.
7. One rename tag is consumed and the target item receives the selected display name.

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
- JAR: `MiraRename-0.1.1.jar`
