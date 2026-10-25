package io.github.edilrey.nearlycraftable.client;

import io.github.edilrey.nearlycraftable.NearlyCraftable;
import io.github.edilrey.nearlycraftable.config.ClientConfig;
import io.github.edilrey.nearlycraftable.core.Inventory;
import io.github.edilrey.nearlycraftable.core.MatchResult;
import io.github.edilrey.nearlycraftable.core.Matcher;
import io.github.edilrey.nearlycraftable.core.RecipeInput;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.world.entity.player.Player;

public final class ResultCache {
    private static List<MatchResult> results = List.of();
    private static List<RecipeInput> lastRecipes;
    private static int lastHash;
    private static int lastSettings;
    private static boolean valid;

    private ResultCache() {
    }

    public static List<MatchResult> get(Player player) {
        return get(player, ClientConfig.categories(), ClientConfig.MIN_MATCHED.get(),
                ClientConfig.MAX_MISSING.get(), ClientConfig.MAX_RESULTS.get());
    }

    static synchronized List<MatchResult> get(Player player, Set<String> categories,
                                                     int minMatched, int maxMissing, int maxResults) {
        Inventory inventory = InventorySnapshot.capture(player);
        int hash = InventorySnapshot.hash(inventory);
        int settings = Objects.hash(categories, minMatched, maxMissing, maxResults);
        List<RecipeInput> recipes = RecipeSource.get(categories);
        if (valid && hash == lastHash && settings == lastSettings && recipes == lastRecipes) {
            return results;
        }
        long start = System.nanoTime();
        results = Matcher.match(inventory, recipes, minMatched, maxMissing, maxResults);
        NearlyCraftable.LOGGER.info("Recalculated {} recipes, {} results in {} ms",
                recipes.size(), results.size(), (System.nanoTime() - start) / 1_000_000.0);
        lastHash = hash;
        lastSettings = settings;
        lastRecipes = recipes;
        valid = true;
        return results;
    }
}
