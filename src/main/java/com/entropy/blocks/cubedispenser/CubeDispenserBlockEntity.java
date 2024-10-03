package com.entropy.blocks.cubedispenser;

import com.entropy.entity.GatewayGunBlockEntities;
import com.entropy.entity.WeightedCube;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.UUID;

import static com.entropy.GatewayGunConstants.*;

public class CubeDispenserBlockEntity extends BlockEntity {
    public BlockState state = Blocks.QUARTZ_BLOCK.getDefaultState();
    public @Nullable UUID uuid = null;
    public int delayTicks = dispenserDelayTicks;

    public CubeDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(GatewayGunBlockEntities.CUBE_DISPENSER, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("block_state", NbtHelper.fromBlockState(state));
        if (uuid != null) nbt.putUuid("uuid", uuid);
        nbt.putInt("ticks", delayTicks);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (getWorld() != null) {
            state = NbtHelper.toBlockState(getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("block_state"));
        }
        uuid = nbt.containsUuid("uuid") ? nbt.getUuid("uuid") : null;
        delayTicks = nbt.getInt("ticks");
    }

    @Override
    public @org.jetbrains.annotations.Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public void spawnCube(boolean overwrite) {
        if (getWorld() == null || getWorld().isClient) return;
        ServerWorld world = (ServerWorld) getWorld();
        if (uuid != null) {
            if (!overwrite) return;
            if (world.getEntity(uuid) != null) world.getEntity(uuid).kill();
        }
        WeightedCube cube = WeightedCube.entityType.create(getWorld());
        cube.setPosition(getPos().toCenterPos().subtract(0, cube.getBoundingBox().getLengthY()/2, 0));
        cube.getDataTracker().set(WeightedCube.BLOCK, state);
        getWorld().spawnEntity(cube);
        uuid = cube.getUuid();
        delayTicks = dispenserDelayTicks;
    }
}
