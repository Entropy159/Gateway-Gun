package com.entropy.blocks.quantumfield;

import com.entropy.entity.GatewayGunBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class QuantumFieldBlockEntity extends BlockEntity {
    public QuantumFieldBlockEntity(BlockPos pos, BlockState state) {
        super(GatewayGunBlockEntities.QUANTUM_FIELD, pos, state);
    }
}
