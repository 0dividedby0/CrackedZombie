package com.dividedby0.crackedzombies.entity;

import com.dividedby0.crackedzombies.config.ConfigManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CrackedZombieEntity extends Zombie {

    public CrackedZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean isSunSensitive() {
        var config = com.dividedby0.crackedzombies.config.ConfigManager.getInstance();
        // If day spawning is enabled, suppress sun burning so they survive in daylight
        return !config.getBoolean("daySpawning", true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = ConfigManager.getInstance();
        double moveSpeed = config.getDouble("moveSpeed", 0.35);
        double followRange = config.getDouble("followRange", 64.0);

        return Zombie.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, moveSpeed)
                .add(Attributes.FOLLOW_RANGE, followRange)
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        var config = ConfigManager.getInstance();
        double aggroRange = config.getDouble("aggroRange", 40.0);
        boolean doorBusting = config.getBoolean("doorBusting", false);

        this.goalSelector.addGoal(1, new FloatGoal(this));
        if (doorBusting) {
            this.goalSelector.addGoal(2, new BreakDoorGoal(this, difficulty -> true));
        }
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, (float) aggroRange));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // Apply poison on hit — called from SpawnHandler event listener
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof CrackedZombieEntity)) return;

        var config = ConfigManager.getInstance();
        if (!config.getBoolean("sickness", true)) return;

        int duration = config.getInt("poisonDuration", 100);
        int amplifier = config.getInt("poisonAmplifier", 0);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
    }

    /**
     * Allow day spawning based on config; otherwise use default zombie behaviour.
     */
    public static boolean checkCrackedZombieSpawnRules(
            EntityType<? extends Zombie> type, ServerLevelAccessor level,
            MobSpawnType spawnType, net.minecraft.core.BlockPos pos,
            RandomSource rand) {

        var config = ConfigManager.getInstance();
        boolean daySpawning = config.getBoolean("daySpawning", true);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        boolean isSurfaceSpawn = pos.getY() >= surfaceY - 1 && level.canSeeSky(pos);

        if (daySpawning) {
            return level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                && net.minecraft.world.entity.Mob.checkMobSpawnRules(type, level, spawnType, pos, rand)
                && isSurfaceSpawn;
        }
        // Night-only: standard monster rules (requires darkness)
        return net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type, level, spawnType, pos, rand)
            && isSurfaceSpawn;
    }
}
