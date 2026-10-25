package io.github.edilrey.nearlycraftable;

import com.mojang.logging.LogUtils;
import io.github.edilrey.nearlycraftable.config.ClientConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(NearlyCraftable.MOD_ID)
public class NearlyCraftable {
    public static final String MOD_ID = "nearlycraftable";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NearlyCraftable() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}
