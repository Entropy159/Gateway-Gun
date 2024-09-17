package com.entropy.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.entropy.GatewayGunMod;
import com.entropy.GatewayRecord;
import com.entropy.misc.BlockList;

import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.function.BiPredicate;

public class Gateway extends Portal {
    private static final Logger LOGGER = LogManager.getLogger();

    public static EntityType<Gateway> entityType = FabricEntityTypeBuilder.create(SpawnGroup.MISC, Gateway::new)
            .dimensions(EntityDimensions.changing(0F, 0F))
            .build();

    public GatewayRecord.GatewayID id;

    public IntBox wallBox;
    public IntBox airBox;

    private BlockList allowedBlocks;

    public int thisSideUpdateCounter = 0;
    public int otherSideUpdateCounter = 0;

    public @Nullable String customColor;

    private BiPredicate<World, BlockPos> wallPredicate;

    public Gateway(@NotNull EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound compoundTag) {
        super.readCustomDataFromNbt(compoundTag);
        id = GatewayRecord.GatewayID.fromTag(compoundTag.getCompound("descriptor"));
        wallBox = IntBox.fromTag(compoundTag.getCompound("wallBox"));
        airBox = IntBox.fromTag(compoundTag.getCompound("airBox"));
        setAllowedBlocks(BlockList.fromTag(compoundTag.getList("allowedBlocks", NbtElement.STRING_TYPE)));
        thisSideUpdateCounter = compoundTag.getInt("thisSideUpdateCounter");
        otherSideUpdateCounter = compoundTag.getInt("otherSideUpdateCounter");
        customColor = compoundTag.getString("customColor");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound compoundTag) {
        super.writeCustomDataToNbt(compoundTag);
        compoundTag.put("descriptor", id.toTag());
        compoundTag.put("wallBox", wallBox.toTag());
        compoundTag.put("airBox", airBox.toTag());
        compoundTag.put("allowedBlocks", allowedBlocks.toTag());
        compoundTag.putInt("thisSideUpdateCounter", thisSideUpdateCounter);
        compoundTag.putInt("otherSideUpdateCounter", otherSideUpdateCounter);
        if (customColor != null) {
            compoundTag.putString("customColor", customColor);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!getWorld().isClient) {
            updateState();
        }
    }

    // disable the interpolation between last tick pos and this tick pos
    // because the portal should change abruptly
    @Override
    public void setPosition(double x, double y, double z) {
        super.setPosition(x, y, z);
        resetPosition();
    }

    public void setAllowedBlocks(@Nullable BlockList allowedBlocks) {
        this.allowedBlocks = allowedBlocks == null ? BlockList.createDefault() : allowedBlocks;
        this.wallPredicate = allowedBlocks == null ? null : allowedBlocks.getWallPredicate();
    }

    void updateState() {
        if (id == null || wallBox == null) {
            LOGGER.error("Portal info abnormal {}", this);
            kill();
            return;
        }

        GatewayRecord record = GatewayRecord.get();
        GatewayRecord.GatewayInfo thisSideInfo = record.data.get(id);
        GatewayRecord.GatewayInfo otherSideInfo = record.data.get(id.getTheOtherSide());
        if (thisSideInfo == null) {
            // info is missing
            GatewayGunMod.LOGGER.info("Info missing!");
            playClosingSound();
            kill();
            return;
        }
        if (thisSideUpdateCounter != thisSideInfo.updateCounter() || !thisSideInfo.id().equals(getUuid())) {
            // replaced by new portal
            GatewayGunMod.LOGGER.info("Replaced by new portal!");
            kill();
            return;
        }
        // check block status
        if (!checkBlockStatus()) {
            GatewayGunMod.LOGGER.info("Block check failed!");
            kill();
            record.data.remove(id);
            record.setDirty(true);
            playClosingSound();
            return;
        }
        if (otherSideInfo == null) {
            // other side is missing, make this side inactive
            if (otherSideUpdateCounter != -1) {
                otherSideUpdateCounter = -1;
                teleportable = false;
                setIsVisible(false);
                setDestination(getOriginPos().add(0, 10, 0));
                reloadAndSyncToClient();
            }
            return;
        }
        if (otherSideInfo.updateCounter() != otherSideUpdateCounter) {
            // other side is replaced by new portal, update linking
            if (!isVisible()) {
                getWorld().playSound(
                        null,
                        getX(), getY(), getZ(),
                        GatewayGunMod.GATEWAY_OPEN_EVENT,
                        SoundCategory.PLAYERS,
                        1.0F, 1.0F
                );
            }
            otherSideUpdateCounter = otherSideInfo.updateCounter();
            teleportable = true;
            setIsVisible(true);
            setDestination(otherSideInfo.pos());
            setDestinationDimension(otherSideInfo.dim());
            setOtherSideOrientation(otherSideInfo.orientation());
            reloadAndSyncToClient();
        }
    }

    private boolean checkBlockStatus() {
        if (wallPredicate == null) {
            return true;
        }

        boolean wallIntact = wallBox.fastStream().allMatch(p -> wallPredicate.test(getWorld(), p));
        if (!wallIntact) {
            return false;
        }

        return GatewayGunMod.isAreaClear(getWorld(), airBox);
    }

    private void playClosingSound() {
        getWorld().playSound(
                null,
                getX(), getY(), getZ(),
                GatewayGunMod.GATEWAY_CLOSE_EVENT,
                SoundCategory.PLAYERS,
                1.0F, 1.0F
        );
    }

    public String getColor() {
        if (customColor != null) {
            return customColor;
        }

        return this.id.side().getColorString();
    }

}
