package com.entropy.datagen;

import com.entropy.GatewayGunMod;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class GatewayGunLootTables extends FabricBlockLootTableProvider {
    public GatewayGunLootTables(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        addDrop(GatewayGunMod.GATEGRID, drops(GatewayGunMod.GATEGRID));
    }
}
