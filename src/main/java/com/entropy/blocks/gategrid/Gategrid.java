package com.entropy.blocks.gategrid;

import com.entropy.CoreData;
import com.entropy.GatewayGunMod;
import com.entropy.GatewayRecord;
import com.entropy.items.GatewayCore;
import com.entropy.misc.GatewayGunUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.my_util.AARotation;
import qouteall.q_misc_util.my_util.IntBox;

public class Gategrid extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public Gategrid() {
        this(Settings.create().luminance(s -> s.get(POWERED) ? 10 : 1).noCollision().nonOpaque().pistonBehavior(PistonBehavior.BLOCK).strength(1, 20));
    }

    public Gategrid(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(POWERED, false).with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(POWERED);
        builder.add(HALF);
    }

    public VoxelShape getShape(BlockState state, BlockView world, BlockPos pos) {
        return switch (state.get(FACING)) {
            case NORTH -> VoxelShapes.cuboid(0, 0, 0, 1, 1, 0.5 / 16);
            case EAST -> VoxelShapes.cuboid(15.5 / 16, 0, 0, 1, 1, 1);
            case WEST -> VoxelShapes.cuboid(0, 0, 0, 0.5 / 16, 1, 1);
            case UP -> VoxelShapes.cuboid(0, 15.5 / 16, 0, 1, 1, 1);
            case DOWN -> VoxelShapes.cuboid(0, 0, 0, 1, 0.5 / 16, 1);
            default -> VoxelShapes.cuboid(0, 0, 15.5 / 16, 1, 1, 1);
        };
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShape(state, world, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return getShape(state, world, pos);
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return getShape(state, world, pos);
    }

    @Override
    public BlockState onBreak(World w, BlockPos pos, BlockState state, PlayerEntity player) {
        if(!w.isClient){
            ServerWorld world = (ServerWorld)w;
            BlockPos pos2;
            if (state.get(HALF) == DoubleBlockHalf.UPPER) {
                pos = pos.down();
                pos2 = pos;
                state = world.getBlockState(pos);
            } else {
                pos2 = pos.up();
            }
            BlockEntity entity = world.getBlockEntity(pos);
            if (state.isOf(state.getBlock())) {
                if (entity instanceof GategridBlockEntity gategrid) {
                    if(gategrid.data.hasCore){
                        Vec3d pos3 = pos.toCenterPos();
                        ItemEntity core = new ItemEntity(world, pos3.x, pos3.y, pos3.z, gategrid.data.toStack(GatewayGunMod.GATEWAY_CORE));
                        world.spawnEntity(core);
                        GatewayRecord record = GatewayRecord.get();
                        GatewayRecord.GatewayID id = new GatewayRecord.GatewayID(gategrid.data.code, gategrid.data.restrictSide == null ? GatewayRecord.GatewaySide.TWO : gategrid.data.restrictSide);
                        record.data.remove(id);
                        record.setDirty(true);
                    }
                }
                world.setBlockState(pos2, Blocks.AIR.getDefaultState());
            }
        }
        return super.onBreak(w, pos, state, player);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        if (ctx.getBlockPos().getY() < ctx.getWorld().getTopY() - 1 && ctx.getWorld().getBlockState(ctx.getBlockPos().up()).canReplace(ctx)) {
            return super.getPlacementState(ctx).with(FACING, ctx.getHorizontalPlayerFacing()).with(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);
        return state.get(HALF) == DoubleBlockHalf.LOWER ? blockState.isSideSolidFullSquare(world, blockPos, Direction.UP) : blockState.isOf(this);
    }

    @Override
    public @NotNull ActionResult onUse(@NotNull BlockState state, @NotNull World level, @NotNull BlockPos pos, @NotNull PlayerEntity player, @NotNull Hand hand, @NotNull BlockHitResult hit) {
        if (!level.isClient) {
            if (state.get(HALF) == DoubleBlockHalf.UPPER) {
                pos = pos.down();
                state = level.getBlockState(pos);
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof GategridBlockEntity gategrid && !state.get(POWERED)) {
                ItemStack held = player.getStackInHand(hand);
                if (gategrid.data.hasCore) {
                    ItemStack newCore = gategrid.data.toStack(GatewayGunMod.GATEWAY_CORE);
                    if (held.isEmpty()) {
                        player.setStackInHand(hand, newCore);
                    } else {
                        player.giveItemStack(newCore);
                    }
                    gategrid.data = new CoreData(false);
                } else if (held.getItem() instanceof GatewayCore) {
                    gategrid.data = CoreData.fromTag(held.getOrCreateNbt(), false);
                    player.setStackInHand(hand, ItemStack.EMPTY);
                }
                level.updateListeners(pos, state, state, 0);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient) {
            ServerWorld server = (ServerWorld) world;
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof GategridBlockEntity gategrid && gategrid.data.hasCore && state.get(HALF) == DoubleBlockHalf.LOWER) {
                GatewayRecord record = GatewayRecord.get();
                if (world.isReceivingRedstonePower(pos) && !state.get(POWERED)) {
                    Direction right = state.get(FACING).rotateYClockwise();
                    Direction up = Direction.UP;
                    AARotation rot = AARotation.getAARotationFromYZ(up, right);
                    BlockPos transformedSize = rot.transform(new BlockPos(gategrid.data.width, gategrid.data.height, 1));
                    IntBox portalArea = IntBox.getBoxByPosAndSignedSize(pos, transformedSize);
                    IntBox wallArea = portalArea.getMoved(state.get(FACING).getOpposite().getVector());
                    GatewayGunUtils.placeGateway(server, pos.toCenterPos().add(0, 0.5, 0).add(Vec3d.of(state.get(FACING).getVector()).multiply(0.5 - GatewayGunMod.gatewayOffset)), Vec3d.of(up.getVector()), Vec3d.of(right.getVector()), gategrid.data, gategrid.data.restrictSide == null ? GatewayRecord.GatewaySide.TWO : gategrid.data.restrictSide, portalArea, wallArea, false);
                    world.playSound(null, pos, GatewayGunMod.GATEWAY_OPEN_EVENT, SoundCategory.BLOCKS, 1, 1);
                    world.setBlockState(pos, state.with(POWERED, true));
                } else if (!world.isReceivingRedstonePower(pos) && state.get(POWERED)) {
                    GatewayRecord.GatewayID id = new GatewayRecord.GatewayID(gategrid.data.code, gategrid.data.restrictSide == null ? GatewayRecord.GatewaySide.TWO : gategrid.data.restrictSide);
                    record.data.remove(id);
                    world.playSound(null, pos, GatewayGunMod.GATEWAY_CLOSE_EVENT, SoundCategory.BLOCKS, 1, 1);
                    world.setBlockState(pos, state.with(POWERED, false));
                }
                record.setDirty(true);
            }
        }
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(Gategrid::new);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GategridBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

}