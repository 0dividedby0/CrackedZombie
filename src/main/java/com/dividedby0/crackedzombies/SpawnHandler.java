package com.dividedby0.crackedzombies;

import com.dividedby0.crackedzombies.config.ConfigManager;
import com.dividedby0.crackedzombies.entity.CrackedZombieEntity;
import com.dividedby0.crackedzombies.entity.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public class SpawnHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DAY_SPAWN_INTERVAL_TICKS = 20;
    private static final int MIN_PLAYER_DISTANCE = 8;
    private static final int MAX_PLAYER_DISTANCE = 48;
    private static final int MAX_NEARBY_SURFACE_CRACKED_ZOMBIES = 20;
    private static final ResourceLocation CRACKED_ZOMBIE_ID = ResourceLocation.parse(CrackedZombiesMod.MODID + ":cracked_zombie");
    private static final ResourceLocation ZOMBIE_ID = ResourceLocation.parse("minecraft:zombie");
    private static final ResourceLocation ZOMBIE_VILLAGER_ID = ResourceLocation.parse("minecraft:zombie_villager");
    private static final ResourceLocation CREEPER_ID = ResourceLocation.parse("minecraft:creeper");
    private static final ResourceLocation ENDERMAN_ID = ResourceLocation.parse("minecraft:enderman");
    private static final ResourceLocation SKELETON_ID = ResourceLocation.parse("minecraft:skeleton");
    private static final ResourceLocation STRAY_ID = ResourceLocation.parse("minecraft:stray");
    private static final ResourceLocation WITHER_SKELETON_ID = ResourceLocation.parse("minecraft:wither_skeleton");
    private static final ResourceLocation SLIME_ID = ResourceLocation.parse("minecraft:slime");
    private static final ResourceLocation SPIDER_ID = ResourceLocation.parse("minecraft:spider");
    private static final ResourceLocation CAVE_SPIDER_ID = ResourceLocation.parse("minecraft:cave_spider");
    private static final ResourceLocation WITCH_ID = ResourceLocation.parse("minecraft:witch");

    public static void init() {
        MinecraftForge.EVENT_BUS.register(SpawnHandler.class);
        MinecraftForge.EVENT_BUS.register(CrackedZombieEntity.class);
    }

    /**
     * Register spawn placements so the engine knows valid spawn positions/conditions.
     * Spawn injection into biomes is handled by the JSON biome modifier in
     * data/crackedzombies/forge/biome_modifier/add_cracked_zombie_spawns.json
     */
    public static void registerSpawnPlacements() {
        SpawnPlacements.register(
                ModEntities.CRACKED_ZOMBIE.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CrackedZombieEntity::checkCrackedZombieSpawnRules);
    }

    /**
     * Cancel spawns for vanilla mobs disabled in config.
     * Uses EntityType identity so CrackedZombieEntity (which extends Zombie) is never caught.
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        var config = ConfigManager.getInstance();
        EntityType<?> type = event.getEntity().getType();
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(type);

        if (CRACKED_ZOMBIE_ID.equals(typeId)) {
            // Intentionally quiet in normal gameplay; keep this branch for future debug hooks.
        }

        if (!config.getBoolean("zombieSpawns", false)
                && (ZOMBIE_ID.equals(typeId) || ZOMBIE_VILLAGER_ID.equals(typeId))) {
            event.setCanceled(true);
            replaceVanillaZombie(event);
            return;
        }
        if (!config.getBoolean("spawnCreepers", true) && CREEPER_ID.equals(typeId)) {
            event.setCanceled(true);
            return;
        }
        if (!config.getBoolean("spawnEnderman", true) && ENDERMAN_ID.equals(typeId)) {
            event.setCanceled(true);
            return;
        }
        if (!config.getBoolean("spawnSkeletons", true)
                && (SKELETON_ID.equals(typeId) || STRAY_ID.equals(typeId) || WITHER_SKELETON_ID.equals(typeId))) {
            event.setCanceled(true);
            return;
        }
        if (!config.getBoolean("spawnSlime", true) && SLIME_ID.equals(typeId)) {
            event.setCanceled(true);
            return;
        }
        if (!config.getBoolean("spawnSpiders", true)
                && (SPIDER_ID.equals(typeId) || CAVE_SPIDER_ID.equals(typeId))) {
            event.setCanceled(true);
            return;
        }
        if (!config.getBoolean("spawnWitches", true) && WITCH_ID.equals(typeId)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        var config = ConfigManager.getInstance();
        if (!config.getBoolean("daySpawning", true)
            || !Level.OVERWORLD.equals(level.dimension())
                || level.getDifficulty() == Difficulty.PEACEFUL
                || !level.isDay()
                || level.getGameTime() % DAY_SPAWN_INTERVAL_TICKS != 0
                || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }

        int eligiblePlayers = 0;
        int totalSpawned = 0;
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
                eligiblePlayers++;
                totalSpawned += attemptDaySurfaceSpawn(level, player, config);
            }
        }

        LOGGER.info("[CrackedZombies][DaySpawn] time={} players={} spawned={}",
                level.getGameTime(), eligiblePlayers, totalSpawned);
    }

    private static int attemptDaySurfaceSpawn(ServerLevel level, ServerPlayer player,
                                              com.dividedby0.crackedzombies.config.JSON5ConfigManager config) {
        int nearbySurfaceCount = 0;
        for (CrackedZombieEntity zombie : level.getEntitiesOfClass(
                CrackedZombieEntity.class, player.getBoundingBox().inflate(MAX_PLAYER_DISTANCE))) {
            if (level.canSeeSky(zombie.blockPosition())) {
                nearbySurfaceCount++;
            }
        }
        if (nearbySurfaceCount >= MAX_NEARBY_SURFACE_CRACKED_ZOMBIES) {
            return 0;
        }

        int minSpawn = Math.max(1, config.getInt("minSpawn", 2));
        int maxSpawn = Math.max(minSpawn, config.getInt("maxSpawn", 10));
        int desiredGroup = minSpawn + level.random.nextInt(maxSpawn - minSpawn + 1);
        int maxToSpawn = Math.max(1, Math.min(desiredGroup, MAX_NEARBY_SURFACE_CRACKED_ZOMBIES - nearbySurfaceCount));
        int spawnWeight = Math.max(1, Math.min(100, config.getInt("zombieSpawnProb", 15)));
        int maxAttempts = Math.max(8, Math.min(24, 4 + (spawnWeight / 5)));
        return spawnDaylightGroupNearPlayer(level, player, maxToSpawn, maxAttempts);
    }

    private static int spawnDaylightGroupNearPlayer(ServerLevel level, ServerPlayer player, int maxToSpawn,
                                                     int maxAttempts) {
        int spawned = 0;

        // Try several nearby columns around the player and spawn directly on the surface.
        for (int attempt = 0; attempt < maxAttempts && spawned < maxToSpawn; attempt++) {
            int dx = randomSignedOffset(level, 10, 28);
            int dz = randomSignedOffset(level, 10, 28);
            BlockPos column = player.blockPosition().offset(dx, 0, dz);
            BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).above();

            if (!level.canSeeSky(spawnPos)) {
                continue;
            }
            if (spawnPos.distSqr(player.blockPosition()) < 9.0 * 9.0) {
                continue;
            }
            // Keep this validation simple to avoid false negatives on superflat/custom heights.
            if (!isSimpleSpawnSafe(level, spawnPos)) {
                continue;
            }

            CrackedZombieEntity zombie = ModEntities.CRACKED_ZOMBIE.get().create(level);
            if (zombie == null) {
                continue;
            }

            zombie.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);
            if (level.addFreshEntity(zombie)) {
                spawned++;
            }
        }

        return spawned;
    }

    private static boolean isSimpleSpawnSafe(ServerLevel level, BlockPos spawnPos) {
        if (!level.isEmptyBlock(spawnPos) || !level.isEmptyBlock(spawnPos.above())) {
            return false;
        }
        return !level.getBlockState(spawnPos.below()).isAir();
    }

    private static BlockPos findSurfaceSpawnPos(ServerLevel level, BlockPos playerPos) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int dx = randomSignedOffset(level, MIN_PLAYER_DISTANCE, MAX_PLAYER_DISTANCE);
            int dz = randomSignedOffset(level, MIN_PLAYER_DISTANCE, MAX_PLAYER_DISTANCE);
            BlockPos column = new BlockPos(playerPos.getX() + dx, 0, playerPos.getZ() + dz);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            BlockPos spawnPos = surface.above();

            if (!level.canSeeSky(spawnPos)) {
                continue;
            }
            if (!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, level, spawnPos, ModEntities.CRACKED_ZOMBIE.get())) {
                continue;
            }
            if (!CrackedZombieEntity.checkCrackedZombieSpawnRules(
                    ModEntities.CRACKED_ZOMBIE.get(), level, MobSpawnType.NATURAL, spawnPos, level.random)) {
                continue;
            }

            return spawnPos;
        }

        return null;
    }

    private static int spawnCluster(ServerLevel level, BlockPos center, int groupSize) {
        int spawned = 0;

        for (int index = 0; index < groupSize; index++) {
            BlockPos spawnPos = center.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
            BlockPos adjustedPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnPos).above();

            if (!level.canSeeSky(adjustedPos)
                    || !NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, level, adjustedPos, ModEntities.CRACKED_ZOMBIE.get())
                    || !CrackedZombieEntity.checkCrackedZombieSpawnRules(
                            ModEntities.CRACKED_ZOMBIE.get(), level, MobSpawnType.NATURAL, adjustedPos, level.random)) {
                continue;
            }

            CrackedZombieEntity zombie = ModEntities.CRACKED_ZOMBIE.get().create(level);
            if (zombie == null) {
                continue;
            }

            zombie.moveTo(adjustedPos.getX() + 0.5, adjustedPos.getY(), adjustedPos.getZ() + 0.5,
                    level.random.nextFloat() * 360.0F, 0.0F);
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(adjustedPos), MobSpawnType.NATURAL, null, null);
            if (level.addFreshEntity(zombie)) {
                spawned++;
            }
        }

        return spawned;
    }

    private static void replaceVanillaZombie(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        BlockPos originalPos = event.getEntity().blockPosition();
        BlockPos spawnPos = findReplacementSpawnPos(level, originalPos, event.getSpawnType());
        if (spawnPos == null) {
            return;
        }

        CrackedZombieEntity zombie = ModEntities.CRACKED_ZOMBIE.get().create(level);
        if (zombie == null) {
            return;
        }

        zombie.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                event.getEntity().getYRot(), event.getEntity().getXRot());
        zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), event.getSpawnType(), null, null);
        level.addFreshEntity(zombie);
    }

    private static BlockPos findReplacementSpawnPos(ServerLevel level, BlockPos originalPos, MobSpawnType spawnType) {
        BlockPos directSurfacePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, originalPos).above();
        BlockPos validatedDirectPos = validateReplacementSpawnPos(level, directSurfacePos, spawnType);
        if (validatedDirectPos != null) {
            return validatedDirectPos;
        }

        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos nearbyPos = originalPos.offset(level.random.nextInt(17) - 8, 0, level.random.nextInt(17) - 8);
            BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, nearbyPos).above();
            BlockPos validatedPos = validateReplacementSpawnPos(level, surfacePos, spawnType);
            if (validatedPos != null) {
                return validatedPos;
            }
        }

        return null;
    }

    private static BlockPos validateReplacementSpawnPos(ServerLevel level, BlockPos spawnPos, MobSpawnType spawnType) {
        if (!level.canSeeSky(spawnPos)) {
            return null;
        }
        if (!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, level, spawnPos, ModEntities.CRACKED_ZOMBIE.get())) {
            return null;
        }
        if (!CrackedZombieEntity.checkCrackedZombieSpawnRules(
                ModEntities.CRACKED_ZOMBIE.get(), level, spawnType, spawnPos, level.random)) {
            return null;
        }
        return spawnPos;
    }

    private static int randomSignedOffset(ServerLevel level, int minDistance, int maxDistance) {
        int magnitude = minDistance + level.random.nextInt(maxDistance - minDistance + 1);
        return level.random.nextBoolean() ? magnitude : -magnitude;
    }
}
