package com.entropy.items;

import com.entropy.CoreData;
import com.entropy.GatewayGunMod;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class GatewayGunComponents {
    public static final ComponentType<CoreData> GATEWAY_DATA = Registry.register(Registries.DATA_COMPONENT_TYPE, GatewayGunMod.id("data"), ComponentType.<CoreData>builder().codec(CoreData.CODEC).build());

    public static void init() {
    }
}
