package com.entropy.misc;

import com.entropy.CoreData;
import com.entropy.GatewayGunMod;
import com.entropy.GatewayRecord;
import com.entropy.client.GatewayGunClient;
import com.entropy.items.GatewayCore;
import com.entropy.items.GatewayGun;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;

import javax.annotation.Nullable;
import java.util.UUID;

public class RemoteCallables {
    public static void onClientLeftClickGatewayGun(ServerPlayerEntity player) {
        ItemStack itemInHand = player.getStackInHand(Hand.MAIN_HAND);
        if (itemInHand.getItem() == GatewayGunMod.GATEWAY_GUN) {
            ItemCooldownManager cooldowns = player.getItemCooldownManager();
            float cooldownPercent = cooldowns.getCooldownProgress(GatewayGunMod.GATEWAY_GUN, 0);

            if (cooldownPercent < 0.001) {
                GatewayGunMod.GATEWAY_GUN.onAttack(player, player.getWorld(), Hand.MAIN_HAND);
            } else {
                GatewayGunMod.LOGGER.warn("Received gateway gun interaction packet while on cooldown {}", player);
            }
        } else {
            GatewayGunMod.LOGGER.error("Invalid left click packet {}", player);
        }
    }

    public static void onClientClearGatewayGun(ServerPlayerEntity player) {
        if (player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayGun) {
            GatewayRecord record = GatewayRecord.get();
            CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), false);
            GatewayRecord.GatewayID id1 = new GatewayRecord.GatewayID(data.code, GatewayRecord.GatewaySide.ONE);
            GatewayRecord.GatewayID id2 = new GatewayRecord.GatewayID(data.code, GatewayRecord.GatewaySide.TWO);
            if (data.restrictSide != GatewayRecord.GatewaySide.TWO) {
                record.data.remove(id1);
            }
            if (data.restrictSide != GatewayRecord.GatewaySide.ONE) {
                record.data.remove(id2);
            }
            record.setDirty(true);
        }
    }

    public static void modifyCore(ServerPlayerEntity player) {
        ItemStack gunStack = player.getStackInHand(Hand.MAIN_HAND);
        if (gunStack.getItem() instanceof GatewayGun) {
            ItemStack offStack = player.getStackInHand(Hand.OFF_HAND);
            CoreData data = CoreData.fromTag(gunStack.getOrCreateNbt(), false);
            if (data.hasCore) {
                ItemStack newCore = data.toStack(GatewayGunMod.GATEWAY_CORE);
                if (offStack.isEmpty()) {
                    player.setStackInHand(Hand.OFF_HAND, newCore);
                } else {
                    player.giveItemStack(newCore);
                }
                data.hasCore = false;
                onClientClearGatewayGun(player);
                gunStack.setNbt(data.toTag());
            } else if (offStack.getItem() instanceof GatewayCore) {
                gunStack.setNbt(offStack.getNbt());
                player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
            }
        }
    }

    public static void grabEntity(ServerPlayerEntity player, @Nullable UUID uuid) {
        ItemStack gunStack = player.getStackInHand(Hand.MAIN_HAND);
        if (gunStack.getItem() instanceof GatewayGun) {
            CoreData data = CoreData.fromTag(gunStack.getOrCreateNbt(), false);
            if (data.grabbedEntityId == null && uuid != null && data.hasCore && data.pickup) {
                data.grabbedEntityId = uuid;
                gunStack.setNbt(data.toTag());
                player.getWorld().playSound(null, player.getEyePos().x, player.getEyePos().y, player.getEyePos().z, GatewayGunMod.GRAB_START_EVENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            } else {
                data.grabbedEntityId = null;
                gunStack.setNbt(data.toTag());
                player.getWorld().playSound(null, player.getEyePos().x, player.getEyePos().y, player.getEyePos().z, GatewayGunMod.GRAB_STOP_EVENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    public static void releaseEntity(ServerPlayerEntity player) {
        grabEntity(player, null);
    }

    public static void updateAirResistance(int newResistance) {
        GatewayGunClient.airResistance = newResistance;
    }
}
