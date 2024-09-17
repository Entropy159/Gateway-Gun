package com.entropy.client.renderer;

import com.entropy.GatewayGunMod;
import com.entropy.items.GatewayGun;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GatewayGunRenderer extends GeoItemRenderer<GatewayGun> {
    public GatewayGunRenderer() {
        super(new DefaultedItemGeoModel<>(GatewayGunMod.id("gatewaygun")){
            @Override
            public RenderLayer getRenderType(GatewayGun animatable, Identifier texture) {
                return RenderLayer.getEntityTranslucent(texture);
            }
        });
    }
}
