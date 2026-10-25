package io.github.edilrey.nearlycraftable.core;

import java.util.List;
import java.util.Map;

/** Keeps the last results and recalculates only when the inventory, recipes or settings change. */
public final class MatchCache {
    private List<MatchResult> results = List.of();
    private List<RecipeInput> lastRecipes;
    private Map<ItemKey, Integer> lastCounts;
    private int lastMinMatched;
    private int lastMaxMissing;
    private int lastMaxResults;
    private int recalculations;
    private long lastNanos;

    public synchronized List<MatchResult> get(Inventory inventory, List<RecipeInput> recipes,
                                              int minMatched, int maxMissing, int maxResults) {
        if (lastCounts != null && recipes == lastRecipes && minMatched == lastMinMatched
                && maxMissing == lastMaxMissing && maxResults == lastMaxResults
                && inventory.counts().equals(lastCounts)) {
            return results;
        }
        long start = System.nanoTime();
        results = List.copyOf(Matcher.match(inventory, recipes, minMatched, maxMissing, maxResults));
        lastNanos = System.nanoTime() - start;
        recalculations++;
        lastCounts = Map.copyOf(inventory.counts());
        lastRecipes = recipes;
        lastMinMatched = minMatched;
        lastMaxMissing = maxMissing;
        lastMaxResults = maxResults;
        return results;
    }

    public synchronized boolean recalculatedLastCall(int before) {
        return recalculations != before;
    }

    public synchronized int recalculations() {
        return recalculations;
    }

    public synchronized long lastNanos() {
        return lastNanos;
    }
}
