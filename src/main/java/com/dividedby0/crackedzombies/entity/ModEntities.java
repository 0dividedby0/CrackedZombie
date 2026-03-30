package com.dividedby0.crackedzombies.entity;

import com.dividedby0.crackedzombies.CrackedZombiesMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CrackedZombiesMod.MODID);

    public static final RegistryObject<EntityType<CrackedZombieEntity>> CRACKED_ZOMBIE =
            ENTITY_TYPES.register("cracked_zombie",
                    () -> EntityType.Builder.<CrackedZombieEntity>of(CrackedZombieEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(8)
                            .build("cracked_zombie"));
}
