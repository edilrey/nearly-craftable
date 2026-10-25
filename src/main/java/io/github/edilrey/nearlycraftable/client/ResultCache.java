package io.github.edilrey.nearlycraftable.client;

import io.github.edilrey.nearlycraftable.NearlyCraftable;
import io.github.edilrey.nearlycraftable.config.ClientConfig;
import io.github.edilrey.nearlycraftable.core.MatchCache;
import io.github.edilrey.nearlycraftable.core.MatchResult;
import io.github.edilrey.nearlycraftable.core.RecipeInput;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public final class ResultCache {
    private static final MatchCache CACHE = new MatchCache();

    private ResultCache() {
    }

    public static synchronized List<MatchResult> get(Player player) {
        List<RecipeInput> recipes = RecipeSource.get(ClientConfig.categories());
        int before = CACHE.recalculations();
        List<MatchResult> results = CACHE.get(InventorySnapshot.capture(player), recipes,
                ClientConfig.MIN_MATCHED.get(), ClientConfig.MAX_MISSING.get(), ClientConfig.MAX_RESULTS.get());
        if (CACHE.recalculatedLastCall(before)) {
            NearlyCraftable.LOGGER.info("Recalculated {} recipes, {} results in {} ms",
                    recipes.size(), results.size(), CACHE.lastNanos() / 1_000_000.0);
        }
        return results;
    }
}
