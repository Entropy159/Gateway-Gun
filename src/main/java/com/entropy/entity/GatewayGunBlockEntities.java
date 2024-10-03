package com.entropy.entity;

import com.entropy.GatewayGunMod;
import com.entropy.blocks.cubedispenser.CubeDispenserBlockEntity;
import com.entropy.blocks.gategrid.GategridBlockEntity;
import com.entropy.blocks.quantumfield.QuantumFieldBlockEntity;
import com.entropy.client.renderer.CubeDispenserRenderer;
import com.entropy.client.renderer.QuantumFieldRenderer;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class GatewayGunBlockEntities {
    public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, GatewayGunMod.id(path), blockEntityType);
    }

    public static final BlockEntityType<GategridBlockEntity> GATEGRID = register("gategrid", BlockEntityType.Builder.create(GategridBlockEntity::new, GatewayGunMod.GATEGRID).build());
    public static final BlockEntityType<CubeDispenserBlockEntity> CUBE_DISPENSER = register("cube_dispenser", BlockEntityType.Builder.create(CubeDispenserBlockEntity::new, GatewayGunMod.CUBE_DISPENSER).build());
    public static final BlockEntityType<QuantumFieldBlockEntity> QUANTUM_FIELD = register("quantum_field", BlockEntityType.Builder.create(QuantumFieldBlockEntity::new, GatewayGunMod.QUANTUM_FIELD).build());

    public static void initialize() {
    }

    public static void initializeClient() {
        BlockEntityRendererFactories.register(GatewayGunBlockEntities.CUBE_DISPENSER, CubeDispenserRenderer::new);
        BlockEntityRendererFactories.register(GatewayGunBlockEntities.QUANTUM_FIELD, QuantumFieldRenderer::new);
    }
}
