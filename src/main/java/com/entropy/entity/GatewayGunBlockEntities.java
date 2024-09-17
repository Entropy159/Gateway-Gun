package com.entropy.entity;

import com.entropy.GatewayGunMod;
import com.entropy.blocks.gategrid.GategridBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class GatewayGunBlockEntities {
    public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, GatewayGunMod.id(path), blockEntityType);
    }

    public static final BlockEntityType<GategridBlockEntity> GATEGRID = register("gategrid", BlockEntityType.Builder.create(GategridBlockEntity::new, GatewayGunMod.GATEGRID).build());

    public static void initialize() {
    }
}
