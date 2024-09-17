package com.entropy.client;

import com.entropy.GatewayGunMod;
import com.entropy.client.renderer.GatewayEntityRenderer;
import com.entropy.client.renderer.GatewayShaders;
import com.entropy.client.renderer.WeightedCubeRenderer;
import com.entropy.client.renderer.models.GatewayOverlayModel;
import com.entropy.entity.Gateway;

import com.entropy.entity.WeightedCube;
import com.entropy.sound.GrabLoopSound;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;

import static com.entropy.GatewayGunMod.id;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import qouteall.q_misc_util.api.McRemoteProcedureCall;

@Environment(EnvType.CLIENT)
public class GatewayGunClient implements ClientModInitializer {
    public static final EntityModelLayer OVERLAY_MODEL_LAYER =
            new EntityModelLayer(id("gateway_overlay"), "main");

    public int airResistance = 90;

    @Override
    public void onInitializeClient() {
        KeyBinding clearGateways = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gatewaygun.cleargateways", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.gatewaygun"));
        KeyBinding grabEntity = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gatewaygun.grabentity", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.gatewaygun"));
        KeyBinding modifyCore = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gatewaygun.modifycore", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.gatewaygun"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (clearGateways.wasPressed()) {
                McRemoteProcedureCall.tellServerToInvoke("com.entropy.misc.RemoteCallables.onClientClearGatewayGun");
            }
            while (modifyCore.wasPressed()) {
                McRemoteProcedureCall.tellServerToInvoke("com.entropy.misc.RemoteCallables.modifyCore");
            }
            while (grabEntity.wasPressed()) {
                if(client.targetedEntity != null){
                    MinecraftClient.getInstance().getSoundManager().play(new GrabLoopSound(client.player));
                    McRemoteProcedureCall.tellServerToInvoke("com.entropy.misc.RemoteCallables.grabEntity", client.targetedEntity.getUuid());
                } else {
                    McRemoteProcedureCall.tellServerToInvoke("com.entropy.misc.RemoteCallables.releaseEntity");
                }
            }
        });

        EntityModelLayerRegistry.registerModelLayer(OVERLAY_MODEL_LAYER, GatewayOverlayModel::getTexturedModelData);
        EntityRendererRegistry.register(Gateway.entityType, GatewayEntityRenderer::new);
        EntityRendererRegistry.register(WeightedCube.entityType, WeightedCubeRenderer::new);

        CoreShaderRegistrationCallback.EVENT.register(ctx -> {
            ctx.register(id("gateway"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, shader -> GatewayShaders.gatewayShader = shader);
            ctx.register(id("gatewaycore"), VertexFormats.POSITION_TEXTURE_COLOR_NORMAL, shader -> GatewayShaders.gatewayCoreShader = shader);
        });

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            ItemStack mainHandItem = player.getMainHandStack();

            if (mainHandItem.getItem() != GatewayGunMod.GATEWAY_GUN) {
                return false;
            }

            ItemCooldownManager cooldowns = player.getItemCooldownManager();
            float cooldownPercent = cooldowns.getCooldownProgress(
                    GatewayGunMod.GATEWAY_GUN, 0
            );

            if (cooldownPercent < 0.001) {
                McRemoteProcedureCall.tellServerToInvoke(
                        "com.entropy.misc.RemoteCallables.onClientLeftClickGatewayGun"
                );
            }

            return true;
        });

        BlockRenderLayerMap.INSTANCE.putBlock(GatewayGunMod.GATEGRID, RenderLayer.getCutout());
    }
}
