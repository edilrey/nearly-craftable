package io.github.edilrey.nearlycraftable.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Hand-written copies of vanilla 1.20.1 crafting recipes, with the planks and logs tags expanded. */
class VanillaRecipesTest {
    private static final ItemKey STICK = ItemKey.of("minecraft:stick");
    private static final ItemKey COBBLE = ItemKey.of("minecraft:cobblestone");
    private static final ItemKey COAL = ItemKey.of("minecraft:coal");
    private static final ItemKey OAK_PLANKS = ItemKey.of("minecraft:oak_planks");
    private static final ItemKey BIRCH_PLANKS = ItemKey.of("minecraft:birch_planks");
    private static final ItemKey OAK_LOG = ItemKey.of("minecraft:oak_log");
    private static final ItemKey BIRCH_LOG = ItemKey.of("minecraft:birch_log");

    private static final RecipeInput.Slot PLANKS_1 = tag(1, OAK_PLANKS, BIRCH_PLANKS);
    private static final RecipeInput.Slot PLANKS_3 = tag(3, OAK_PLANKS, BIRCH_PLANKS);
    private static final RecipeInput.Slot PLANKS_8 = tag(8, OAK_PLANKS, BIRCH_PLANKS);

    private static RecipeInput.Slot tag(int amount, ItemKey... keys) {
        return new RecipeInput.Slot(new LinkedHashSet<>(List.of(keys)), amount);
    }

    private static RecipeInput recipe(String id, int count, RecipeInput.Slot... slots) {
        return new RecipeInput("minecraft:" + id, id, ItemKey.of("minecraft:" + id), count, List.of(slots));
    }

    private static final List<RecipeInput> RECIPES = List.of(
            recipe("wooden_pickaxe", 1, PLANKS_3, tag(2, STICK)),
            recipe("stone_pickaxe", 1, tag(3, COBBLE), tag(2, STICK)),
            recipe("chest", 1, PLANKS_8),
            recipe("torch", 4, tag(1, COAL), tag(1, STICK)),
            recipe("stick", 4, tag(2, OAK_PLANKS, BIRCH_PLANKS)),
            recipe("crafting_table", 1, tag(4, OAK_PLANKS, BIRCH_PLANKS)),
            recipe("oak_planks", 4, tag(1, OAK_LOG)),
            recipe("birch_planks", 4, tag(1, BIRCH_LOG)));

    private static Set<String> ids(List<MatchResult> results) {
        Set<String> ids = new LinkedHashSet<>();
        results.forEach(r -> ids.add(r.recipe().id()));
        return ids;
    }

    @Test
    void threePlanksAndOneStickShowsWoodenPickaxeMissingOneStick() {
        Inventory inv = new Inventory().add(OAK_PLANKS, 3).add(STICK, 1);
        List<MatchResult> results = Matcher.match(inv, RECIPES, 1, 2, 200);
        MatchResult pickaxe = results.stream()
                .filter(r -> r.recipe().id().equals("minecraft:wooden_pickaxe")).findFirst().orElseThrow();
        assertEquals(1, pickaxe.missingCount());
        assertEquals(Set.of(STICK), pickaxe.missing().get(0).allowed());
        assertEquals(1, pickaxe.missing().get(0).amount());
    }

    @Test
    void oneLogDoesNotListPlankBasedRecipes() {
        Inventory inv = new Inventory().add(OAK_LOG, 1);
        Set<String> listed = ids(Matcher.match(inv, RECIPES, 1, 2, 200));
        assertTrue(listed.isEmpty(), "log crafts planks fully, and no other recipe uses a log: " + listed);
    }

    @Test
    void removingOneIngredientChangesTheList() {
        Inventory full = new Inventory().add(OAK_PLANKS, 3).add(STICK, 2);
        Inventory less = new Inventory().add(OAK_PLANKS, 3).add(STICK, 1);
        assertTrue(!ids(Matcher.match(full, RECIPES, 1, 2, 200)).contains("minecraft:wooden_pickaxe"));
        assertTrue(ids(Matcher.match(less, RECIPES, 1, 2, 200)).contains("minecraft:wooden_pickaxe"));
    }

    @Test
    void mixedPlankTypesFillOneTagSlot() {
        Inventory inv = new Inventory().add(OAK_PLANKS, 2).add(BIRCH_PLANKS, 1).add(STICK, 2);
        assertTrue(!ids(Matcher.match(inv, RECIPES, 1, 3, 200)).contains("minecraft:wooden_pickaxe"),
                "2 oak + 1 birch planks and 2 sticks craft a wooden pickaxe");
    }

    @Test
    void mixedPlankTypesCountTowardsAPartialMatch() {
        Inventory inv = new Inventory().add(OAK_PLANKS, 2).add(BIRCH_PLANKS, 1);
        MatchResult table = Matcher.match(inv, RECIPES, 1, 3, 200).stream()
                .filter(r -> r.recipe().id().equals("minecraft:crafting_table")).findFirst().orElseThrow();
        assertEquals(1, table.missingCount());
    }
}
