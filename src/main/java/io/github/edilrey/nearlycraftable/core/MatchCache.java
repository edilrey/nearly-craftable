package io.github.edilrey.nearlycraftable.core;

import java.util.List;
import java.util.Objects;

/** Keeps the last results and recalculates only when the inventory, recipes or settings change. */
public final class MatchCache {
    private List<MatchResult> results = List.of();
    private List<RecipeInput> lastRecipes;
    private int lastHash;
    private int lastSettings;
    private boolean valid;
    private int recalculations;
    private long lastNanos;

    public synchronized List<MatchResult> get(Inventory inventory, List<RecipeInput> recipes,
                                              int minMatched, int maxMissing, int maxResults) {
        int hash = inventory.counts().hashCode();
        int settings = Objects.hash(minMatched, maxMissing, maxResults);
        if (valid && hash == lastHash && settings == lastSettings && recipes == lastRecipes) {
            return results;
        }
        long start = System.nanoTime();
        results = Matcher.match(inventory, recipes, minMatched, maxMissing, maxResults);
        lastNanos = System.nanoTime() - start;
        recalculations++;
        lastHash = hash;
        lastSettings = settings;
        lastRecipes = recipes;
        valid = true;
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
