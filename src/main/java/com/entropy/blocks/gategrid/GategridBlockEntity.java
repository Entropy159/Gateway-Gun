package com.entropy.blocks.gategrid;

import com.entropy.CoreData;
import com.entropy.entity.GatewayGunBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class GategridBlockEntity extends BlockEntity {
    public CoreData data = new CoreData(false);

    public GategridBlockEntity(BlockPos pos, BlockState state){
        super(GatewayGunBlockEntities.GATEGRID, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("coreData", data.toTag());
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        data = CoreData.fromTag(nbt.getCompound("coreData"), false);
    }
}
