package com.entropy.datagen;

import com.entropy.GatewayGunMod;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;

public class GatewayGunLootTables extends FabricBlockLootTableProvider {
    public GatewayGunLootTables(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate() {
        addDrop(GatewayGunMod.GATEGRID, drops(GatewayGunMod.GATEGRID));
    }
}
