package io.github.edilrey.nearlycraftable.client;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/** EMI calls register on each reload, so the converted recipes must be dropped here. */
@EmiEntrypoint
public class NearlyCraftableEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        RecipeSource.clear();
    }
}
