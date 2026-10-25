package io.github.edilrey.nearlycraftable.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Matcher {
    private Matcher() {
    }

    public static List<MatchResult> match(Inventory inventory, Iterable<RecipeInput> recipes,
                                          int minMatched, int maxMissing, int maxResults) {
        List<MatchResult> results = new ArrayList<>();
        for (RecipeInput recipe : recipes) {
            MatchResult result = matchOne(inventory, recipe);
            int missing = result.missingCount();
            if (result.matched() >= minMatched && missing >= 1 && missing <= maxMissing) {
                results.add(result);
            }
        }
        results.sort(Comparator.comparingInt(MatchResult::missingCount)
                .thenComparing(Comparator.comparingDouble(MatchResult::matchedRatio).reversed())
                .thenComparing(r -> r.recipe().id()));
        return results.size() > maxResults ? new ArrayList<>(results.subList(0, maxResults)) : results;
    }

    public static MatchResult matchOne(Inventory inventory, RecipeInput recipe) {
        Map<ItemKey, Integer> left = new java.util.HashMap<>(inventory.counts());
        List<RecipeInput.Slot> slots = new ArrayList<>(recipe.slots());
        slots.sort(Comparator.comparingInt(s -> s.allowed().size()));

        Map<Set<ItemKey>, Integer> missing = new LinkedHashMap<>();
        int missingTotal = 0;
        for (RecipeInput.Slot slot : slots) {
            ItemKey full = null;
            for (ItemKey key : slot.allowed()) {
                if (left.getOrDefault(key, 0) >= slot.amount()) {
                    full = key;
                    break;
                }
            }
            if (full != null) {
                left.merge(full, -slot.amount(), Integer::sum);
                continue;
            }
            // No single item is enough. Each grid cell is separate, so different allowed items can be combined.
            int need = slot.amount();
            for (ItemKey key : slot.allowed()) {
                int take = Math.min(need, left.getOrDefault(key, 0));
                if (take > 0) {
                    left.merge(key, -take, Integer::sum);
                    need -= take;
                    if (need == 0) {
                        break;
                    }
                }
            }
            if (need > 0) {
                missing.merge(slot.allowed(), need, Integer::sum);
                missingTotal += need;
            }
        }
        List<MatchResult.Missing> grouped = new ArrayList<>();
        missing.forEach((allowed, amount) -> grouped.add(new MatchResult.Missing(allowed, amount)));
        int total = recipe.totalAmount();
        return new MatchResult(recipe, total - missingTotal, total, grouped);
    }
}
