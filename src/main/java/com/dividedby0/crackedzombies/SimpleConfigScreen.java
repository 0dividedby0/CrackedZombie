package com.dividedby0.crackedzombies;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import com.dividedby0.crackedzombies.config.JSON5ConfigManager;

public class SimpleConfigScreen extends Screen {
    private final Screen previousScreen;
    private final JSON5ConfigManager configManager;

    // Spawn fields
    private EditBox minSpawnInput;
    private EditBox maxSpawnInput;
    private EditBox spawnProbInput;

    // Behavior fields
    private EditBox poisonDurationInput;
    private EditBox poisonAmpInput;
    private EditBox moveSpeedInput;
    private EditBox aggroRangeInput;
    private EditBox followRangeInput;

    // Boolean fields
    private boolean zombieSpawns;
    private boolean daySpawning;
    private boolean doorBusting;
    private boolean sickness;
    private boolean spawnCreepers;
    private boolean spawnEnderman;
    private boolean spawnSkeletons;
    private boolean spawnSlime;
    private boolean spawnSpiders;
    private boolean spawnWitches;

    private Button zombieSpawnsButton;
    private Button daySpawningButton;
    private Button doorBustingButton;
    private Button sicknessButton;
    private Button spawnCreepersButton;
    private Button spawnEndermanButton;
    private Button spawnSkeletonsButton;
    private Button spawnSlimeButton;
    private Button spawnSpidersButton;
    private Button spawnWitchesButton;

