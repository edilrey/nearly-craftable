package io.github.edilrey.nearlycraftable.core;

import java.util.List;
import java.util.Set;

/**
 * One recipe as the matcher sees it.
 *
 * @param id     recipe id, used for tie-breaking and lookup
 * @param ref    opaque handle that the caller uses to find the original recipe
 * @param output display key of the recipe output
 * @param outputCount number of output items
 * @param slots  ingredient slots
 */
public record RecipeInput(String id, Object ref, ItemKey output, int outputCount, List<Slot> slots) {

    /** An ingredient slot. {@code allowed} is in preference order. */
    public record Slot(Set<ItemKey> allowed, int amount) {
    }

    public int totalAmount() {
        int total = 0;
        for (Slot slot : slots) {
            total += slot.amount();
        }
        return total;
    }
}
