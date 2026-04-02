package com.dividedby0.crackedzombies;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.dividedby0.crackedzombies.config.JSON5ConfigManager;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class SimpleConfigScreen extends Screen {
    private final Screen previousScreen;
    private final JSON5ConfigManager configManager;

    private int minSpawn;
    private int maxSpawn;
    private int zombieSpawnProb;
    private int minSpawnDistance;
    private int maxSpawnDistance;
    private int poisonDuration;
    private int poisonAmplifier;
    private double moveSpeed;
    private double aggroRange;
    private double followRange;

    // Boolean fields
    private boolean spawnInCreative;
    private boolean zombieSpawns;
    private boolean daySpawning;
    private boolean doorBusting;
    private boolean sickness;


    public SimpleConfigScreen(Screen previousScreen, JSON5ConfigManager configManager) {
        super(Component.literal("Cracked Zombies Configuration"));
        this.previousScreen = previousScreen;
        this.configManager = configManager;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        minSpawn = i("minSpawn", 2);
        maxSpawn = i("maxSpawn", 10);
        zombieSpawnProb = i("zombieSpawnProb", 15);
        minSpawnDistance = i("minSpawnDistance", 12);
        maxSpawnDistance = i("maxSpawnDistance", 24);
        moveSpeed = d("moveSpeed", 0.35);
        aggroRange = d("aggroRange", 40.0);
        followRange = d("followRange", 64.0);
        poisonDuration = i("poisonDuration", 100);
        poisonAmplifier = i("poisonAmplifier", 0);

        spawnInCreative = b("spawnInCreative", false);
        zombieSpawns = b("zombieSpawns", false);
        daySpawning = b("daySpawning", true);
        doorBusting = b("doorBusting", false);
        sickness = b("sickness", true);


        int cx = this.width / 2;
        int leftX = cx - 155;
        int rightX = cx + 5;
        int y = 30;
        int rowH = 24;

        addIntSlider(leftX, y, "Min Zombies", 1, 64, minSpawn, v -> minSpawn = v);
        addToggleButton(rightX, y, "Vanilla Zombies", () -> zombieSpawns, () -> zombieSpawns = !zombieSpawns);
        y += rowH;

        addIntSlider(leftX, y, "Max Zombies", 1, 128, maxSpawn, v -> maxSpawn = v);
        addToggleButton(rightX, y, "Day Spawning", () -> daySpawning, () -> daySpawning = !daySpawning);
        y += rowH;

        addIntSlider(leftX, y, "Spawn Weight", 1, 100, zombieSpawnProb, v -> zombieSpawnProb = v);
        addToggleButton(rightX, y, "Door Busting", () -> doorBusting, () -> doorBusting = !doorBusting);
        y += rowH;

        addIntSlider(leftX, y, "Min Spawn Distance", 4, 64, minSpawnDistance, v -> minSpawnDistance = v);
        addToggleButton(rightX, y, "Spawn In Creative", () -> spawnInCreative, () -> spawnInCreative = !spawnInCreative);
        y += rowH;

        addIntSlider(leftX, y, "Max Spawn Distance", 8, 128, maxSpawnDistance, v -> maxSpawnDistance = v);
        addToggleButton(rightX, y, "Poison On Hit", () -> sickness, () -> sickness = !sickness);
        y += rowH;

        addDoubleSlider(leftX, y, "Move Speed", 0.1, 1.5, moveSpeed, 2, v -> moveSpeed = v);
        y += rowH;

        addDoubleSlider(leftX, y, "Aggro Range", 8.0, 128.0, aggroRange, 1, v -> aggroRange = v);
        y += rowH;

        addDoubleSlider(leftX, y, "Follow Range", 16.0, 256.0, followRange, 1, v -> followRange = v);
        y += rowH;

        addIntSlider(leftX, y, "Poison Duration", 20, 600, poisonDuration, v -> poisonDuration = v);
        y += rowH;

        addIntSlider(leftX, y, "Poison Amplifier", 0, 4, poisonAmplifier, v -> poisonAmplifier = v);

        int by = this.height - 30;
        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> save())
                .bounds(cx - 110, by, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Back"), btn -> onClose())
                .bounds(cx + 10, by, 100, 20).build());
    }

    private IntSlider addIntSlider(int x, int y, String title, int min, int max, int value, IntConsumer onChange) {
        return this.addRenderableWidget(new IntSlider(x, y, 150, 20, title, min, max, value, onChange));
    }

    private DoubleSlider addDoubleSlider(int x, int y, String title, double min, double max, double value,
                                         int decimals, DoubleConsumer onChange) {
        return this.addRenderableWidget(new DoubleSlider(x, y, 150, 20, title, min, max, value, decimals, onChange));
    }

    private Button addToggleButton(int x, int y, String title, BooleanSupplier state, Runnable onToggle) {
        return this.addRenderableWidget(Button.builder(toggleLabel(title, state.getAsBoolean()), btn -> {
            onToggle.run();
            btn.setMessage(toggleLabel(title, state.getAsBoolean()));
        }).bounds(x, y, 150, 20).build());
    }

    private int i(String key, int def) { return configManager.getInt(key, def); }
    private double d(String key, double def) { return configManager.getDouble(key, def); }
    private boolean b(String key, boolean def) { return configManager.getBoolean(key, def); }

    private Component toggleLabel(String title, boolean value) {
        return Component.literal(title + ": " + (value ? "ON" : "OFF"));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        super.render(g, mx, my, pt);
    }

    private void save() {
        configManager.setInt("minSpawn", clamp(minSpawn, 1, 64));
        configManager.setInt("maxSpawn", clamp(maxSpawn, 1, 128));
        configManager.setInt("zombieSpawnProb", clamp(zombieSpawnProb, 1, 100));
        int clampedMinDistance = clamp(minSpawnDistance, 4, 64);
        int clampedMaxDistance = clamp(maxSpawnDistance, 8, 128);
        if (clampedMaxDistance <= clampedMinDistance) {
            clampedMaxDistance = Math.min(128, clampedMinDistance + 1);
            if (clampedMaxDistance <= clampedMinDistance) {
                clampedMinDistance = Math.max(4, clampedMaxDistance - 1);
            }
        }
        configManager.setInt("minSpawnDistance", clampedMinDistance);
        configManager.setInt("maxSpawnDistance", clampedMaxDistance);
        configManager.setDouble("moveSpeed", clampD(moveSpeed, 0.1, 1.5));
        configManager.setDouble("aggroRange", clampD(aggroRange, 8.0, 128.0));
        configManager.setDouble("followRange", clampD(followRange, 16.0, 256.0));
        configManager.setInt("poisonDuration", clamp(poisonDuration, 20, 600));
        configManager.setInt("poisonAmplifier", clamp(poisonAmplifier, 0, 4));

        configManager.setBoolean("spawnInCreative", spawnInCreative);
        configManager.setBoolean("zombieSpawns", zombieSpawns);
        configManager.setBoolean("daySpawning", daySpawning);
        configManager.setBoolean("doorBusting", doorBusting);
        configManager.setBoolean("sickness", sickness);


        configManager.saveConfig();
        this.onClose();
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private double clampD(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

    @Override
    public void onClose() { this.minecraft.setScreen(this.previousScreen); }

    @Override
    public boolean isPauseScreen() { return false; }

    private static class IntSlider extends AbstractSliderButton {
        private final String title;
        private final int min;
        private final int max;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int width, int height, String title, int min, int max, int value, IntConsumer onChange) {
            super(x, y, width, height, Component.empty(), toSlider(value, min, max));
            this.title = title;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(this.title + ": " + this.toValue()));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(this.toValue());
        }

        private int toValue() {
            return Mth.clamp((int) Math.round(this.min + (this.max - this.min) * this.value), this.min, this.max);
        }

        private static double toSlider(int value, int min, int max) {
            if (max <= min) {
                return 0.0;
            }
            return Mth.clamp((value - (double) min) / (double) (max - min), 0.0, 1.0);
        }
    }

    private static class DoubleSlider extends AbstractSliderButton {
        private final String title;
        private final double min;
        private final double max;
        private final int decimals;
        private final DoubleConsumer onChange;

        DoubleSlider(int x, int y, int width, int height, String title, double min, double max,
                     double value, int decimals, DoubleConsumer onChange) {
            super(x, y, width, height, Component.empty(), toSlider(value, min, max));
            this.title = title;
            this.min = min;
            this.max = max;
            this.decimals = decimals;
            this.onChange = onChange;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            String format = "%1$." + this.decimals + "f";
            this.setMessage(Component.literal(this.title + ": " + String.format(Locale.ROOT, format, this.toValue())));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(this.toValue());
        }

        private double toValue() {
            return Mth.clamp(this.min + (this.max - this.min) * this.value, this.min, this.max);
        }

        private static double toSlider(double value, double min, double max) {
            if (max <= min) {
                return 0.0;
            }
            return Mth.clamp((value - min) / (max - min), 0.0, 1.0);
        }
    }
}
