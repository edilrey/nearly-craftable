# Nearly Craftable: Development Plan

A client-side Forge 1.20.1 addon for EMI. It shows a list of recipes that the player can almost craft: the player has some of the ingredients, and 1 to N items are missing.

Target pack: Beyond Depth (ATLauncher client, Forge 1.20.1). The server does not need this mod.

---

## 1. Requirements

### 1.1 Functional

| ID | Requirement |
|---|---|
| F1 | Read all crafting recipes from the EMI recipe manager. This includes modded recipes that EMI shows. |
| F2 | Compare each recipe with the player inventory. Support tag ingredients (for example, "any log"). |
| F3 | Keep a recipe if the player has at least 1 matching ingredient and the number of missing items is between 1 and `maxMissing`. |
| F4 | Sort the results: fewest missing items first, then highest percentage of matched items. |
| F5 | Show the results in a list screen. Each row shows the output item, the output count, and the missing items with quantities. |
| F6 | Open the list screen with a keybind (default `N`). The keybind must work in the world and in container screens. |
| F7 | When the player clicks a row, open that recipe in EMI. |
| F8 | Give a search field to filter the list by output name. |
| F9 | Give a client config: `maxMissing` (default 2), `minMatched` (default 1), `maxResults` (default 200), recipe categories (default: crafting only). |

### 1.2 Non-functional

| ID | Requirement |
|---|---|
| N1 | Client-side only. If a user puts the JAR on a dedicated server, the server must not crash. |
| N2 | Players without the mod can join a server where other players have it. |
| N3 | A full recalculation must take less than 100 ms with all Beyond Depth recipes loaded. |
| N4 | Recalculate only when the inventory changes or the screen opens. Never on each tick. |
| N5 | The matcher must not depend on Minecraft classes, so that unit tests run without the game. |

### 1.3 Out of scope (version 1)

- Recursive crafting (craft the missing intermediate items).
- Ingredients from chests, backpacks, or storage networks.
- Automatic grid fill. EMI does this from the recipe view.
- Machine recipes (furnace, modded workstations). Add later through the categories config.

---

## 2. Technology

| Item | Value |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.2.0 minimum. Test on the Forge version of the installed Beyond Depth instance. |
| Java | 17 |
| Build | ForgeGradle 6, official mappings (Parchment optional) |
| EMI | Compile against the EMI API for Forge 1.20.1 (1.1.23 or newer). Runtime dependency in dev. |
| Tests | JUnit 5 |
| CI | GitHub Actions |
| License | MIT (recommended) |

Mod ID: `nearlycraftable`
Package: `io.github.<github-user>.nearlycraftable`

Before you write code, verify the EMI Maven repository URL and the artifact name for the Forge 1.20.1 API in the EMI documentation or the EMI GitHub README. Do not guess them.

---

## 3. Architecture

```
nearlycraftable/
  NearlyCraftable.java          mod entry, registers client setup only on Dist.CLIENT
  client/
    ClientSetup.java            keybind, event listeners, config registration
    InventorySnapshot.java      counts items in the player inventory, gives a change hash
    RecipeSource.java           reads recipes from EMI, converts them to matcher input
    ResultCache.java            holds the last results, recalculates on change
    NearlyCraftableScreen.java  list screen, search, scroll, click handling
  core/
    ItemKey.java                identity of an item (item id + relevant NBT)
    RecipeInput.java            list of ingredient slots, each with allowed keys and amount
    Inventory.java              map of ItemKey to count
    MatchResult.java            recipe ref, matched count, missing list
    Matcher.java                pure matching logic, no Minecraft imports
  config/
    ClientConfig.java           ForgeConfigSpec values
```

### 3.1 Data flow

1. The player presses `N`.
2. `InventorySnapshot` makes a hash of the inventory. If the hash is the same as the last hash, `ResultCache` returns the old results.
3. If the hash changed, `RecipeSource` gives the converted recipes (cached after the first call, cleared when EMI reloads).
4. `Matcher` makes the results.
5. `NearlyCraftableScreen` shows the results.

