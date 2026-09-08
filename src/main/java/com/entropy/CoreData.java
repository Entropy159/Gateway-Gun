package com.entropy;

import com.entropy.GatewayRecord.GatewaySide;
import com.entropy.misc.BlockList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static com.entropy.GatewayGunConstants.*;
import static com.entropy.items.GatewayGunComponents.GATEWAY_DATA;

public record CoreData(@NotNull BlockList allowedBlocks, int color1, int color2, int width, int height,
                       boolean gravity, boolean pickup, int code, boolean hasCore, @Nullable GatewaySide restrictSide,
                       @Nullable UUID grabbedEntityId) {
    public static final Codec<CoreData> CODEC = RecordCodecBuilder.create(builder -> builder.group(BlockList.CODEC.fieldOf("allowedBlocks").forGetter(CoreData::allowedBlocks), Codec.INT.fieldOf("color1").forGetter(CoreData::color1), Codec.INT.fieldOf("color2").forGetter(CoreData::color2), Codec.INT.fieldOf("width").forGetter(CoreData::width), Codec.INT.fieldOf("height").forGetter(CoreData::height), Codec.BOOL.fieldOf("gravity").forGetter(CoreData::gravity), Codec.BOOL.fieldOf("pickup").forGetter(CoreData::pickup), Codec.INT.fieldOf("code").forGetter(CoreData::code), Codec.BOOL.fieldOf("hasCore").forGetter(CoreData::hasCore), Codec.STRING.fieldOf("side").forGetter(data -> data.restrictSide == null ? "" : data.restrictSide.toString()), Codec.STRING.fieldOf("grabbedEntity").forGetter(data -> data.grabbedEntityId == null ? "" : data.grabbedEntityId.toString())).apply(builder, CoreData::new));

    public CoreData() {
        this(BlockList.createDefault());
    }

    public CoreData(@NotNull BlockList allowedBlocks) {
        this(allowedBlocks, defaultColor1, defaultColor2, false, null);
    }

    public CoreData(boolean hasCore) {
        this(BlockList.createDefault(), defaultColor1, defaultColor2, defaultWidth, defaultHeight, false, true, 0, hasCore, null, null);
    }

    public CoreData(@NotNull BlockList allowedBlocks, int side1Color, int side2Color, boolean transformGravity, @Nullable GatewaySide side) {
        this(allowedBlocks, side1Color, side2Color, defaultWidth, defaultHeight, transformGravity, 0, side);
    }

    public CoreData(@NotNull BlockList allowedBlocks, int side1Color, int side2Color, int width, int height, boolean transformGravity, int id, @Nullable GatewaySide side) {
        this(allowedBlocks, side1Color, side2Color, width, height, transformGravity, true, id, true, side, null);
    }

    public CoreData(BlockList allowedBlocks, int side1Color, int side2Color, Integer width, Integer height, Boolean transformGravity, Boolean pickup, Integer id, Boolean hasCore, String side, String grabbed) {
        this(allowedBlocks, side1Color, side2Color, width, height, transformGravity, pickup, id, hasCore, side.isEmpty() ? null : GatewaySide.valueOf(side), grabbed.isEmpty() ? null : UUID.fromString(grabbed));
    }

    public static CoreData get(ItemStack stack, boolean hasCore) {
        return stack.getOrDefault(GATEWAY_DATA, new CoreData(hasCore));
    }

    public static CoreData fromTag(NbtCompound tag, boolean shouldHaveCore) {
        if (tag.isEmpty()) {
            return new CoreData(shouldHaveCore);
        }

        BlockList allowedBlocks = BlockList.fromTag(tag.getList("allowedBlocks", NbtElement.STRING_TYPE));

        int side1Color = tag.contains("color1", NbtElement.STRING_TYPE) ? Integer.parseInt(tag.getString("color1")) : tag.getInt("color1");
        int side2Color = tag.contains("color2", NbtElement.STRING_TYPE) ? Integer.parseInt(tag.getString("color2")) : tag.getInt("color2");

        int w = tag.contains("width") ? tag.getInt("width") : defaultWidth;
        int h = tag.contains("height") ? tag.getInt("height") : defaultHeight;

        boolean transformGravity = tag.contains("transformGravity") && tag.getBoolean("transformGravity");

        int id = tag.getInt("code");

        @Nullable GatewaySide side = tag.contains("restrictSide") ? GatewaySide.valueOf(tag.getString("restrictSide")) : null;

        boolean hasCore = tag.getBoolean("hasCore");

        boolean canPickup = tag.getBoolean("pickup");

        @Nullable UUID grabbed = tag.contains("grabbedEntityId") ? tag.getUuid("grabbedEntityId") : null;

        return new CoreData(allowedBlocks, side1Color, side2Color, w, h, transformGravity, canPickup, id, hasCore, side, grabbed);
    }

    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("allowedBlocks", allowedBlocks.toTag());
        tag.putInt("color1", color1);
        tag.putInt("color2", color2);
        tag.putInt("width", width);
        tag.putInt("height", height);
        tag.putBoolean("transformGravity", gravity);
        tag.putInt("code", code);
        if (restrictSide != null) {
            tag.putString("restrictSide", restrictSide.name());
        }
        tag.putBoolean("hasCore", hasCore);
        if (grabbedEntityId != null) {
            tag.putUuid("grabbedEntityId", grabbedEntityId);
        }
        tag.putBoolean("pickup", pickup);
        return tag;
    }

    public ItemStack toStack(ItemConvertible item) {
        ItemStack stack = new ItemStack(item);
        stack.set(GATEWAY_DATA, this);
        return stack;
    }

    public int getCustomColor(GatewayRecord.GatewaySide side) {
        return switch (side) {
            case ONE -> color1;
            case TWO -> color2;
        };
    }

    public void setTooltip(List<Text> tooltip) {
        if (hasCore) {
            if (!allowedBlocks.list().isEmpty()) {
                tooltip.add(Text.translatable("gatewaygun.limit_allowed_blocks"));
                int displayLimit = 5;
                List<Block> allowed = allowedBlocks.asStream().limit(displayLimit + 1).toList();
                for (int i = 0; i < displayLimit; i++) {
                    if (i < allowed.size()) {
                        Block block = allowed.get(i);
                        tooltip.add(block.getName().formatted(Formatting.LIGHT_PURPLE));
                    }
                }
                if (allowed.size() > displayLimit) {
                    tooltip.add(Text.literal("..."));
                }
            }

            tooltip.add(Text.empty().append(Text.literal("█").setStyle(Style.EMPTY.withColor(color1))).append(" ").append(Text.literal("█").setStyle(Style.EMPTY.withColor(color2))));
            if (width != defaultWidth || height != defaultHeight) {
                tooltip.add(Text.literal("Size: " + width + "x" + height).formatted(Formatting.BLUE));
            }

            if (gravity) {
                tooltip.add(Text.translatable("gatewaygun.transform_gravity").formatted(Formatting.AQUA));
            }

            if (restrictSide != null) {
                tooltip.add(Text.literal("Restricted to side " + restrictSide.name()).formatted(Formatting.DARK_RED));
            }

            if (!pickup) {
                tooltip.add(Text.literal("No entity pickup").formatted(Formatting.RED));
            }

            tooltip.add(Text.literal("Gateway Code: " + code).formatted(Formatting.YELLOW));
        } else {
            tooltip.add(Text.literal("No gate core").formatted(Formatting.RED));
        }
    }

    public CoreData withBlockList(BlockList val) {
        return new CoreData(val, color1, color2, width, height, gravity, pickup, code, hasCore, restrictSide, grabbedEntityId);
    }

    public CoreData withColor1(int val) {
        return new CoreData(allowedBlocks, val, color2, width, height, gravity, pickup, code, hasCore, restrictSide, grabbedEntityId);
    }

    public CoreData withColor2(int val) {
        return new CoreData(allowedBlocks, color1, val, width, height, gravity, pickup, code, hasCore, restrictSide, grabbedEntityId);
    }

    public CoreData withWidth(int val) {
        return new CoreData(allowedBlocks, color1, color2, val, height, gravity, pickup, code, hasCore, restrictSide, grabbedEntityId);
    }

    public CoreData withHeight(int val) {
        return new CoreData(allowedBlocks, color1, color2, width, val, gravity, pickup, code, hasCore, restrictSide, grabbedEntityId);
    }

    public CoreData withGravity(boolean val) {
        return new CoreData(allowedBlocks, color1, color2, width, height, val, pickup, code, hasCore, restrictSide, grabbedEntityId);
    }

    public CoreData withPickup(boolean val) {
        return new CoreData(allowedBlocks, color1, color2, width, height, gravity, val, code, hasCore, restrictSide, grabbedEntityId);
    }

    public CoreData withCode(int val) {
        return new CoreData(allowedBlocks, color1, color2, width, height, gravity, pickup, val, hasCore, restrictSide, grabbedEntityId);
    }

    public CoreData withHasCore(boolean val) {
        return new CoreData(allowedBlocks, color1, color2, width, height, gravity, pickup, code, val, restrictSide, grabbedEntityId);
    }

    public CoreData withRestrictSide(@Nullable GatewaySide val) {
        return new CoreData(allowedBlocks, color1, color2, width, height, gravity, pickup, code, hasCore, val, grabbedEntityId);
    }

    public CoreData withGrabbed(@Nullable UUID val) {
        return new CoreData(allowedBlocks, color1, color2, width, height, gravity, pickup, code, hasCore, restrictSide, val);
    }
}
