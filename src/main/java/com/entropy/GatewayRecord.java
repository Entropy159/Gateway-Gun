package com.entropy;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.DQuaternion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.entropy.GatewayGunConstants.*;

public class GatewayRecord extends PersistentState {
    public enum GatewaySide {
        ONE, // left click
        TWO; // right click

        public static GatewaySide fromString(String c) {
            if ("TWO".equals(c)) {
                return TWO;
            }
            return ONE;
        }

        public GatewaySide getTheOtherSide() {
            return switch (this) {
                case TWO -> ONE;
                case ONE -> TWO;
            };
        }

        public String getColorString() {
            return switch (this) {
                case ONE -> defaultColor1;
                case TWO -> defaultColor2;
            };
        }
    }

    public record GatewayID(int code, GatewaySide side) {
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putInt("code", code);
            tag.putString("side", side.name());
            return tag;
        }

        public static GatewayID fromTag(NbtCompound tag) {
            return new GatewayID(
                    tag.getInt("code"),
                    GatewaySide.fromString(tag.getString("side"))
            );
        }

        public GatewayID getTheOtherSide() {
            return new GatewayID(code, side.getTheOtherSide());
        }

        @Override
        public String toString() {
            return "GatewayID: channel "+code+", side "+side.name();
        }
    }

    public record GatewayInfo(
            UUID id,
            RegistryKey<World> dim,
            Vec3d pos,
            DQuaternion orientation,
            int updateCounter
    ) {
        NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("id", id);
            tag.putString("dim", dim.getValue().toString());
            tag.putDouble("x", pos.x);
            tag.putDouble("y", pos.y);
            tag.putDouble("z", pos.z);
            tag.put("orientation", orientation.toTag());
            tag.putInt("updateCounter", updateCounter);
            return tag;
        }

        static GatewayInfo fromTag(NbtCompound tag) {
            return new GatewayInfo(
                    tag.getUuid("id"),
                    RegistryKey.of(
                            RegistryKeys.WORLD,
                            new Identifier(tag.getString("dim"))
                    ),
                    new Vec3d(
                            tag.getDouble("x"),
                            tag.getDouble("y"),
                            tag.getDouble("z")
                    ),
                    DQuaternion.fromTag(tag.getCompound("orientation")),
                    tag.getInt("updateCounter")
            );
        }
    }

    public final Map<GatewayID, GatewayInfo> data;
    public int airResistance;

    public GatewayRecord(Map<GatewayID, GatewayInfo> data, int resistance) {
        this.data = data;
        this.airResistance = resistance;
    }

    @SuppressWarnings("deprecation")
    public static GatewayRecord get() {
        ServerWorld overworld = MiscHelper.getServer().getOverworld();

        return overworld.getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(
                        () -> {
                            GatewayGunMod.LOGGER.info("Gateway record initialized ");
                            return new GatewayRecord(new HashMap<>(), 90);
                        },
                        GatewayRecord::load,
                        null
                ),
                "gateway_record"
        );
    }

    @Override
    public @NotNull NbtCompound writeNbt(NbtCompound compoundTag) {
        NbtList dataTag = new NbtList();

        data.forEach((key, value) -> {
            NbtCompound entryTag = new NbtCompound();
            entryTag.put("key", key.toTag());
            entryTag.put("value", value.toTag());
            dataTag.add(entryTag);
        });

        compoundTag.putInt("airResistance", airResistance);

        compoundTag.put("data", dataTag);

        return compoundTag;
    }

    public static GatewayRecord load(NbtCompound compoundTag) {
        NbtList dataTag = compoundTag.getList("data", 10);

        try {
            Map<GatewayID, GatewayInfo> data = dataTag.stream()
                    .map(NbtCompound.class::cast)
                    .collect(Collectors.toMap(
                            entryTag -> GatewayID.fromTag(entryTag.getCompound("key")),
                            entryTag -> GatewayInfo.fromTag(entryTag.getCompound("value"))
                    ));

            return new GatewayRecord(data, compoundTag.getInt("airResistance"));
        } catch (Exception e) {
            GatewayGunMod.LOGGER.error("Failed to deserialize gateway info", e);
            return new GatewayRecord(new HashMap<>(), 90);
        }
    }
}
