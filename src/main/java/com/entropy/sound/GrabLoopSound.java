package com.entropy.sound;

import com.entropy.GatewayGunMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;

@Environment(EnvType.CLIENT)
public class GrabLoopSound extends MovingSoundInstance {
    private final Entity player;

    public GrabLoopSound(Entity entity) {
        super(GatewayGunMod.GRAB_LOOP_EVENT, SoundCategory.PLAYERS, SoundInstance.createRandom());
        player = entity;
        repeat = true;
        repeatDelay = 0;
    }

    @Override
    public void tick() {
        if (player.isRemoved()) {
            setDone();
            return;
        }
        x = player.getEyePos().x;
        y = player.getEyePos().y;
        z = player.getEyePos().z;
    }
}