### 3.2 Matching algorithm (core/Matcher)

For each recipe:

1. Make a copy of the inventory counts.
2. Sort the ingredient slots by the number of allowed items, smallest first. Specific slots must take items before tag slots.
3. For each slot, find an allowed key with enough count. Subtract the amount. If no key has enough, add the slot to the missing list.
4. `matched = slots - missing`. Keep the recipe if `matched >= minMatched` and `1 <= missingItems <= maxMissing`.
5. Group the missing list by item for display (for example, "2x Iron Ingot").

This greedy method can give a wrong result in rare cases with overlapping tags. This is acceptable for version 1. Write a unit test that records the known limit.

### 3.3 Client-only safety (N1, N2)

- Register all client code through `FMLClientSetupEvent` and `@Mod.EventBusSubscriber(value = Dist.CLIENT)`.
- Do not reference client classes from the main mod class.
- In `mods.toml`, set `displayTest="IGNORE_ALL_VERSION"` so that the server and other clients do not need the mod.
- Set the EMI dependency `side="CLIENT"`.

---

## 4. Development steps

Each step ends with a commit. Do not start a step before the previous step passes its check.

### Step 1: Project setup
1. Make the project from the Forge 1.20.1 MDK.
2. Set the mod ID, group, and package.
3. Add the EMI API as a compile dependency and EMI as a runtime dependency for `runClient`.
4. Add JUnit 5.
5. Add `.gitignore` for Gradle, IDE, and `run/` folders.

Check: `./gradlew build` passes. `./gradlew runClient` starts the game with EMI.

### Step 2: Core matcher
1. Write `ItemKey`, `RecipeInput`, `Inventory`, `MatchResult`, `Matcher`.
2. Write unit tests:
   - Recipe with all items present: not in the results (0 missing).
   - Recipe with 1 item missing: in the results.
   - Recipe with no matched items: not in the results.
   - Tag slot with two allowed items: uses either item.
   - Same item used in two slots: counts are subtracted correctly.
   - Amount above 1 in one slot.
   - Sort order.
   - Known greedy limit (marked as expected behavior).

Check: `./gradlew test` passes.

### Step 3: EMI recipe source
1. Read the crafting recipes from the EMI recipe manager.
2. Convert each input (`EmiIngredient`) to a `RecipeInput` slot. Skip empty inputs.
3. Skip recipes that EMI marks as not supported or hidden, and recipes with no item output.
4. Cache the result. Clear the cache when EMI reloads recipes.
5. Log the number of recipes and the time for conversion.

Check: In `runClient`, a debug command or log line shows the recipe count.

### Step 4: Inventory snapshot and cache
1. Count the items in the main inventory, hotbar, and offhand.
2. Make a hash from the counts.
3. `ResultCache` recalculates only when the hash changes.
4. Log the time for each recalculation.

Check: The log shows a recalculation only after an inventory change.

### Step 5: Keybind and list screen
1. Register the keybind `N` in the "Nearly Craftable" category.
2. Open the screen from the world (client tick key check) and from container screens (`ScreenEvent.KeyPressed`).
3. Show rows: output icon, output name, missing items as icons with counts.
4. Add a scroll bar and a search field.
5. Show a tooltip with the full missing list on hover.
6. On click, open the recipe in EMI.
7. Press `Esc` or `N` to close and go back to the previous screen.

Check: In `runClient` with a few vanilla items, the list shows the expected recipes.

### Step 6: Config
1. Add `ClientConfig` with the values from F9.
2. Read the values in `Matcher` and `RecipeSource`.

Check: A change of `maxMissing` in the config file changes the list after a restart.

### Step 7: Polish
1. Add a language file (`en_us.json`) for all text.
2. Add a mod icon and a description in `mods.toml`.
3. Write `README.md`: purpose, requirements (EMI), install, keybind, config.
4. Write `CHANGELOG.md`.

