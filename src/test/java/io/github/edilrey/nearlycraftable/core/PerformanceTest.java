package io.github.edilrey.nearlycraftable.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Rough guard for requirement N3. The real figure is in the game log. */
class PerformanceTest {
    @Test
    void matchingTwentyThousandRecipesTakesUnderOneHundredMilliseconds() {
        Random random = new Random(42);
        int itemCount = 15_000;
        List<ItemKey> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(ItemKey.of("mod:item" + i));
        }
        List<RecipeInput> recipes = new ArrayList<>();
        for (int r = 0; r < 20_000; r++) {
            List<RecipeInput.Slot> slots = new ArrayList<>();
            int slotCount = 1 + random.nextInt(9);
            for (int s = 0; s < slotCount; s++) {
                LinkedHashSet<ItemKey> allowed = new LinkedHashSet<>();
                int choices = random.nextInt(10) == 0 ? 1 + random.nextInt(30) : 1;
                for (int c = 0; c < choices; c++) {
                    allowed.add(items.get(random.nextInt(itemCount)));
                }
                slots.add(new RecipeInput.Slot(allowed, 1 + random.nextInt(3)));
            }
            recipes.add(new RecipeInput("r" + r, r, items.get(r % itemCount), 1, slots));
        }
        Inventory inv = new Inventory();
        for (int i = 0; i < 36; i++) {
            inv.add(items.get(random.nextInt(itemCount)), 1 + random.nextInt(64));
        }

        Matcher.match(inv, recipes, 1, 2, 200);
        long millis = Long.MAX_VALUE;
        for (int run = 0; run < 5; run++) {
            long start = System.nanoTime();
            Matcher.match(inv, recipes, 1, 2, 200);
            millis = Math.min(millis, (System.nanoTime() - start) / 1_000_000);
        }
        assertTrue(millis < 100, "took " + millis + " ms");
    }
}
