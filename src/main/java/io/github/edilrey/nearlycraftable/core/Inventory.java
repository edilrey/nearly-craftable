package io.github.edilrey.nearlycraftable.core;

import java.util.HashMap;
import java.util.Map;

public final class Inventory {
    private final Map<ItemKey, Integer> counts = new HashMap<>();

    public Inventory add(ItemKey key, int amount) {
        if (amount > 0) {
            counts.merge(key, amount, Integer::sum);
        }
        return this;
    }

    public int count(ItemKey key) {
        return counts.getOrDefault(key, 0);
    }

    public Map<ItemKey, Integer> counts() {
        return counts;
    }

    public Inventory copy() {
        Inventory copy = new Inventory();
        copy.counts.putAll(counts);
        return copy;
    }
}
