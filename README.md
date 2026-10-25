# Nearly Craftable

Nearly Craftable is a client-side Forge 1.20.1 addon for EMI. It shows a list of recipes that you can almost craft. You have some of the ingredients, and only a few items are missing.

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0 or newer
- EMI 1.1.23 or newer

The server does not need this mod. Players without the mod can join a server where other players have it.

## Install

1. Install EMI.
2. Put the Nearly Craftable JAR in the `mods` folder.
3. Start the game.

## Use

Press `N` in the world or in a container screen (for example, a crafting table). The list shows each recipe with the output item and the missing items. Tag ingredients, such as "any log", show one item at a time and change every second.

- Type in the search field to filter the list by output name.
- Hover over a row to see the full list of missing items.
- Click a row to open the recipe in EMI.
- Press `Esc` or `N` to close the list and go back to the previous screen.

You can change the key in the controls menu, in the "Nearly Craftable" category.

## Config

The file is `config/nearlycraftable-client.toml`. Restart the game after you change it.

| Key | Default | Description |
|---|---|---|
| `maxMissing` | 2 | Maximum number of missing items for a recipe to be listed. |
| `minMatched` | 1 | Minimum number of ingredient items that you already have. |
| `maxResults` | 200 | Maximum number of recipes in the list. |
| `categories` | `["minecraft:crafting"]` | EMI recipe category ids to search. |

## Known limits

- The matcher is greedy. In rare cases with overlapping tag ingredients, it can show a recipe as missing one item when the recipe is craftable.
- Only the main inventory, the hotbar, and the offhand count. Chests and backpacks do not count.
- Item NBT data is ignored.

## Build

Use Java 17. Run `./gradlew build`. The JAR is in `build/libs/`. Run `./gradlew test` for the unit tests.

## License

MIT
