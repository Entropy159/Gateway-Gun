package com.entropy.config;

import com.entropy.GatewayGunMod;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = GatewayGunMod.MODID)
public class GatewayGunConfig implements ConfigData, ModMenuApi {
    @ConfigEntry.Gui.Tooltip
    public boolean staticGatewayRendering = false;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(GatewayGunConfig.class, parent).get();
    }
}
