package io.github.edilrey.nearlycraftable.client;

import com.mojang.brigadier.Command;
import io.github.edilrey.nearlycraftable.NearlyCraftable;
import io.github.edilrey.nearlycraftable.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NearlyCraftable.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nearlycraftable")
                .then(Commands.literal("recipes").executes(ctx -> {
                    int count = RecipeSource.get(ClientConfig.categories()).size();
                    ctx.getSource().sendSuccess(() -> Component.literal("Nearly Craftable: " + count + " recipes"), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("results").executes(ctx -> {
                    int count = ResultCache.get(Minecraft.getInstance().player).size();
                    ctx.getSource().sendSuccess(() -> Component.literal("Nearly Craftable: " + count + " results"), false);
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
