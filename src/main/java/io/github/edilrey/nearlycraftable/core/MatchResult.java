package io.github.edilrey.nearlycraftable.core;

import java.util.List;
import java.util.Set;

public record MatchResult(RecipeInput recipe, int matched, int total, List<Missing> missing) {

    /** A group of missing items. {@code allowed} has more than one key for tag slots. */
    public record Missing(Set<ItemKey> allowed, int amount) {
    }

    public int missingCount() {
        return total - matched;
    }

    public double matchedRatio() {
        return total == 0 ? 0 : (double) matched / total;
    }
}
