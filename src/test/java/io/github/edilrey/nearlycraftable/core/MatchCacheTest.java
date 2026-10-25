package io.github.edilrey.nearlycraftable.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class MatchCacheTest {
    private static final ItemKey A = ItemKey.of("a");
    private static final ItemKey B = ItemKey.of("b");
    private static final List<RecipeInput> RECIPES = List.of(new RecipeInput("r", "r", B, 1,
            List.of(new RecipeInput.Slot(new LinkedHashSet<>(List.of(A)), 1),
                    new RecipeInput.Slot(new LinkedHashSet<>(List.of(B)), 1))));

    @Test
    void recalculatesOnlyWhenInventoryChanges() {
        MatchCache cache = new MatchCache();
        Inventory inv = new Inventory().add(A, 1);
        List<MatchResult> first = cache.get(inv, RECIPES, 1, 2, 10);
        assertEquals(1, cache.recalculations());
        assertSame(first, cache.get(new Inventory().add(A, 1), RECIPES, 1, 2, 10));
        assertEquals(1, cache.recalculations());

        List<MatchResult> second = cache.get(new Inventory().add(A, 1).add(B, 1), RECIPES, 1, 2, 10);
        assertEquals(2, cache.recalculations());
        assertEquals(0, second.size());
    }

    @Test
    void recalculatesWhenSettingsOrRecipesChange() {
        MatchCache cache = new MatchCache();
        Inventory inv = new Inventory().add(A, 1);
        cache.get(inv, RECIPES, 1, 2, 10);
        cache.get(inv, RECIPES, 1, 3, 10);
        assertEquals(2, cache.recalculations());
        cache.get(inv, new java.util.ArrayList<>(RECIPES), 1, 3, 10);
        assertEquals(3, cache.recalculations());
    }

    /** These two inventories have the same HashMap hash code, so a hash-only key returns stale results. */
    @Test
    void recalculatesWhenInventoryHashCollides() {
        ItemKey planks = ItemKey.of("minecraft:oak_planks");
        ItemKey coal = ItemKey.of("minecraft:coal");
        Inventory before = new Inventory().add(planks, 1).add(coal, 2);
        Inventory after = new Inventory().add(planks, 2).add(coal, 1);
        assertEquals(before.counts().hashCode(), after.counts().hashCode());
        List<RecipeInput> recipes = List.of(new RecipeInput("r", "r", A, 1,
                List.of(new RecipeInput.Slot(new LinkedHashSet<>(List.of(planks)), 2),
                        new RecipeInput.Slot(new LinkedHashSet<>(List.of(coal)), 1))));
        MatchCache cache = new MatchCache();
        assertEquals(1, cache.get(before, recipes, 1, 2, 10).size());
        assertEquals(0, cache.get(after, recipes, 1, 2, 10).size());
    }

    @Test
    void recalculatesWhenSettingsHashCollides() {
        assertEquals(Objects.hash(1, 2, 200), Objects.hash(1, 3, 169));
        MatchCache cache = new MatchCache();
        Inventory inv = new Inventory().add(A, 1);
        cache.get(inv, RECIPES, 1, 2, 200);
        cache.get(inv, RECIPES, 1, 3, 169);
        assertEquals(2, cache.recalculations());
    }

    @Test
    void cachedResultsAreReadOnly() {
        MatchCache cache = new MatchCache();
        List<MatchResult> results = cache.get(new Inventory().add(A, 1), RECIPES, 1, 2, 10);
        assertThrows(UnsupportedOperationException.class, results::clear);
    }
}
