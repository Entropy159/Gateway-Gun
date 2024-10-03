package com.entropy.blocks.cubedispenser;

import com.entropy.entity.GatewayGunBlockEntities;
import com.entropy.entity.WeightedCube;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CubeDispenser extends BlockWithEntity implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    public CubeDispenser() {
        this(Settings.create().strength(10, 10).noCollision());
    }

    public CubeDispenser(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(POWERED, false));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof CubeDispenserBlockEntity dispenser) {
                if (player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof BlockItem item) {
                    dispenser.state = item.getBlock().getDefaultState();
                    dispenser.markDirty();
                    world.updateListeners(pos, state, state, 0);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, GatewayGunBlockEntities.CUBE_DISPENSER, CubeDispenser::tick);
    }

    private static void tick(World w, BlockPos pos, BlockState state, CubeDispenserBlockEntity dispenser) {
        if (!w.isClient) {
            ServerWorld world = (ServerWorld) w;
            if (!(world.getEntity(dispenser.uuid) instanceof WeightedCube)) {
                dispenser.uuid = null;
                if (dispenser.delayTicks > 0) {
                    dispenser.delayTicks--;
                } else if (world.isReceivingRedstonePower(pos)) {
                    dispenser.spawnCube(false);
                }
            }
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World w, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, w, pos, sourceBlock, sourcePos, notify);
        if (!w.isClient) {
            ServerWorld world = (ServerWorld) w;
            if (world.getBlockEntity(pos) instanceof CubeDispenserBlockEntity dispenser) {
                if (world.isReceivingRedstonePower(pos) && !state.get(POWERED)) {
                    dispenser.spawnCube(true);
                    world.setBlockState(pos, state.with(POWERED, true));
                } else if (!world.isReceivingRedstonePower(pos) && state.get(POWERED)) {
                    world.setBlockState(pos, state.with(POWERED, false));
                }
            }
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(FACING, ctx.getSide()).with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(CubeDispenser::new);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CubeDispenserBlockEntity(pos, state);
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

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }
}
