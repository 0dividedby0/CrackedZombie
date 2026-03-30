package com.dividedby0.crackedzombies;

import com.dividedby0.crackedzombies.config.ConfigManager;
import com.dividedby0.crackedzombies.entity.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CrackedZombiesMod.MODID)
public class CrackedZombiesMod {
    public static final String MODID = "crackedzombies";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CrackedZombiesMod() {
        var cfg = ConfigManager.getInstance();
        LOGGER.info("[CrackedZombies] Loaded config: daySpawning={}, zombieSpawns={}, minSpawn={}, maxSpawn={}",
                cfg.getBoolean("daySpawning", true),
                cfg.getBoolean("zombieSpawns", false),
                cfg.getInt("minSpawn", 2),
                cfg.getInt("maxSpawn", 10));

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, screen) -> new SimpleConfigScreen(screen, ConfigManager.getInstance())));

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITY_TYPES.register(modBus);
        modBus.addListener(this::onAttributeCreate);
        modBus.addListener(this::onCommonSetup);
    }

    @SubscribeEvent
    public void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CRACKED_ZOMBIE.get(),
                com.dividedby0.crackedzombies.entity.CrackedZombieEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SpawnHandler.registerSpawnPlacements();
            SpawnHandler.init();
        });
    }
}
