package com.dividedby0.crackedzombies.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class JSON5ConfigManager {
    private static final String CONFIG_FILENAME = "crackedzombies.json5";
    private final Path configPath;
    private final Map<String, Object> configData;

    public static class ConfigEntry {
        public String key;
        public Object value;
        public Object minValue;
        public Object maxValue;
        public String description;
        public String type;

        public ConfigEntry(String key, Object value, String type, String description) {
            this.key = key;
            this.value = value;
            this.type = type;
            this.description = description;
        }
    }

    // LinkedHashMap preserves insertion order for readable config file output
    private final Map<String, ConfigEntry> configMetadata;

    public JSON5ConfigManager(Path configDir) {
        this.configPath = configDir.resolve(CONFIG_FILENAME);
        this.configData = new LinkedHashMap<>();
        this.configMetadata = new LinkedHashMap<>();
        initializeMetadata();
        loadConfig();
    }

    private void initializeMetadata() {
        // --- Spawn settings ---
        addInt("minSpawn", 2, 1, 64,
                "Minimum number of Cracked Zombies per spawn event");
        addInt("maxSpawn", 10, 1, 128,
                "Maximum number of Cracked Zombies per spawn event");
        addInt("zombieSpawnProb", 15, 1, 100,
                "Relative probability of zombie spawning (higher = more frequent)");
        addBool("zombieSpawns", false,
                "Allow vanilla zombie spawns alongside Cracked Zombies (false = only Cracked Zombies spawn)");
        addBool("daySpawning", true,
                "Allow Cracked Zombies to spawn during daytime");

        // --- Behavior settings ---
        addBool("doorBusting", false,
                "Allow Cracked Zombies to break down doors");
        addBool("sickness", true,
                "Apply Poison effect to players hit by a Cracked Zombie");
        addInt("poisonDuration", 100, 20, 600,
                "Duration of poison effect in ticks (20 ticks = 1 second)");
        addInt("poisonAmplifier", 0, 0, 4,
                "Amplifier of poison effect (0 = Poison I, 1 = Poison II, etc.)");

        // --- Speed & aggro settings ---
        addDouble("moveSpeed", 0.35, 0.1, 1.5,
                "Movement speed of Cracked Zombies (vanilla zombie = 0.23)");
        addDouble("aggroRange", 40.0, 8.0, 128.0,
                "Distance in blocks at which Cracked Zombies detect players");
        addDouble("followRange", 64.0, 16.0, 256.0,
                "Maximum distance in blocks Cracked Zombies will chase a player");

        // --- Other mob spawn toggles (matching original config) ---
        addBool("spawnCreepers", true, "Allow Creeper spawns");
        addBool("spawnEnderman", true, "Allow Enderman spawns");
        addBool("spawnSkeletons", true, "Allow Skeleton spawns");
        addBool("spawnSlime", true, "Allow Slime spawns");
        addBool("spawnSpiders", true, "Allow Spider spawns");
        addBool("spawnWitches", true, "Allow Witch spawns");
    }

    private void addInt(String key, int value, int min, int max, String description) {
        ConfigEntry e = new ConfigEntry(key, value, "int", description);
        e.minValue = min;
        e.maxValue = max;
        configMetadata.put(key, e);
    }

    private void addDouble(String key, double value, double min, double max, String description) {
        ConfigEntry e = new ConfigEntry(key, value, "double", description);
        e.minValue = min;
        e.maxValue = max;
        configMetadata.put(key, e);
    }

    private void addBool(String key, boolean value, String description) {
        configMetadata.put(key, new ConfigEntry(key, value, "boolean", description));
    }

    private void loadConfig() {
        createDefaults();
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                parseJSON5(content);
            } catch (IOException e) {
                System.err.println("[CrackedZombies] Error reading config: " + e.getMessage());
            }
        } else {
            saveConfig();
        }
    }

    private void parseJSON5(String json) {
        json = json.replaceAll("//[^\n]*", "");
        json = json.replaceAll("/\\*.*?\\*/", "");

        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*([^,}\\n]+)");
        var matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String valueStr = matcher.group(2).trim();
            Object value = parseValue(valueStr);
            if (value != null && configMetadata.containsKey(key)) {
                configData.put(key, value);
                configMetadata.get(key).value = value;
            }
        }
    }

    private Object parseValue(String s) {
        s = s.replaceAll("[,\\s]+$", "");
        if (s.equalsIgnoreCase("true")) return true;
        if (s.equalsIgnoreCase("false")) return false;
        if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
        try {
            if (s.contains(".")) return Double.parseDouble(s);
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void createDefaults() {
        for (String key : configMetadata.keySet()) {
            configData.put(key, configMetadata.get(key).value);
        }
    }

    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            StringBuilder json = new StringBuilder("{\n");
            boolean first = true;
            for (String key : configData.keySet()) {
                if (!first) json.append(",\n");
                first = false;
                ConfigEntry entry = configMetadata.get(key);
                json.append("  // ").append(entry.description);
                if (entry.minValue != null) {
                    json.append(" [").append(entry.minValue).append("-").append(entry.maxValue).append("]");
                }
                json.append("\n");
                json.append("  \"").append(key).append("\": ");
                Object value = configData.get(key);
                if (value instanceof String) json.append("\"").append(value).append("\"");
                else json.append(value);
            }
            json.append("\n}\n");
            Files.writeString(configPath, json.toString());
        } catch (IOException e) {
            System.err.println("[CrackedZombies] Error saving config: " + e.getMessage());
        }
    }

    public int getInt(String key, int def) {
        Object v = configData.get(key);
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    public double getDouble(String key, double def) {
        Object v = configData.get(key);
        if (v instanceof Double) return (Double) v;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return def;
    }

    public boolean getBoolean(String key, boolean def) {
        Object v = configData.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return def;
    }

    public void setInt(String key, int value) {
        configData.put(key, value);
        if (configMetadata.containsKey(key)) configMetadata.get(key).value = value;
    }

    public void setDouble(String key, double value) {
        configData.put(key, value);
        if (configMetadata.containsKey(key)) configMetadata.get(key).value = value;
    }

    public void setBoolean(String key, boolean value) {
        configData.put(key, value);
        if (configMetadata.containsKey(key)) configMetadata.get(key).value = value;
    }

    public Map<String, ConfigEntry> getConfigMetadata() {
        return configMetadata;
    }

    public Map<String, Object> getAllConfig() {
        return new LinkedHashMap<>(configData);
    }
}
