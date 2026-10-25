package io.github.edilrey.nearlycraftable.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MatcherTest {
    private static final ItemKey PLANKS = ItemKey.of("planks");
    private static final ItemKey STICK = ItemKey.of("stick");
    private static final ItemKey OAK = ItemKey.of("oak_planks");
    private static final ItemKey BIRCH = ItemKey.of("birch_planks");
    private static final ItemKey IRON = ItemKey.of("iron_ingot");
    private static final ItemKey X = ItemKey.of("x");
    private static final ItemKey Y = ItemKey.of("y");
    private static final ItemKey Z = ItemKey.of("z");

    private static RecipeInput.Slot slot(int amount, ItemKey... keys) {
        Set<ItemKey> allowed = new LinkedHashSet<>(List.of(keys));
        return new RecipeInput.Slot(allowed, amount);
    }

    private static RecipeInput recipe(String id, RecipeInput.Slot... slots) {
        return new RecipeInput(id, id, ItemKey.of(id), 1, List.of(slots));
    }

    private static List<MatchResult> run(Inventory inv, int max, RecipeInput... recipes) {
        return Matcher.match(inv, List.of(recipes), 1, max, 100);
    }

    @Test
    void fullyCraftableRecipeIsNotListed() {
        Inventory inv = new Inventory().add(PLANKS, 3).add(STICK, 2);
        assertTrue(run(inv, 2, recipe("pick", slot(3, PLANKS), slot(2, STICK))).isEmpty());
    }

    @Test
    void oneMissingItemIsListed() {
        Inventory inv = new Inventory().add(PLANKS, 3).add(STICK, 1);
        List<MatchResult> r = run(inv, 2, recipe("pick", slot(3, PLANKS), slot(2, STICK)));
        assertEquals(1, r.size());
        assertEquals(1, r.get(0).missingCount());
        assertEquals(STICK, r.get(0).missing().get(0).allowed().iterator().next());
        assertEquals(1, r.get(0).missing().get(0).amount());
    }

    @Test
    void recipeWithNoMatchIsNotListed() {
        Inventory inv = new Inventory().add(IRON, 5);
        assertTrue(run(inv, 5, recipe("pick", slot(3, PLANKS), slot(2, STICK))).isEmpty());
    }

    @Test
    void tooManyMissingIsNotListed() {
        Inventory inv = new Inventory().add(PLANKS, 1);
        assertTrue(run(inv, 2, recipe("pick", slot(3, PLANKS), slot(2, STICK))).isEmpty());
    }

    @Test
    void tagSlotAcceptsEitherItem() {
        RecipeInput r = recipe("chest", slot(1, OAK, BIRCH), slot(1, STICK));
        assertEquals(1, run(new Inventory().add(OAK, 1), 2, r).get(0).matched());
        assertEquals(1, run(new Inventory().add(BIRCH, 1), 2, r).get(0).matched());
    }

    @Test
    void sameItemInTwoSlotsIsSubtractedOnce() {
        Inventory inv = new Inventory().add(PLANKS, 1);
        MatchResult r = run(inv, 2, recipe("r", slot(1, PLANKS), slot(1, PLANKS))).get(0);
        assertEquals(1, r.matched());
        assertEquals(1, r.missingCount());
    }

    @Test
    void amountAboveOneCountsPartialMatch() {
        Inventory inv = new Inventory().add(IRON, 2);
        MatchResult r = run(inv, 3, recipe("r", slot(5, IRON))).get(0);
        assertEquals(2, r.matched());
        assertEquals(3, r.missingCount());
        assertEquals(3, r.missing().get(0).amount());
    }

    @Test
    void missingItemsAreGroupedBySlotKind() {
        Inventory inv = new Inventory().add(STICK, 1);
        MatchResult r = run(inv, 3, recipe("r", slot(1, STICK), slot(1, IRON), slot(1, IRON))).get(0);
        assertEquals(1, r.missing().size());
        assertEquals(2, r.missing().get(0).amount());
    }

    @Test
    void sortsByFewestMissingThenHighestRatio() {
        Inventory inv = new Inventory().add(X, 1).add(Y, 3);
        RecipeInput twoMissing = recipe("a", slot(1, X), slot(2, Z));
        RecipeInput oneOfFour = recipe("b", slot(3, Y), slot(1, Z));
        RecipeInput oneOfTwo = recipe("c", slot(1, X), slot(1, Z));
        List<MatchResult> r = run(inv, 2, twoMissing, oneOfTwo, oneOfFour);
        assertEquals(List.of("b", "c", "a"), r.stream().map(m -> m.recipe().id()).toList());
    }

    @Test
    void respectsMinMatchedAndMaxResults() {
        Inventory inv = new Inventory().add(X, 1);
        List<RecipeInput> recipes = List.of(recipe("a", slot(1, X), slot(1, Z)), recipe("b", slot(1, X), slot(1, Y)));
        assertTrue(Matcher.match(inv, recipes, 2, 5, 10).isEmpty());
        assertEquals(1, Matcher.match(inv, recipes, 1, 5, 1).size());
    }

    /** Known limit of the greedy method. Slot 1 takes Y first, so slot 2 finds nothing. */
    @Test
    void knownGreedyLimitWithOverlappingTags() {
        Inventory inv = new Inventory().add(X, 1).add(Y, 1);
        RecipeInput r = recipe("r", slot(1, Y, X), slot(1, Y, Z));
        List<MatchResult> results = run(inv, 1, r);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).missingCount());
    }
}
