package com.entropy.blocks.quantumfield;

import com.entropy.CoreData;
import com.entropy.GatewayGunMod;
import com.entropy.GatewayRecord;
import com.entropy.entity.WeightedCube;
import com.entropy.items.GatewayGun;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("deprecation")
public class QuantumField extends BlockWithEntity implements BlockEntityProvider {
    public static final MapCodec<QuantumField> CODEC = createCodec(QuantumField::new);
    public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
    public static final BooleanProperty EAST = ConnectingBlock.EAST;
    public static final BooleanProperty SOUTH = ConnectingBlock.SOUTH;
    public static final BooleanProperty WEST = ConnectingBlock.WEST;
    protected static final Map<Direction, BooleanProperty> FACING_PROPERTIES = ConnectingBlock.FACING_PROPERTIES.entrySet().stream().filter((entry) -> entry.getKey().getAxis().isHorizontal()).collect(Util.toMap());
    protected final VoxelShape[] collisionShapes;
    protected final VoxelShape[] boundingShapes;
    private final Object2IntMap<BlockState> SHAPE_INDEX_CACHE = new Object2IntOpenHashMap<>();

    public static final BooleanProperty ENABLED = Properties.ENABLED;

    public QuantumField() {
        this(Settings.create().strength(1, 20).pistonBehavior(PistonBehavior.IGNORE).noCollision().luminance(s -> s.get(ENABLED) ? 3 : 0));
    }

    public QuantumField(Settings settings) {
        super(settings);
        this.collisionShapes = this.createShapes();
        this.boundingShapes = this.createShapes();
        for (BlockState blockState : this.stateManager.getStates()) {
            this.getShapeIndex(blockState);
        }

        setDefaultState(getDefaultState().with(ENABLED, true).with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ENABLED);
        builder.add(NORTH, EAST, WEST, SOUTH);
    }

