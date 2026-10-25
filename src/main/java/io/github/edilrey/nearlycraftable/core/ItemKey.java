package io.github.edilrey.nearlycraftable.core;

/** Identity of an item: registry id plus the NBT string, or null when NBT is not relevant. */
public record ItemKey(String id, String nbt) {
    public static ItemKey of(String id) {
        return new ItemKey(id, null);
    }
}