---

## 5. Testing

### 5.1 Automatic
- Unit tests for `core/` run in CI on each push.

### 5.2 Dev environment (`runClient`)
| Test | Expected result |
|---|---|
| Empty inventory | Screen shows "No results" |
| 1 oak log | Shows recipes that use planks are not in the list (log is not planks). Shows recipes that use logs directly with missing items |
| 3 planks + 1 stick | Shows wooden pickaxe with "1x Stick" missing |
| Search "pick" | Only pickaxe rows show |
| Click a row | EMI opens that recipe |
| Open from a crafting table screen | Screen opens, `Esc` goes back to the crafting table |

### 5.3 Beyond Depth test copy (ATLauncher)
1. Build the JAR: `./gradlew build`. The file is in `build/libs/`.
2. In ATLauncher, click **Open Folder** on "Beyond Depth - QoL Test". Put the JAR in `mods`.
3. Start the game. Make sure the game loads without errors.
4. Do the tests from 5.2 with modded items.
5. Read `logs/latest.log`. Record the recipe count and the recalculation time. The time must be less than 100 ms (N3).
6. Play for 30 minutes. Monitor the frame rate when you open the screen.

### 5.4 Server test (ATBP)
1. Join the ATBP server with the mod installed. The connection must work.
2. Join with a second client without the mod. The connection must work.
3. Do not install the mod on the server.

### 5.5 Release checklist
- [ ] All unit tests pass.
- [ ] All tests in 5.2 and 5.3 pass.
- [ ] Server test 5.4 passes.
- [ ] Version number updated in `gradle.properties`.
- [ ] `CHANGELOG.md` updated.

---

## 6. Publishing

The destination is CurseForge (ATLauncher installs mods from CurseForge and Modrinth). Modrinth is optional.

### 6.1 One-time setup
1. Make a CurseForge author account.
2. Create a new project: Minecraft > Mods. Name: "Nearly Craftable". Category: Utility & QoL. Add the summary, description (from README), icon, and license.
3. Wait for CurseForge to approve the project. Record the project ID.
4. Make a CurseForge API token (account settings > API tokens).
5. In the GitHub repo, add the secret `CURSEFORGE_TOKEN`.
6. Optional: do the same for Modrinth (`MODRINTH_TOKEN`).

### 6.2 CI workflows
1. `build.yml`: on each push and pull request, run `./gradlew build test`. Upload the JAR as an artifact.
2. `release.yml`: on a tag `v*`:
   - Build the JAR.
   - Make a GitHub Release with the JAR and the changelog section.
   - Upload to CurseForge (and Modrinth) with a publish action (for example, `Kir-Antipov/mc-publish`). Set: game version 1.20.1, loader Forge, release type, required dependency EMI, environment client.

### 6.3 Release procedure
1. Update the version and the changelog.
2. Commit, then make a tag: `git tag v1.0.0` and `git push --tags`.
3. Monitor the workflow. Make sure the file shows on CurseForge.
4. In ATLauncher, remove the manual JAR from the test copy. Install the mod with **Add Mods** to check that the CurseForge file works.

---

## 7. Milestones

| Version | Content |
|---|---|
| 0.1.0 | Steps 1 to 5. Private test in the Beyond Depth test copy |
| 0.2.0 | Step 6 and 7. First CurseForge release (beta) |
| 1.0.0 | Fixes from testing. Release type: release |
| Later | Machine recipe categories, nearby chests, recursive "craft the missing parts" |

---

## 8. Rules for the coding agent

- Write documentation, commit messages, and README text in ASD-STE100 Simplified Technical English.
- Do not use emojis or em dashes.
- Add code comments only when the code is not clear. Do not comment self-explanatory code.
- Keep `core/` free of Minecraft imports.
- Verify EMI and Forge API names against the sources before use. Do not guess.
- Make one commit for each step in section 4.