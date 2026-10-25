package io.github.edilrey.nearlycraftable.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.Command;
import dev.emi.emi.api.EmiApi;
import io.github.edilrey.nearlycraftable.NearlyCraftable;
import io.github.edilrey.nearlycraftable.config.ClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    public static final KeyMapping OPEN_KEY = new KeyMapping("key.nearlycraftable.open",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N,
            "key.categories.nearlycraftable");

    private ClientSetup() {
    }

    @Mod.EventBusSubscriber(modid = NearlyCraftable.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = NearlyCraftable.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_KEY.consumeClick()) {
                if (mc.screen == null && mc.player != null) {
                    NearlyCraftableScreen.open();
                }
            }
        }

        @SubscribeEvent
        public static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
            Screen screen = event.getScreen();
            if (!(screen instanceof AbstractContainerScreen<?>) || !OPEN_KEY.matches(event.getKeyCode(), event.getScanCode())) {
                return;
            }
            if (screen.getFocused() instanceof EditBox || EmiApi.isSearchFocused()) {
                return;
            }
            NearlyCraftableScreen.open();
            event.setCanceled(true);
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
}