    public SimpleConfigScreen(Screen previousScreen, JSON5ConfigManager configManager) {
        super(Component.literal("Cracked Zombies Configuration"));
        this.previousScreen = previousScreen;
        this.configManager = configManager;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int leftLabelX = cx - 235;
        int leftInputX = cx - 70;
        int rightLabelX = cx + 20;
        int rightButtonX = cx + 140;
        int y = 30;
        int rowH = 24;

        minSpawnInput = addInput(leftInputX, y, "Min Spawn"); minSpawnInput.setValue(s("minSpawn", 2));
        zombieSpawns = b("zombieSpawns", false);
        zombieSpawnsButton = addToggleButton(rightButtonX, y, () -> zombieSpawns = !zombieSpawns);
        y += rowH;

        maxSpawnInput = addInput(leftInputX, y, "Max Spawn"); maxSpawnInput.setValue(s("maxSpawn", 10));
        daySpawning = b("daySpawning", true);
        daySpawningButton = addToggleButton(rightButtonX, y, () -> daySpawning = !daySpawning);
        y += rowH;

        spawnProbInput = addInput(leftInputX, y, "Spawn Weight"); spawnProbInput.setValue(s("zombieSpawnProb", 15));
        doorBusting = b("doorBusting", false);
        doorBustingButton = addToggleButton(rightButtonX, y, () -> doorBusting = !doorBusting);
        y += rowH;

        moveSpeedInput = addInput(leftInputX, y, "Move Speed"); moveSpeedInput.setValue(d("moveSpeed", 0.35));
        sickness = b("sickness", true);
        sicknessButton = addToggleButton(rightButtonX, y, () -> sickness = !sickness);
        y += rowH;

        aggroRangeInput = addInput(leftInputX, y, "Aggro Range"); aggroRangeInput.setValue(d("aggroRange", 40.0));
        spawnCreepers = b("spawnCreepers", true);
        spawnCreepersButton = addToggleButton(rightButtonX, y, () -> spawnCreepers = !spawnCreepers);
        y += rowH;

        followRangeInput = addInput(leftInputX, y, "Follow Range"); followRangeInput.setValue(d("followRange", 64.0));
        spawnEnderman = b("spawnEnderman", true);
        spawnEndermanButton = addToggleButton(rightButtonX, y, () -> spawnEnderman = !spawnEnderman);
        y += rowH;

        poisonDurationInput = addInput(leftInputX, y, "Poison Duration (ticks)"); poisonDurationInput.setValue(s("poisonDuration", 100));
        spawnSkeletons = b("spawnSkeletons", true);
        spawnSkeletonsButton = addToggleButton(rightButtonX, y, () -> spawnSkeletons = !spawnSkeletons);
        y += rowH;

        poisonAmpInput = addInput(leftInputX, y, "Poison Amplifier"); poisonAmpInput.setValue(s("poisonAmplifier", 0));
        spawnSlime = b("spawnSlime", true);
        spawnSlimeButton = addToggleButton(rightButtonX, y, () -> spawnSlime = !spawnSlime);
        y += rowH;

        // Extra row for remaining spawn toggles
        y += 4;
        spawnSpiders = b("spawnSpiders", true);
        spawnSpidersButton = addToggleButton(rightButtonX, y, () -> spawnSpiders = !spawnSpiders);
        y += rowH;
        spawnWitches = b("spawnWitches", true);
        spawnWitchesButton = addToggleButton(rightButtonX, y, () -> spawnWitches = !spawnWitches);

        refreshToggleLabels();

        int by = this.height - 30;
        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> save())
                .bounds(cx - 110, by, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Back"), btn -> onClose())
                .bounds(cx + 10, by, 100, 20).build());
    }

    private EditBox addInput(int x, int y, String hint) {
        EditBox box = new EditBox(this.font, x, y, 140, 18, Component.literal(hint));
        this.addRenderableWidget(box);
        return box;
    }

    private Button addToggleButton(int x, int y, Runnable onToggle) {
        return this.addRenderableWidget(Button.builder(Component.literal(""), btn -> {
            onToggle.run();
            refreshToggleLabels();
        }).bounds(x, y, 130, 18).build());
    }

    private String s(String key, int def) { return String.valueOf(configManager.getInt(key, def)); }
    private String d(String key, double def) { return String.valueOf(configManager.getDouble(key, def)); }
    private boolean b(String key, boolean def) { return configManager.getBoolean(key, def); }

    private void refreshToggleLabels() {
        zombieSpawnsButton.setMessage(boolLabel(zombieSpawns));
        daySpawningButton.setMessage(boolLabel(daySpawning));
        doorBustingButton.setMessage(boolLabel(doorBusting));
        sicknessButton.setMessage(boolLabel(sickness));
        spawnCreepersButton.setMessage(boolLabel(spawnCreepers));
        spawnEndermanButton.setMessage(boolLabel(spawnEnderman));
        spawnSkeletonsButton.setMessage(boolLabel(spawnSkeletons));
        spawnSlimeButton.setMessage(boolLabel(spawnSlime));
        spawnSpidersButton.setMessage(boolLabel(spawnSpiders));
        spawnWitchesButton.setMessage(boolLabel(spawnWitches));
    }

    private Component boolLabel(boolean value) {
        return Component.literal(value ? "Enabled" : "Disabled");
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        int leftLabelX = this.width / 2 - 235;
        int rightLabelX = this.width / 2 + 20;
        int y = 34;
        int h = 24;
        String[] leftLabels = {
            "Min Zombies per spawn [1-64]:",
            "Max Zombies per spawn [1-128]:",
            "Spawn weight [1-100]:",
            "Move speed [0.1-1.5] (vanilla=0.23):",
            "Aggro range blocks [8-128]:",
            "Follow range blocks [16-256]:",
            "Poison duration ticks [20-600]:",
            "Poison amplifier [0-4] (0=Poison I):"
        };
        String[] rightLabels = {
            "Allow vanilla zombies:",
            "Allow daytime spawning:",
            "Door busting:",
            "Poison on hit:",
            "Allow creepers:",
            "Allow enderman:",
            "Allow skeletons:",
            "Allow slimes:",
            "Allow spiders:",
            "Allow witches:"
        };

        for (int i = 0; i < leftLabels.length; i++) {
            g.drawString(this.font, leftLabels[i], leftLabelX, y, 0xAAAAAA);
            g.drawString(this.font, rightLabels[i], rightLabelX, y, 0xAAAAAA);
            y += h;
        }

        y += 4;
        g.drawString(this.font, rightLabels[8], rightLabelX, y, 0xAAAAAA);
        y += h;
        g.drawString(this.font, rightLabels[9], rightLabelX, y, 0xAAAAAA);

        super.render(g, mx, my, pt);
    }

    private void save() {
        try { configManager.setInt("minSpawn", clamp(parseInt(minSpawnInput), 1, 64)); } catch (NumberFormatException ignored) {}
        try { configManager.setInt("maxSpawn", clamp(parseInt(maxSpawnInput), 1, 128)); } catch (NumberFormatException ignored) {}
        try { configManager.setInt("zombieSpawnProb", clamp(parseInt(spawnProbInput), 1, 100)); } catch (NumberFormatException ignored) {}
        try { configManager.setDouble("moveSpeed", clampD(parseDouble(moveSpeedInput), 0.1, 1.5)); } catch (NumberFormatException ignored) {}
        try { configManager.setDouble("aggroRange", clampD(parseDouble(aggroRangeInput), 8.0, 128.0)); } catch (NumberFormatException ignored) {}
        try { configManager.setDouble("followRange", clampD(parseDouble(followRangeInput), 16.0, 256.0)); } catch (NumberFormatException ignored) {}
        try { configManager.setInt("poisonDuration", clamp(parseInt(poisonDurationInput), 20, 600)); } catch (NumberFormatException ignored) {}
        try { configManager.setInt("poisonAmplifier", clamp(parseInt(poisonAmpInput), 0, 4)); } catch (NumberFormatException ignored) {}

        configManager.setBoolean("zombieSpawns", zombieSpawns);
        configManager.setBoolean("daySpawning", daySpawning);
        configManager.setBoolean("doorBusting", doorBusting);
        configManager.setBoolean("sickness", sickness);
        configManager.setBoolean("spawnCreepers", spawnCreepers);
        configManager.setBoolean("spawnEnderman", spawnEnderman);
        configManager.setBoolean("spawnSkeletons", spawnSkeletons);
        configManager.setBoolean("spawnSlime", spawnSlime);
        configManager.setBoolean("spawnSpiders", spawnSpiders);
        configManager.setBoolean("spawnWitches", spawnWitches);

        configManager.saveConfig();
        this.onClose();
    }

    private int parseInt(EditBox box) { return Integer.parseInt(box.getValue().trim()); }
    private double parseDouble(EditBox box) { return Double.parseDouble(box.getValue().trim()); }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private double clampD(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

    @Override
    public void onClose() { this.minecraft.setScreen(this.previousScreen); }

    @Override
    public boolean isPauseScreen() { return false; }
}
