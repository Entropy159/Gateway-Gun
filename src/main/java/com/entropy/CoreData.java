package com.entropy;

import com.entropy.GatewayRecord.GatewaySide;
import com.entropy.misc.BlockList;
import com.entropy.misc.GatewayGunUtils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static com.entropy.GatewayGunConstants.*;

public class CoreData {
    public @NotNull BlockList allowedBlocks;

    public String color1;
    public String color2;

    public int width;
    public int height;

    public boolean gravity;
    public boolean pickup;

    public int code;

    public @Nullable GatewaySide restrictSide;

    public boolean hasCore;

    public @Nullable UUID grabbedEntityId;

    public CoreData() {
        this(BlockList.createDefault());
    }

    public CoreData(@NotNull BlockList allowedBlocks) {
        this(allowedBlocks, defaultColor1, defaultColor2, false, null);
    }

    public CoreData(boolean hasCore) {
        this(BlockList.createDefault(), defaultColor1, defaultColor2, false, defaultWidth, defaultHeight, 0, null, hasCore, null, true);
    }

    public CoreData(@NotNull BlockList allowedBlocks, String side1Color, String side2Color, boolean transformGravity, @Nullable GatewaySide side) {
        this(allowedBlocks, side1Color, side2Color, transformGravity, defaultWidth, defaultHeight, 0, side);
    }

    public CoreData(@NotNull BlockList allowedBlocks, String side1Color, String side2Color, boolean transformGravity, int width, int height, int id, @Nullable GatewaySide side) {
        this(allowedBlocks, side1Color, side2Color, transformGravity, width, height, id, side, true, null, true);
    }

    public CoreData(@NotNull BlockList allowedBlocks, String side1Color, String side2Color, boolean transformGravity, int width, int height, int id, @Nullable GatewaySide side, boolean core, @Nullable UUID grabbed, boolean pickup) {
        this.allowedBlocks = allowedBlocks;
        this.color1 = side1Color;
        this.color2 = side2Color;
        this.gravity = transformGravity;
        this.width = width;
        this.height = height;
        this.code = id;
        this.restrictSide = side;
        this.hasCore = core;
        this.grabbedEntityId = grabbed;
        this.pickup = pickup;
    }

    public static CoreData fromTag(NbtCompound tag, boolean shouldHaveCore) {
        if (tag.isEmpty()) {
            return new CoreData(shouldHaveCore);
        }

        BlockList allowedBlocks = BlockList.fromTag(tag.getList("allowedBlocks", NbtElement.STRING_TYPE));

        String side1Color = tag.getString("color1");
        String side2Color = tag.getString("color2");

        int w = tag.contains("width") ? tag.getInt("width") : defaultWidth;
        int h = tag.contains("height") ? tag.getInt("height") : defaultHeight;

        boolean transformGravity = tag.contains("transformGravity") && tag.getBoolean("transformGravity");

        int id = tag.getInt("code");

        @Nullable GatewaySide side = tag.contains("restrictSide") ? GatewaySide.fromString(tag.getString("restrictSide")) : null;

        boolean hasCore = tag.getBoolean("hasCore");

        boolean canPickup = tag.getBoolean("pickup");

        @Nullable UUID grabbed = tag.contains("grabbedEntityId") ? tag.getUuid("grabbedEntityId") : null;

        return new CoreData(allowedBlocks, side1Color, side2Color, transformGravity, w, h, id, side, hasCore, grabbed, canPickup);
    }

    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("allowedBlocks", allowedBlocks.toTag());
        tag.putString("color1", color1);
        tag.putString("color2", color2);
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
        stack.setNbt(toTag());
        return stack;
    }

    public @Nullable String getCustomColor(GatewayRecord.GatewaySide side) {
        return switch (side) {
            case ONE -> color1;
            case TWO -> color2;
        };
    }

    public String getColor(GatewayRecord.GatewaySide side) {
        String customColor = getCustomColor(side);
        if (customColor != null) {
            return customColor;
        }

        return side.getColorString();
    }

    @Override
    public String toString() {
        return "CoreData{" + "allowedBlocks=" + allowedBlocks + ", color1=" + color1 + ", color2=" + color2 + ", width=" + width + ", height=" + height + '}';
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

            tooltip.add(Text.empty().append(Text.literal("█").setStyle(Style.EMPTY.withColor(GatewayGunUtils.hexToInt(color1)))).append(" ").append(Text.literal("█").setStyle(Style.EMPTY.withColor(GatewayGunUtils.hexToInt(color2)))));
            if (width != defaultWidth || height != defaultHeight) {
                tooltip.add(Text.literal("Size: " + width + "x" + height).formatted(Formatting.BLUE));
            }

            if (gravity) {
                tooltip.add(Text.translatable("gatewaygun.transform_gravity").formatted(Formatting.AQUA));
            }

            if (restrictSide != null) {
                tooltip.add(Text.literal("Restricted to side " + restrictSide.name()).formatted(Formatting.DARK_RED));
            }

            if(!pickup){
                tooltip.add(Text.literal("No entity pickup").formatted(Formatting.RED));
            }

            tooltip.add(Text.literal("Gateway Code: " + code).formatted(Formatting.YELLOW));
        } else {
            tooltip.add(Text.literal("No gate core").formatted(Formatting.RED));
        }
    }
}
