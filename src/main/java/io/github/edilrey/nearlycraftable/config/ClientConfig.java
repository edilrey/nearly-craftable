package io.github.edilrey.nearlycraftable.config;

import java.util.List;
import java.util.Set;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAX_MISSING;
    public static final ForgeConfigSpec.IntValue MIN_MATCHED;
    public static final ForgeConfigSpec.IntValue MAX_RESULTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CATEGORIES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        MAX_MISSING = builder
                .comment("Maximum number of missing items for a recipe to be listed.")
                .defineInRange("maxMissing", 2, 1, 64);
        MIN_MATCHED = builder
                .comment("Minimum number of items the player must already have.")
                .defineInRange("minMatched", 1, 1, 64);
        MAX_RESULTS = builder
                .comment("Maximum number of recipes in the list.")
                .defineInRange("maxResults", 200, 1, 5000);
        CATEGORIES = builder
                .comment("EMI recipe category ids to search, for example \"emi:crafting\" or \"emi:smelting\".")
                .defineListAllowEmpty("categories", List.of("emi:crafting"), o -> o instanceof String);
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static Set<String> categories() {
        return Set.copyOf(CATEGORIES.get());
    }
}
