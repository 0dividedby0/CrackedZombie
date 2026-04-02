package com.dividedby0.crackedzombies.config;

import net.minecraftforge.fml.loading.FMLPaths;

public class ConfigManager {
    private static JSON5ConfigManager instance = null;

    public static JSON5ConfigManager getInstance() {
        if (instance == null) {
            try {
                java.nio.file.Path configPath = FMLPaths.CONFIGDIR.get();
                instance = new JSON5ConfigManager(configPath);
            } catch (Exception e) {
                System.err.println("[CrackedZombies] Failed to initialize config manager: " + e.getMessage());
                try {
                    instance = new JSON5ConfigManager(java.nio.file.Paths.get(System.getProperty("java.io.tmpdir")));
                } catch (Exception ex) {
                    System.err.println("[CrackedZombies] Failed to create fallback config manager: " + ex.getMessage());
                }
            }
        }
        return instance;
    }

    public static void reload() {
        instance = null;
        getInstance();
    }
}
