package io.github.edilrey.nearlycraftable.client;

import io.github.edilrey.nearlycraftable.core.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventorySnapshot {
    private InventorySnapshot() {
    }

    /** Counts main inventory, hotbar and offhand. */
    public static Inventory capture(Player player) {
        Inventory inventory = new Inventory();
        for (ItemStack stack : player.getInventory().items) {
            add(inventory, stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            add(inventory, stack);
        }
        return inventory;
    }

    public static int hash(Inventory inventory) {
        return inventory.counts().hashCode();
    }

    private static void add(Inventory inventory, ItemStack stack) {
        if (!stack.isEmpty()) {
            inventory.add(RecipeSource.keyOf(stack.getItem()), stack.getCount());
        }
    }
}
