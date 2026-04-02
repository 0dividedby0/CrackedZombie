package com.dividedby0.crackedzombies;

import com.dividedby0.crackedzombies.config.ConfigManager;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(CrackedZombiesMod.MODID)
public class CrackedZombiesMod {
    public static final String MODID = "crackedzombies";

    @SuppressWarnings("removal")
    public CrackedZombiesMod() {
        ConfigManager.getInstance();

        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, screen) -> new SimpleConfigScreen(screen, ConfigManager.getInstance())
            )
        );

        CrackedZombieHandler.init();
    }
}
