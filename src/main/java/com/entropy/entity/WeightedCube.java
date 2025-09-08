package com.entropy.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Arm;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Collections;

import static com.entropy.GatewayGunConstants.cubeKills;
import static com.entropy.GatewayGunConstants.weightedCubeSize;

public class WeightedCube extends LivingEntity {
    public static final String BLOCK_KEY = "block_state";
    public static final TrackedData<BlockState> BLOCK = DataTracker.registerData(WeightedCube.class, TrackedDataHandlerRegistry.BLOCK_STATE);

    public static final EntityType<WeightedCube> entityType = EntityType.Builder.create(WeightedCube::new, SpawnGroup.MISC).dimensions(weightedCubeSize, weightedCubeSize).build("weighted_cube");

    public WeightedCube(EntityType<WeightedCube> entityEntityType, World world) {
        super(entityEntityType, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(BLOCK, Blocks.DIRT.getDefaultState());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        dataTracker.set(BLOCK, NbtHelper.toBlockState(getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound(BLOCK_KEY)));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.put(BLOCK_KEY, NbtHelper.fromBlockState(dataTracker.get(BLOCK)));
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Collections::emptyIterator;
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {

    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public void kill() {
        this.remove(RemovalReason.KILLED);
        this.emitGameEvent(GameEvent.ENTITY_DIE);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        for (String s : cubeKills) {
            if (s.equals(source.getName())) {
                kill();
            }
        }
        return false;
    }
}
