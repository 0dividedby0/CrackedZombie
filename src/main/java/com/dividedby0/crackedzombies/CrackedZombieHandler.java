package com.dividedby0.crackedzombies;

import com.dividedby0.crackedzombies.config.ConfigManager;
import com.dividedby0.crackedzombies.config.JSON5ConfigManager;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CrackedZombieHandler {
    private static final String CRACKED_TAG = "CrackedZombies_Modified";
    private static final String LAST_PATH_X_TAG = "CrackedZombies_LastPathX";
    private static final String LAST_PATH_Y_TAG = "CrackedZombies_LastPathY";
    private static final String LAST_PATH_Z_TAG = "CrackedZombies_LastPathZ";
    private static final int TARGET_SCAN_INTERVAL = 15;
    private static final int NAVIGATION_REFRESH_INTERVAL = 20;
    private static final double NAVIGATION_SPEED = 1.15D;
    private static final double PATH_REFRESH_DISTANCE_SQR = 4.0D;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(CrackedZombieHandler.class);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        Entity entity = event.getEntity();
        JSON5ConfigManager config = ConfigManager.getInstance();
        if (config == null) {
            return;
        }

        if (!(entity instanceof Zombie zombie)) {
            return;
        }

        boolean allowVanilla = config.getBoolean("zombieSpawns", false);
        int spawnChance = clamp(config.getInt("zombieSpawnProb", 15), 1, 100);
        boolean shouldConvert = !allowVanilla || zombie.getRandom().nextInt(100) < spawnChance;
        if (!shouldConvert) {
            return;
        }

        applyCrackedZombieStats(zombie, config);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || zombie.level().isClientSide()) {
            return;
        }

        if (!isCrackedZombie(zombie)) {
            return;
        }

        JSON5ConfigManager config = ConfigManager.getInstance();
        if (config == null) {
            return;
        }

        if (config.getBoolean("daySpawning", true) && zombie.isOnFire()) {
            zombie.clearFire();
        }

        double aggroRange = config.getDouble("aggroRange", 40.0D);
        double hiddenAggroRange = Math.min(config.getDouble("hiddenAggroRange", 10.0D), aggroRange);
        double followRange = config.getDouble("followRange", 64.0D);

        LivingEntity currentTarget = zombie.getTarget();
        if (currentTarget != null) {
            if (!currentTarget.isAlive() || zombie.distanceToSqr(currentTarget) > followRange * followRange) {
                clearTarget(zombie);
            }
        }

        if (zombie.getTarget() == null && shouldRunInterval(zombie, TARGET_SCAN_INTERVAL)) {
            ServerLevel level = (ServerLevel) zombie.level();
            Player nearestPlayer = findNearestAggroTarget(level, zombie, aggroRange, hiddenAggroRange);
            if (nearestPlayer != null) {
                zombie.setTarget(nearestPlayer);
            }
        }

        LivingEntity target = zombie.getTarget();
        int navigationRefreshInterval = getNavigationRefreshInterval(config, zombie, target);
        if (target != null && shouldRunInterval(zombie, navigationRefreshInterval) && shouldRefreshNavigation(zombie, target)) {
            zombie.getNavigation().moveTo(target, NAVIGATION_SPEED);
            rememberPathTarget(zombie, target);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        if (!Objects.equals(level.dimension(), Level.OVERWORLD)) {
            return;
        }

        JSON5ConfigManager config = ConfigManager.getInstance();
        if (config == null) {
            return;
        }

        int spawnIntervalTicks = clamp(config.getInt("spawnIntervalTicks", 120), 20, 1200);
        if (level.getGameTime() % spawnIntervalTicks != 0) {
            return;
        }

        boolean daySpawning = config.getBoolean("daySpawning", true);
        if (!daySpawning && level.isDay()) {
            return;
        }

        int spawnChance = clamp(config.getInt("zombieSpawnProb", 15), 1, 100);
        int minSpawn = clamp(config.getInt("minSpawn", 2), 1, 64);
        int maxSpawn = clamp(config.getInt("maxSpawn", 10), minSpawn, 128);
        int minSpawnDistance = clamp(config.getInt("minSpawnDistance", 12), 4, 64);
        int maxSpawnDistance = clamp(config.getInt("maxSpawnDistance", 24), minSpawnDistance + 1, 128);
        int maxNearbyCrackedZombies = clamp(config.getInt("maxNearbyCrackedZombies", 24), 0, 128);
        int nearbyZombieCapRange = clamp(config.getInt("nearbyZombieCapRange", 48), 8, 128);
        boolean spawnInCreative = config.getBoolean("spawnInCreative", false);

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || (player.isCreative() && !spawnInCreative)) {
                continue;
            }

            if (level.random.nextInt(100) >= spawnChance) {
                continue;
            }

            if (maxNearbyCrackedZombies > 0
                && countNearbyCrackedZombies(level, player, nearbyZombieCapRange) >= maxNearbyCrackedZombies) {
                continue;
            }

            int groupSize = minSpawn + level.random.nextInt((maxSpawn - minSpawn) + 1);
            int clusterBursts = 1;
            for (int i = 0; i < clusterBursts; i++) {
                spawnZombieCluster(level, player.blockPosition(), groupSize, config, minSpawnDistance, maxSpawnDistance);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Zombie zombie)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player) || !isCrackedZombie(zombie)) {
            return;
        }

        JSON5ConfigManager config = ConfigManager.getInstance();
        if (config == null || !config.getBoolean("sickness", true)) {
            return;
        }

        int duration = clamp(config.getInt("poisonDuration", 100), 20, 600);
        int amplifier = clamp(config.getInt("poisonAmplifier", 0), 0, 4);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
    }

    private static void spawnZombieCluster(ServerLevel level, BlockPos center, int count, JSON5ConfigManager config, int minDistance, int maxDistance) {
        BlockPos anchor = findClusterAnchor(level, center, minDistance, maxDistance);
        if (anchor == null) {
            return;
        }

        int spawned = 0;
        int attempts = Math.max(8, count * 6);

        for (int i = 0; i < attempts && spawned < count; i++) {
            int radius = 1 + level.random.nextInt(4);
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int x = anchor.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = anchor.getZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos spawnPos = new BlockPos(x, y, z);
            BlockState below = level.getBlockState(spawnPos.below());

            if (!below.isFaceSturdy(level, spawnPos.below(), Direction.UP)) {
                continue;
            }

            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) {
                continue;
            }

            zombie.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);

            if (!level.noCollision(zombie)) {
                continue;
            }

            level.addFreshEntity(zombie);
            applyCrackedZombieStats(zombie, config);
            if (config.getBoolean("daySpawning", true)) {
                zombie.clearFire();
            }
            spawned++;
        }
    }

    private static BlockPos findClusterAnchor(ServerLevel level, BlockPos playerPos, int minDistance, int maxDistance) {
        for (int i = 0; i < 12; i++) {
            int radius = minDistance + level.random.nextInt((maxDistance - minDistance) + 1);
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            int x = playerPos.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = playerPos.getZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                return pos;
            }
        }
        return null;
    }

    private static void applyCrackedZombieStats(Zombie zombie, JSON5ConfigManager config) {
        if (isCrackedZombie(zombie)) {
            return;
        }

        double moveSpeed = config.getDouble("moveSpeed", 0.35D);
        double aggroRange = config.getDouble("aggroRange", 40.0D);
        double followRange = config.getDouble("followRange", 64.0D);
        boolean doorBusting = config.getBoolean("doorBusting", false);

        AttributeInstance speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(moveSpeed);
        }

        AttributeInstance follow = zombie.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) {
            follow.setBaseValue(Math.max(aggroRange, followRange));
        }

        zombie.setCanBreakDoors(doorBusting);
        zombie.getNavigation().setCanFloat(true);
        if (zombie.getNavigation() instanceof GroundPathNavigation groundNav) {
            groundNav.setCanPassDoors(true);
            groundNav.setCanOpenDoors(doorBusting);
        }

        zombie.getPersistentData().putBoolean(CRACKED_TAG, true);
    }

    private static int getNavigationRefreshInterval(JSON5ConfigManager config, Zombie zombie, LivingEntity target) {
        if (target == null) {
            return NAVIGATION_REFRESH_INTERVAL;
        }

        int farNavigationRefreshDistance = clamp(config.getInt("farNavigationRefreshDistance", 32), 8, 128);
        if (zombie.distanceToSqr(target) <= farNavigationRefreshDistance * farNavigationRefreshDistance) {
            return NAVIGATION_REFRESH_INTERVAL;
        }

        return clamp(config.getInt("farNavigationRefreshInterval", 40), NAVIGATION_REFRESH_INTERVAL, 200);
    }

    private static Player findNearestAggroTarget(ServerLevel level, Zombie zombie, double aggroRange, double hiddenAggroRange) {
        double bestDistanceSqr = aggroRange * aggroRange;
        double hiddenAggroRangeSqr = hiddenAggroRange * hiddenAggroRange;
        Player bestTarget = null;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                continue;
            }

            double distanceSqr = zombie.distanceToSqr(player);
            if (distanceSqr > bestDistanceSqr) {
                continue;
            }

            if (distanceSqr <= hiddenAggroRangeSqr || zombie.hasLineOfSight(player)) {
                bestDistanceSqr = distanceSqr;
                bestTarget = player;
            }
        }

        return bestTarget;
    }

    private static int countNearbyCrackedZombies(ServerLevel level, ServerPlayer player, int radius) {
        AABB searchBox = player.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(Zombie.class, searchBox, CrackedZombieHandler::isCrackedZombie).size();
    }

    private static boolean shouldRefreshNavigation(Zombie zombie, LivingEntity target) {
        if (zombie.getNavigation().isDone()) {
            return true;
        }

        var data = zombie.getPersistentData();
        if (!data.contains(LAST_PATH_X_TAG) || !data.contains(LAST_PATH_Y_TAG) || !data.contains(LAST_PATH_Z_TAG)) {
            return true;
        }

        double dx = target.getX() - data.getDouble(LAST_PATH_X_TAG);
        double dy = target.getY() - data.getDouble(LAST_PATH_Y_TAG);
        double dz = target.getZ() - data.getDouble(LAST_PATH_Z_TAG);
        return dx * dx + dy * dy + dz * dz >= PATH_REFRESH_DISTANCE_SQR;
    }

    private static void rememberPathTarget(Zombie zombie, LivingEntity target) {
        var data = zombie.getPersistentData();
        data.putDouble(LAST_PATH_X_TAG, target.getX());
        data.putDouble(LAST_PATH_Y_TAG, target.getY());
        data.putDouble(LAST_PATH_Z_TAG, target.getZ());
    }

    private static void clearTarget(Zombie zombie) {
        zombie.setTarget(null);
        zombie.getNavigation().stop();
    }

    private static boolean shouldRunInterval(Zombie zombie, int interval) {
        return interval <= 1 || Math.floorMod(zombie.tickCount + zombie.getId(), interval) == 0;
    }

    private static boolean isCrackedZombie(Zombie zombie) {
        return zombie.getPersistentData().getBoolean(CRACKED_TAG);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
