package com.entropy;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import static com.entropy.GatewayGunConstants.*;

@Config(name = MODID)
public class GatewayGunConfig implements ConfigData, ModMenuApi {
    @ConfigEntry.Gui.Tooltip
    public boolean staticGatewayRendering = false;
    @ConfigEntry.Gui.Tooltip
    public boolean staticQuantumFieldRendering = false;

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(GatewayGunConfig.class, parent).get();
    }

    public static GatewayGunConfig get() {
        return AutoConfig.getConfigHolder(GatewayGunConfig.class).getConfig();
    }
}
