package com.entropy.mixin;

import com.entropy.GatewayRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyVariable(method = "travel", at = @At("STORE"), ordinal = 1)
    public float airResistance(float value) {
        return !isOnGround() && value == 0.91F ? 1 - GatewayRecord.get().airResistance / 1000F : value;
    }
}
