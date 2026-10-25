package io.github.edilrey.nearlycraftable.client;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github.edilrey.nearlycraftable.NearlyCraftable;
import io.github.edilrey.nearlycraftable.core.ItemKey;
import io.github.edilrey.nearlycraftable.core.RecipeInput;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class RecipeSource {
    private static List<RecipeInput> cache;
    private static Set<String> cachedCategories;

    private RecipeSource() {
    }

    public static synchronized void clear() {
        cache = null;
    }

    public static synchronized List<RecipeInput> get(Set<String> categoryIds) {
        if (cache == null || !categoryIds.equals(cachedCategories)) {
            cache = convertAll(categoryIds);
            cachedCategories = Set.copyOf(categoryIds);
        }
        return cache;
    }

    public static ItemKey keyOf(Item item) {
        return ItemKey.of(BuiltInRegistries.ITEM.getKey(item).toString());
    }

    private static List<RecipeInput> convertAll(Set<String> categoryIds) {
        long start = System.nanoTime();
        List<RecipeInput> converted = new ArrayList<>();
        List<EmiRecipe> recipes = EmiApi.getRecipeManager().getRecipes();
        for (EmiRecipe recipe : recipes) {
            if (!categoryIds.contains(String.valueOf(recipe.getCategory().getId()))) {
                continue;
            }
            RecipeInput input = convert(recipe);
            if (input != null) {
                converted.add(input);
            }
        }
        NearlyCraftable.LOGGER.info("Converted {} of {} EMI recipes in {} ms",
                converted.size(), recipes.size(), (System.nanoTime() - start) / 1_000_000.0);
        return converted;
    }

    private static RecipeInput convert(EmiRecipe recipe) {
        if (recipe.getId() == null) {
            return null;
        }
        ItemKey output = null;
        int outputCount = 0;
        for (EmiStack stack : recipe.getOutputs()) {
            if (!stack.isEmpty() && stack.getKey() instanceof Item item) {
                output = keyOf(item);
                outputCount = (int) stack.getAmount();
                break;
            }
        }
        if (output == null) {
            return null;
        }

        Map<Set<ItemKey>, Integer> merged = new LinkedHashMap<>();
        for (EmiIngredient ingredient : recipe.getInputs()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            Set<ItemKey> allowed = new LinkedHashSet<>();
            for (EmiStack stack : ingredient.getEmiStacks()) {
                if (stack.getKey() instanceof Item item) {
                    allowed.add(keyOf(item));
                }
            }
            if (allowed.isEmpty()) {
                return null;
            }
            merged.merge(allowed, (int) Math.max(1, ingredient.getAmount()), Integer::sum);
        }
        if (merged.isEmpty()) {
            return null;
        }
        List<RecipeInput.Slot> slots = new ArrayList<>();
        merged.forEach((allowed, amount) -> slots.add(new RecipeInput.Slot(allowed, amount)));
        return new RecipeInput(recipe.getId().toString(), recipe, output, outputCount, slots);
    }
}