    @Override
    public void neighborUpdate(BlockState state, World w, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, w, pos, sourceBlock, sourcePos, notify);
        if (!w.isClient) {
            ServerWorld world = (ServerWorld) w;
            boolean powered = isPowered(world, pos);
            if (powered == state.get(ENABLED)) {
                world.setBlockState(pos, state.with(ENABLED, !powered), 2);
                world.updateNeighborsAlways(pos, this);
            }
        }
    }

    public boolean isPowered(ServerWorld world, BlockPos pos) {
        return isPowered(world, pos, new ArrayList<>());
    }

    public boolean isPowered(ServerWorld world, BlockPos pos, ArrayList<BlockPos> previous) {
        previous.add(pos);
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.add(dir.getVector());
            if (world.getBlockState(neighbor).isOf(this) && !previous.contains(neighbor)) {
                if (isPowered(world, neighbor, previous)) {
                    return true;
                }
            }
        }
        return world.isReceivingRedstonePower(pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(ENABLED) ? this.boundingShapes[this.getShapeIndex(state)] : VoxelShapes.empty();
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return state.get(ENABLED) ? super.getRaycastShape(state, world, pos) : VoxelShapes.empty();
    }

    @Override
    public boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }

    @Override
    public void onEntityCollision(BlockState state, World w, BlockPos pos, Entity entity) {
        super.onEntityCollision(state, w, pos, entity);
        if (!w.isClient && state.get(ENABLED)) {
            if (entity instanceof WeightedCube cube) {
                cube.kill();
            }
            if (entity instanceof PlayerEntity player) {
                boolean cleared = false;
                for (int s = 0; s < player.getInventory().size(); s++) {
                    ItemStack stack = player.getInventory().getStack(s);
                    if (stack.getItem() instanceof GatewayGun) {
                        CoreData data = CoreData.fromTag(stack.getOrCreateNbt(), false);
                        GatewayRecord record = GatewayRecord.get();
                        record.data.remove(new GatewayRecord.GatewayID(data.code, GatewayRecord.GatewaySide.ONE));
                        record.data.remove(new GatewayRecord.GatewayID(data.code, GatewayRecord.GatewaySide.TWO));
                        record.setDirty(true);
                        cleared = true;
                    }
                }
                if (cleared) {
                    w.playSound(player, player.getEyePos().x, player.getEyePos().y, player.getEyePos().z, GatewayGunMod.GATEWAY_CLOSE_EVENT, SoundCategory.PLAYERS, 1F, 1F);
                }
            }
        }
    }

    @Override
    public MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumFieldBlockEntity(pos, state);
    }

    protected VoxelShape[] createShapes() {
        float f = 7.0F;
        float g = 9.0F;
        float h = 7.0F;
        float i = 9.0F;
        VoxelShape voxelShape = Block.createCuboidShape(f, 0.0, f, g, 16F, g);
        VoxelShape voxelShape2 = Block.createCuboidShape(h, 0F, 0.0, i, 16F, i);
        VoxelShape voxelShape3 = Block.createCuboidShape(h, 0F, h, i, 16F, 16.0);
        VoxelShape voxelShape4 = Block.createCuboidShape(0.0, 0F, h, i, 16F, i);
        VoxelShape voxelShape5 = Block.createCuboidShape(h, 0F, h, 16.0, 16F, i);
        VoxelShape voxelShape6 = VoxelShapes.union(voxelShape2, voxelShape5);
        VoxelShape voxelShape7 = VoxelShapes.union(voxelShape3, voxelShape4);
        VoxelShape[] voxelShapes = new VoxelShape[]{VoxelShapes.empty(), voxelShape3, voxelShape4, voxelShape7, voxelShape2, VoxelShapes.union(voxelShape3, voxelShape2), VoxelShapes.union(voxelShape4, voxelShape2), VoxelShapes.union(voxelShape7, voxelShape2), voxelShape5, VoxelShapes.union(voxelShape3, voxelShape5), VoxelShapes.union(voxelShape4, voxelShape5), VoxelShapes.union(voxelShape7, voxelShape5), voxelShape6, VoxelShapes.union(voxelShape3, voxelShape6), VoxelShapes.union(voxelShape4, voxelShape6), VoxelShapes.union(voxelShape7, voxelShape6)};

        for(int j = 0; j < 16; ++j) {
            voxelShapes[j] = VoxelShapes.union(voxelShape, voxelShapes[j]);
        }

        return voxelShapes;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    private static int getDirectionMask(Direction dir) {
        return 1 << dir.getHorizontal();
    }

    protected int getShapeIndex(BlockState state) {
        return this.SHAPE_INDEX_CACHE.computeIntIfAbsent(state, (statex) -> {
            int i = 0;
            if (statex.get(NORTH)) {
                i |= getDirectionMask(Direction.NORTH);
            }

            if (statex.get(EAST)) {
                i |= getDirectionMask(Direction.EAST);
            }

            if (statex.get(SOUTH)) {
                i |= getDirectionMask(Direction.SOUTH);
            }

            if (statex.get(WEST)) {
                i |= getDirectionMask(Direction.WEST);
            }

            return i;
        });
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return !state.get(ENABLED);
    }

    public BlockState rotate(BlockState state, BlockRotation rotation) {
        switch (rotation) {
            case CLOCKWISE_180 -> {
                return state.with(NORTH, state.get(SOUTH)).with(EAST, state.get(WEST)).with(SOUTH, state.get(NORTH)).with(WEST, state.get(EAST));
            }
            case COUNTERCLOCKWISE_90 -> {
                return state.with(NORTH, state.get(EAST)).with(EAST, state.get(SOUTH)).with(SOUTH, state.get(WEST)).with(WEST, state.get(NORTH));
            }
            case CLOCKWISE_90 -> {
                return state.with(NORTH, state.get(WEST)).with(EAST, state.get(NORTH)).with(SOUTH, state.get(EAST)).with(WEST, state.get(SOUTH));
            }
            default -> {
                return state;
            }
        }
    }

    public BlockState mirror(BlockState state, BlockMirror mirror) {
        switch (mirror) {
            case LEFT_RIGHT -> {
                return state.with(NORTH, state.get(SOUTH)).with(SOUTH, state.get(NORTH));
            }
            case FRONT_BACK -> {
                return state.with(EAST, state.get(WEST)).with(WEST, state.get(EAST));
            }
            default -> {
                return super.mirror(state, mirror);
            }
        }
    }

    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockView blockView = ctx.getWorld();
        BlockPos blockPos = ctx.getBlockPos();
        BlockPos blockPos2 = blockPos.north();
        BlockPos blockPos3 = blockPos.south();
        BlockPos blockPos4 = blockPos.west();
        BlockPos blockPos5 = blockPos.east();
        BlockState blockState = blockView.getBlockState(blockPos2);
        BlockState blockState2 = blockView.getBlockState(blockPos3);
        BlockState blockState3 = blockView.getBlockState(blockPos4);
        BlockState blockState4 = blockView.getBlockState(blockPos5);
        return this.getDefaultState().with(NORTH, this.connectsTo(blockState, blockState.isSideSolidFullSquare(blockView, blockPos2, Direction.SOUTH))).with(SOUTH, this.connectsTo(blockState2, blockState2.isSideSolidFullSquare(blockView, blockPos3, Direction.NORTH))).with(WEST, this.connectsTo(blockState3, blockState3.isSideSolidFullSquare(blockView, blockPos4, Direction.EAST))).with(EAST, this.connectsTo(blockState4, blockState4.isSideSolidFullSquare(blockView, blockPos5, Direction.WEST)));
    }

    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? state.with(FACING_PROPERTIES.get(direction), this.connectsTo(neighborState, neighborState.isSideSolidFullSquare(world, neighborPos, direction.getOpposite()))) : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        if (stateFrom.isOf(this)) {
            if (!direction.getAxis().isHorizontal()) {
                return true;
            }

            if (state.get(FACING_PROPERTIES.get(direction)) && stateFrom.get(FACING_PROPERTIES.get(direction.getOpposite()))) {
                return true;
            }
        }

        return super.isSideInvisible(state, stateFrom, direction);
    }

    public final boolean connectsTo(BlockState state, boolean sideSolidFullSquare) {
        return !cannotConnect(state) && sideSolidFullSquare || state.getBlock() instanceof QuantumField || state.isIn(BlockTags.WALLS);
    }
}
