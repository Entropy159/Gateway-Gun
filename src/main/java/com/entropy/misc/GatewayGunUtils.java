package com.entropy.misc;

import com.entropy.CoreData;
import com.entropy.GatewayGunMod;
import com.entropy.GatewayRecord;
import com.entropy.entity.Gateway;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.PortalUtils;
import qouteall.imm_ptl.core.portal.animation.NormalAnimation;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.RayTraceResult;
import qouteall.q_misc_util.my_util.Vec2d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GatewayGunUtils {
    public static void placeGateway(ServerWorld world, Vec3d pos, Vec3d up, Vec3d right, CoreData data, GatewayRecord.GatewaySide side, IntBox area, IntBox wall, boolean blockPredicate) {
        GatewayRecord record = GatewayRecord.get();
        GatewayRecord.GatewayID id = new GatewayRecord.GatewayID(data.code, side);
        GatewayRecord.GatewayID otherSideId = id.getTheOtherSide();
        GatewayRecord.GatewayInfo thisSideInfo = record.data.get(id);
        GatewayRecord.GatewayInfo otherSideInfo = record.data.get(otherSideId);
        Gateway gateway;
        boolean existing = false;
        if (thisSideInfo != null && thisSideInfo.id() != null && world.getEntity(thisSideInfo.id()) instanceof Gateway g) {
            gateway = g;
            existing = true;
        } else {
            gateway = Gateway.entityType.create(world);
            if (gateway == null) {
                return;
            }
        }

        gateway.clearAnimationDrivers(true, true);
        gateway.setWidth(data.width);
        gateway.setHeight(data.height);
        NormalAnimation anim = NormalAnimation.createSizeAnimation(gateway, new Vec2d(0.01, 0.01), new Vec2d(GatewayGunMod.sizeMult, GatewayGunMod.sizeMult), gateway.getAnimationEffectiveTime(), 5, TimingFunction.easeInOutCubic);
        gateway.addThisSideAnimationDriver(anim);
        gateway.addOtherSideAnimationDriver(anim);

        gateway.setOriginPos(pos);
        gateway.setOrientationAndSize(right, up, data.width, data.height);
        gateway.id = id;
        gateway.wallBox = wall;
        gateway.airBox = area;
        if (blockPredicate) gateway.setAllowedBlocks(data.allowedBlocks);
        else gateway.setAllowedBlocks(null);
        gateway.thisSideUpdateCounter = thisSideInfo == null ? 0 : thisSideInfo.updateCounter();
        gateway.otherSideUpdateCounter = otherSideInfo == null ? 0 : otherSideInfo.updateCounter();
        PortalManipulation.makePortalRound(gateway, 50);
        gateway.disableDefaultAnimation();
        gateway.customColor = data.getColor(side);
        gateway.setTeleportChangesGravity(data.gravity);

        if (otherSideInfo == null) {
            // it's unpaired, invisible and not teleportable
            gateway.setDestinationDimension(world.getRegistryKey());
            gateway.setDestination(pos.add(0, 10, 0));
            gateway.setIsVisible(false);
            gateway.setTeleportable(false);
        } else {
            // it's linked
            gateway.setDestinationDimension(otherSideInfo.dim());
            gateway.setDestination(otherSideInfo.pos());
            gateway.setOtherSideOrientation(otherSideInfo.orientation());
            gateway.setIsVisible(true);
            gateway.setTeleportable(true);
            world.playSound(null, pos.x, pos.y, pos.z, GatewayGunMod.GATEWAY_OPEN_EVENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }

        gateway.thisSideUpdateCounter += 1;
        thisSideInfo = new GatewayRecord.GatewayInfo(gateway.getUuid(), world.getRegistryKey(), pos, gateway.getOrientationRotation(), gateway.thisSideUpdateCounter);

        record.data.put(id, thisSideInfo);
        record.setDirty(true);

        if (!existing) {
            McHelper.spawnServerEntity(gateway);
        } else {
            gateway.reloadAndSyncToClient();
        }

    }

    public static void broadcast(ServerWorld world, Text message) {
        world.getPlayers().forEach(player -> player.sendMessage(message));
    }

    public record PortalRTResult(List<Portal> portals, Vec3d pos) {
    }

    public static PortalRTResult portalRaytrace(World world, Vec3d start, Vec3d end) {
        return portalRaytrace(world, start, end, new ArrayList<>());
    }

    public static PortalRTResult portalRaytrace(World world, Vec3d start, Vec3d end, ArrayList<Portal> portals) {
        Optional<Pair<Portal, RayTraceResult>> op = PortalUtils.raytracePortals(world, start, end, true, Portal::isTeleportable);
        if (op.isPresent()) {
            portals.add(op.get().getFirst());
            return portalRaytrace(world, op.get().getFirst().transformPoint(op.get().getSecond().hitPos()), op.get().getFirst().transformPoint(end), portals);
        }
        return new PortalRTResult(portals, end);
    }

    public static int hexToInt(String hex) {
        try {
            return Integer.parseUnsignedInt(hex, 16);
        } catch (NumberFormatException exception) {
            GatewayGunMod.LOGGER.error(exception.getMessage());
            return 0;
        }
    }

    public static String intToHex(int num) {
        return Integer.toHexString(num);
    }
}
