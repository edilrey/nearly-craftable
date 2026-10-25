package io.github.edilrey.nearlycraftable.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.LinkedHashSet;
import java.util.List;
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
}
