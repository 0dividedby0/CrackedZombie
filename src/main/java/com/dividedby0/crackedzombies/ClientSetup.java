package com.dividedby0.crackedzombies;

import com.dividedby0.crackedzombies.entity.ModEntities;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CrackedZombiesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Reuse vanilla zombie renderer — Cracked Zombie uses default zombie texture
        event.registerEntityRenderer(ModEntities.CRACKED_ZOMBIE.get(), ZombieRenderer::new);
    }
}
