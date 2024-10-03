package com.entropy;

import com.entropy.blocks.cubedispenser.CubeDispenser;
import com.entropy.blocks.gategrid.Gategrid;
import com.entropy.blocks.quantumfield.QuantumField;
import com.entropy.entity.Gateway;
import com.entropy.entity.GatewayGunBlockEntities;
import com.entropy.entity.WeightedCube;
import com.entropy.items.GatewayCore;
import com.entropy.items.GatewayGun;
import com.entropy.misc.BlockList;
import com.entropy.misc.SideSuggestionProvider;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.List;
import java.util.Optional;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.command.argument.BlockStateArgumentType.blockState;
import static net.minecraft.command.argument.BlockStateArgumentType.getBlockState;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static com.entropy.GatewayGunConstants.*;

public class GatewayGunMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    public static final GatewayGun GATEWAY_GUN = new GatewayGun();
    public static final GatewayCore GATEWAY_CORE = new GatewayCore();

    public static final Gategrid GATEGRID = new Gategrid();
    public static final QuantumField QUANTUM_FIELD = new QuantumField();
    public static final CubeDispenser CUBE_DISPENSER = new CubeDispenser();

    public static final Identifier GATEWAY1_SHOOT = id("gateway1_shoot");
    public static final Identifier GATEWAY2_SHOOT = id("gateway2_shoot");
    public static final Identifier GATEWAY_OPEN = id("gateway_open");
    public static final Identifier GATEWAY_CLOSE = id("gateway_close");
    public static final Identifier GRAB_START = id("grab_start");
    public static final Identifier GRAB_LOOP = id("grab_loop");
    public static final Identifier GRAB_STOP = id("grab_stop");

    public static final SoundEvent GATEWAY1_SHOOT_EVENT = SoundEvent.of(GATEWAY1_SHOOT);
    public static final SoundEvent GATEWAY2_SHOOT_EVENT = SoundEvent.of(GATEWAY2_SHOOT);
    public static final SoundEvent GATEWAY_OPEN_EVENT = SoundEvent.of(GATEWAY_OPEN);
    public static final SoundEvent GATEWAY_CLOSE_EVENT = SoundEvent.of(GATEWAY_CLOSE);
    public static final SoundEvent GRAB_START_EVENT = SoundEvent.of(GRAB_START);
    public static final SoundEvent GRAB_LOOP_EVENT = SoundEvent.of(GRAB_LOOP);
    public static final SoundEvent GRAB_STOP_EVENT = SoundEvent.of(GRAB_STOP);

    public static final RegistryKey<ItemGroup> TAB_KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), id("general"));
    public static final ItemGroup TAB = FabricItemGroup.builder()
            .icon(() -> new ItemStack(GatewayGunMod.GATEWAY_GUN))
            .displayName(Text.translatable("gatewaygun.item_group"))
            .build();

    public static final SimpleCommandExceptionType NOT_GATE_CORE = new SimpleCommandExceptionType(Text.translatable("fail.nocore"));
    public static final SimpleCommandExceptionType BAD_COLOR = new SimpleCommandExceptionType(Text.translatable("fail.badcolor"));
    public static final SimpleCommandExceptionType BAD_SIZE = new SimpleCommandExceptionType(Text.translatable("fail.badsize"));
    public static final SimpleCommandExceptionType BAD_BLOCK = new SimpleCommandExceptionType(Text.translatable("fail.badblock"));

    public static Identifier id(String path) {
        return new Identifier(MODID, path);
    }

    public static boolean isAreaClear(World world, IntBox airBox1) {
        return airBox1.fastStream().allMatch(
                p -> world.getBlockState(p).getCollisionShape(world, p).isEmpty()
        );
    }

    public static void registerBlock(String name, Block block, String tooltipTranslation) {
        Registry.register(Registries.BLOCK, id(name), block);
        Registry.register(Registries.ITEM, id(name), new BlockItem(block, new FabricItemSettings().rarity(Rarity.EPIC)) {
            @Override
            public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
                super.appendTooltip(stack, world, tooltip, context);
                tooltip.add(Text.translatable(tooltipTranslation));
            }
        });
    }

    @Override
    public void onInitialize() {
        AutoConfig.register(GatewayGunConfig.class, JanksonConfigSerializer::new);

        registerBlock("gategrid", GATEGRID, "block.gatewaygun.gategrid_desc");
        registerBlock("quantumfield", QUANTUM_FIELD, "block.gatewaygun.quantumfield_desc");
        registerBlock("cube_dispenser", CUBE_DISPENSER, "block.gatewaygun.cube_dispenser_desc");

        Registry.register(Registries.ITEM, id("gatewaygun"), GATEWAY_GUN);
        Registry.register(Registries.ITEM, id("gatewaycore"), GATEWAY_CORE);

        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(literal("cube").then(argument("block", blockState(access)).executes(ctx -> {
            WeightedCube cube = new WeightedCube(WeightedCube.entityType, ctx.getSource().getWorld());
            cube.setPosition(ctx.getSource().getPosition());
            cube.getDataTracker().set(WeightedCube.BLOCK, getBlockState(ctx, "block").getBlockState());
            ctx.getSource().getWorld().spawnEntity(cube);
            return Command.SINGLE_SUCCESS;
        }))));
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(literal("core").then(literal("color1").then(argument("color", string()).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                String col = getString(ctx, "color");
                try {
                    Integer.parseUnsignedInt(col, 16);
                } catch (NumberFormatException e) {
                    throw BAD_COLOR.create();
                }
                data.color1 = col;
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Color 1 set to " + data.color1), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        }))).then(literal("color2").then(argument("color", string()).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                String col = getString(ctx, "color");
                try {
                    Integer.parseUnsignedInt(col, 16);
                } catch (NumberFormatException e) {
                    throw BAD_COLOR.create();
                }
                data.color2 = col;
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Color 2 set to " + data.color2), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        }))).then(literal("width").then(argument("width", integer()).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                int width = getInteger(ctx, "width");
                if (width < 1) {
                    throw BAD_SIZE.create();
                }
                data.width = width;
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Width set to " + data.width), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        }))).then(literal("height").then(argument("height", integer()).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                int height = getInteger(ctx, "height");
                if (height < 1) {
                    throw BAD_SIZE.create();
                }
                data.height = height;
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Height set to " + data.height), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        }))).then(literal("gravity").then(argument("gravity", bool()).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                data.gravity = getBool(ctx, "gravity");
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Gravity set to " + data.gravity), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        }))).then(literal("gatecode").then(argument("code", integer()).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                data.code = getInteger(ctx, "code");
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Code set to " + data.code), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        }))).then(literal("pickup").then(argument("pickup", bool()).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                data.pickup = getBool(ctx, "pickup");
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Pickup set to " + data.pickup), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        }))).then(literal("blocks").then(literal("list").executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                player.sendMessage(Text.literal("Allowed blocks:").formatted(Formatting.DARK_AQUA));
                for (String block : data.allowedBlocks.list()) {
                    player.sendMessage(Text.literal(block).formatted(Formatting.AQUA));
                }
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        })).then(literal("add").then(argument("block", blockState(access)).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                Optional<RegistryKey<Block>> op = getBlockState(ctx, "block").getBlockState().getRegistryEntry().getKey();
                if (op.isEmpty()) {
                    throw BAD_BLOCK.create();
                }
                String block = op.get().getValue().toString();
                data.allowedBlocks.list().add(block);
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Added block " + block + " to allowlist"), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        }))).then(literal("remove").then(argument("block", blockState(access)).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                Optional<RegistryKey<Block>> op = getBlockState(ctx, "block").getBlockState().getRegistryEntry().getKey();
                if (op.isEmpty()) {
                    throw BAD_BLOCK.create();
                }
                String block = op.get().getValue().toString();
                data.allowedBlocks.list().remove(block);
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Removed block " + block + " from allowlist"), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        })))).then(literal("side").then(argument("side", string()).suggests(new SideSuggestionProvider(false, true)).executes(ctx -> {
            if (ctx.getSource().getEntity() instanceof PlayerEntity player && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof GatewayCore) {
                CoreData data = CoreData.fromTag(player.getStackInHand(Hand.MAIN_HAND).getOrCreateNbt(), true);
                data.restrictSide = GatewayRecord.GatewaySide.fromString(getString(ctx, "side"));
                if ("NONE".equals(getString(ctx, "side"))) {
                    data.restrictSide = null;
                }
                player.getStackInHand(Hand.MAIN_HAND).setNbt(data.toTag());
                player.sendMessage(Text.literal("Side set to " + data.restrictSide), true);
                return Command.SINGLE_SUCCESS;
            }
            throw NOT_GATE_CORE.create();
        })))));
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> dispatcher.register(literal("airResistance").then(argument("resistance", integer(-1000, 1000)).executes(ctx -> {
            GatewayRecord record = GatewayRecord.get();
            record.airResistance = getInteger(ctx, "resistance");
            record.setDirty(true);
            for (ServerPlayerEntity player : ctx.getSource().getWorld().getPlayers()) {
                McRemoteProcedureCall.tellClientToInvoke(player, "com.entropy.misc.RemoteCallables.updateAirResistance", record.airResistance);
            }
            return Command.SINGLE_SUCCESS;
        })))));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("cleargateways").then(argument("code", integer(0)).then(argument("side", string()).suggests(new SideSuggestionProvider(true, false)).executes(ctx -> {
            GatewayRecord record = GatewayRecord.get();
            String side = getString(ctx, "side");
            int code = getInteger(ctx, "code");
            if (side.equals("ONE") || side.equals("BOTH")) {
                record.data.remove(new GatewayRecord.GatewayID(code, GatewayRecord.GatewaySide.ONE));
            }
            if (side.equals("TWO") || side.equals("BOTH")) {
                record.data.remove(new GatewayRecord.GatewayID(code, GatewayRecord.GatewaySide.TWO));
            }
            ctx.getSource().sendFeedback(() -> Text.literal("Removed gateways for code " + code + ", sides " + side), true);
            return Command.SINGLE_SUCCESS;
        })))));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            GatewayRecord record = GatewayRecord.get();
            McRemoteProcedureCall.tellClientToInvoke(handler.getPlayer(), "com.entropy.misc.RemoteCallables.updateAirResistance", record.airResistance);
        });

        Registry.register(Registries.ENTITY_TYPE, id("gateway"), Gateway.entityType);
        Registry.register(Registries.ENTITY_TYPE, id("weighted_cube"), WeightedCube.entityType);

        GatewayGunBlockEntities.initialize();

        FabricDefaultAttributeRegistry.register(WeightedCube.entityType, WeightedCube.createLivingAttributes());

        Registry.register(Registries.SOUND_EVENT, GATEWAY1_SHOOT, GATEWAY1_SHOOT_EVENT);
        Registry.register(Registries.SOUND_EVENT, GATEWAY2_SHOOT, GATEWAY2_SHOOT_EVENT);
        Registry.register(Registries.SOUND_EVENT, GATEWAY_OPEN, GATEWAY_OPEN_EVENT);
        Registry.register(Registries.SOUND_EVENT, GATEWAY_CLOSE, GATEWAY_CLOSE_EVENT);
        Registry.register(Registries.SOUND_EVENT, GRAB_START, GRAB_START_EVENT);
        Registry.register(Registries.SOUND_EVENT, GRAB_LOOP, GRAB_LOOP_EVENT);
        Registry.register(Registries.SOUND_EVENT, GRAB_STOP, GRAB_STOP_EVENT);

        Registry.register(Registries.ITEM_GROUP, id("general"), TAB);

        ItemGroupEvents.modifyEntriesEvent(TAB_KEY).register(entries -> {
            entries.add(new CoreData(false).toStack(GATEWAY_GUN));

            entries.add(new CoreData().toStack(GATEWAY_GUN));
            entries.add(new CoreData().toStack(GATEWAY_CORE));

            entries.add(new CoreData(new BlockList(List.of("minecraft:quartz_block"))).toStack(GATEWAY_GUN));
            entries.add(new CoreData(new BlockList(List.of("minecraft:quartz_block"))).toStack(GATEWAY_CORE));

            entries.add(new CoreData(BlockList.createDefault(), "005ddf", "ee7f1b", false, null).toStack(GATEWAY_GUN));
            entries.add(new CoreData(BlockList.createDefault(), "005ddf", "ee7f1b", false, null).toStack(GATEWAY_CORE));

            entries.add(new CoreData(BlockList.createDefault(), defaultColor1, defaultColor2, false, GatewayRecord.GatewaySide.ONE).toStack(GATEWAY_GUN));
            entries.add(new CoreData(BlockList.createDefault(), defaultColor1, defaultColor2, false, GatewayRecord.GatewaySide.ONE).toStack(GATEWAY_CORE));

            entries.add(new CoreData(BlockList.createDefault(), defaultColor1, defaultColor2, false, defaultWidth, defaultHeight, 0, null, true, null, false).toStack(GATEWAY_GUN));
            entries.add(new CoreData(BlockList.createDefault(), defaultColor1, defaultColor2, false, defaultWidth, defaultHeight, 0, null, true, null, false).toStack(GATEWAY_CORE));

            entries.add(new CoreData(false).toStack(GATEGRID));
            entries.add(new ItemStack(QUANTUM_FIELD));
            entries.add(new ItemStack(CUBE_DISPENSER));
        });
    }
}
