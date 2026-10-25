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
            ItemKey best = null;
            int bestCount = 0;
            for (ItemKey key : slot.allowed()) {
                int have = left.getOrDefault(key, 0);
                if (have >= slot.amount()) {
                    full = key;
                    break;
                }
                if (have > bestCount) {
                    best = key;
                    bestCount = have;
                }
            }
            if (full != null) {
                left.merge(full, -slot.amount(), Integer::sum);
            } else {
                if (best != null) {
                    left.merge(best, -bestCount, Integer::sum);
                }
                int lack = slot.amount() - bestCount;
                missing.merge(slot.allowed(), lack, Integer::sum);
                missingTotal += lack;
            }
        }
        List<MatchResult.Missing> grouped = new ArrayList<>();
        missing.forEach((allowed, amount) -> grouped.add(new MatchResult.Missing(allowed, amount)));
        int total = recipe.totalAmount();
        return new MatchResult(recipe, total - missingTotal, total, grouped);
    }
}
