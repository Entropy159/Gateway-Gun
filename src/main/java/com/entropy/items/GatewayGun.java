package com.entropy.items;

import com.entropy.CoreData;
import com.entropy.GatewayGunMod;
import com.entropy.GatewayRecord;
import com.entropy.client.renderer.GatewayGunRenderer;
import com.entropy.entities.Gateway;
import com.entropy.misc.GatewayGunUtils;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.portal.PortalUtils;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.AARotation;
import qouteall.q_misc_util.my_util.IntBox;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GatewayGun extends Item implements GeoItem {
    public static final int COOLDOWN_TICKS = 4;

    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    private static final RawAnimation SHOOT_ANIM = RawAnimation.begin().thenPlay("gateway_shoot");

    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    public GatewayGun() {
        super(new FabricItemSettings().fireproof().maxCount(1).rarity(Rarity.EPIC));

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // Utilise our own render hook to define our custom renderer
    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private final GatewayGunRenderer renderer = new GatewayGunRenderer();

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }

    // Register our animation controllers
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "gatewayGunController", 1, state -> PlayState.CONTINUE)
                        .triggerableAnim("shoot_anim", SHOOT_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean canMine(@NotNull BlockState state, @NotNull World world, @NotNull BlockPos pos, @NotNull PlayerEntity miner) {
        return false;
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(World world, PlayerEntity player, @NotNull Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        CoreData data = CoreData.fromTag(itemStack.getOrCreateNbt(), false);
        if (world.isClient || data.restrictSide != null || !data.hasCore) {
            return TypedActionResult.fail(itemStack);
        }

        boolean success = interact(
                (ServerPlayerEntity) player, hand,
                GatewayRecord.GatewaySide.TWO
        );

        if (success) {
            player.incrementStat(Stats.USED.getOrCreateStat(this));
        }
        return TypedActionResult.fail(itemStack);
    }

    @Override
    public void appendTooltip(
            @NotNull ItemStack stack, @Nullable World world,
            @NotNull List<Text> tooltip, @NotNull TooltipContext context
    ) {
        super.appendTooltip(stack, world, tooltip, context);

        tooltip.add(Text.translatable("item.gatewaygun.gatewaygun_desc").formatted(Formatting.GOLD));
        CoreData.fromTag(stack.getOrCreateNbt(), false).setTooltip(tooltip);
    }

    public void onAttack(
            PlayerEntity player, World world, Hand hand
    ) {
        if (world.isClient) {
            return;
        }

        CoreData data = CoreData.fromTag(player.getStackInHand(hand).getOrCreateNbt(), false);

        if (!data.hasCore) {
            return;
        }

        GatewayRecord.GatewaySide side = data.restrictSide;
        if (side == null) {
            side = GatewayRecord.GatewaySide.ONE;
        }

        boolean success = interact(
                (ServerPlayerEntity) player, hand,
                side
        );

        if (success) {
            player.incrementStat(Stats.USED.getOrCreateStat(this));
        }
    }

    // return whether successful
    public boolean interact(
            ServerPlayerEntity player,
            Hand hand,
            GatewayRecord.GatewaySide side
    ) {
        ItemStack itemStack = player.getStackInHand(hand);
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        PortalUtils.PortalAwareRaytraceResult raytraceResult = PortalUtils.portalAwareRayTrace(player, 100);

        if (raytraceResult == null) {
            return false;
        }

        BlockHitResult blockHit = raytraceResult.hitResult();
        ServerWorld world = ((ServerWorld) raytraceResult.world());
        Direction wallFacing = blockHit.getSide();

        if (!checkAction(player, world)) {
            return false;
        }

        Validate.isTrue(blockHit.getType() == HitResult.Type.BLOCK);

        player.getWorld().playSound(
                null,
                player.getEyePos().x, player.getEyePos().y, player.getEyePos().z,
                side == GatewayRecord.GatewaySide.ONE ?
                        GatewayGunMod.GATEWAY1_SHOOT_EVENT : GatewayGunMod.GATEWAY2_SHOOT_EVENT,
                SoundCategory.PLAYERS,
                1.0F, 1.0F
        );

        NbtCompound tag = itemStack.getOrCreateNbt();
        CoreData data = CoreData.fromTag(tag, false);

        GatewayRecord.GatewayID id = new GatewayRecord.GatewayID(data.code, side);

        BiPredicate<World, BlockPos> wallPredicate = data.allowedBlocks.getWallPredicate();

        PortalPlacement placement = findPortalPlacement(
                player, raytraceResult, id, wallPredicate, data.width, data.height
        );

        if (placement == null) {
            return false;
        }

        Direction rightDir = placement.rotation.transformedX;
        Direction upDir = placement.rotation.transformedY;

        triggerAnim(
                player,
                GeoItem.getOrAssignId(player.getStackInHand(hand), ((ServerWorld) player.getWorld())),
                "gatewayGunController", "shoot_anim"
        );

        Vec3d wallFacingVec = Vec3d.of(wallFacing.getVector());
        Vec3d newOrigin = Helper
                .getBoxSurface(placement.areaBox.toRealNumberBox(), wallFacing.getOpposite())
                .getCenter()
                .add(wallFacingVec.multiply(GatewayGunMod.gatewayOffset));

        GatewayGunUtils.placeGateway(world, newOrigin, Vec3d.of(upDir.getVector()), Vec3d.of(rightDir.getVector()), data, side, placement.areaBox, placement.wallBox, true);

        return true;
    }

    private static boolean checkAction(ServerPlayerEntity player, ServerWorld world) {
        if (world.getRegistryKey() == World.END) {
            EnderDragonFight endDragonFight = world.getEnderDragonFight();
            if (endDragonFight != null) {
                if (!endDragonFight.hasPreviouslyKilled()) {
                    player.sendMessage(
                            Text.literal("Using gateway gun in end before killing any ender dragon is not allowed"),
                            true
                    );
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull World level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClient) {
            if (!stack.hasNbt()) {
                stack.setNbt(new CoreData(false).toTag());
            }
            CoreData data = CoreData.fromTag(stack.getOrCreateNbt(), false);
            if (data.code <= 0) {
                data.code = Random.create().nextInt(Integer.MAX_VALUE);
                stack.setNbt(data.toTag());
            }
            ServerWorld world = (ServerWorld) level;
            if (!isSelected) {
                data.grabbedEntityId = null;
                stack.setNbt(data.toTag());
            }
            if (data.grabbedEntityId != null) {
                Entity grabbed = world.getEntity(data.grabbedEntityId);
                if (grabbed == null) {
                    data.grabbedEntityId = null;
                    stack.setNbt(data.toTag());
                    world.playSound(null, entity.getEyePos().x, entity.getEyePos().y, entity.getEyePos().z, GatewayGunMod.GRAB_STOP_EVENT, SoundCategory.PLAYERS, 1, 1);
                } else {
                    grabbed.fallDistance = 0;
                    Vec3d pos = entity.getEyePos().add(entity.getRotationVec(1).x * GatewayGunMod.grabDistance, entity.getRotationVec(1).y * GatewayGunMod.grabDistance, entity.getRotationVec(1).z * GatewayGunMod.grabDistance);
                    pos = GatewayGunUtils.portalRaytrace(world, entity.getEyePos(), pos).pos();
                    grabbed.setVelocity(pos.subtract(grabbed.getPos()).subtract(new Vec3d(0, grabbed.getBoundingBox().getLengthY() / 2, 0)));
                }
            } else {
                if (entity instanceof ServerPlayerEntity player) {
                    player.networkHandler.sendPacket(new StopSoundS2CPacket(GatewayGunMod.GRAB_LOOP, SoundCategory.PLAYERS));
                }
            }
        }
    }

    @Nullable
    private static PortalPlacement findPortalPlacement(
            ServerPlayerEntity player,
            PortalUtils.PortalAwareRaytraceResult raytraceResult,
            GatewayRecord.GatewayID descriptor,
            BiPredicate<World, BlockPos> wallPredicate,
            int width, int height
    ) {
        BlockHitResult blockHit = raytraceResult.hitResult();
        ServerWorld world = ((ServerWorld) raytraceResult.world());
        BlockPos interactingAirPos = blockHit.getBlockPos().offset(blockHit.getSide());
        Direction wallFacing = blockHit.getSide();

        Direction playerGravity = GravityChangerInterface.invoker.getGravityDirection(player);
        Direction effectiveGravity = raytraceResult.portalsPassingThrough().stream().reduce(
                playerGravity,
                (gravity, portal) -> portal.getTeleportedGravityDirection(gravity),
                (g1, g2) -> {
                    throw new RuntimeException();
                }
        );
        Vec3d viewVector = player.getRotationVector();
        Vec3d transformedViewVector = raytraceResult.portalsPassingThrough().stream().reduce(
                viewVector,
                (v, portal) -> portal.transformLocalVec(v),
                (v1, v2) -> {
                    throw new RuntimeException();
                }
        );

        Vec3d localViewVector = GravityChangerInterface.invoker
                .transformWorldToPlayer(effectiveGravity, transformedViewVector);

        Direction localWallFacing = GravityChangerInterface.invoker
                .transformDirWorldToPlayer(effectiveGravity, wallFacing);

        Direction[] localUpDirCandidates =
                Helper.getAnotherFourDirections(localWallFacing.getAxis());

        Arrays.sort(localUpDirCandidates, Comparator.comparingDouble((Direction dir) -> {
            if (dir == Direction.UP) {
                // the local up direction has the highest priority
                return 1;
            }
            // horizontal dot product
            return dir.getVector().getX() * localViewVector.x + dir.getVector().getZ() * localViewVector.z;
        }).reversed());

        BlockPos portalAreaSize = new BlockPos(width, height, 1);

        for (Direction localUpDir : localUpDirCandidates) {
            Direction upDir = GravityChangerInterface.invoker
                    .transformDirPlayerToWorld(effectiveGravity, localUpDir);

            AARotation rot = AARotation.getAARotationFromYZ(upDir, wallFacing);
            BlockPos transformedSize = rot.transform(portalAreaSize);
            IntBox portalArea = IntBox.getBoxByPosAndSignedSize(interactingAirPos, transformedSize);
            IntBox wallArea = portalArea.getMoved(wallFacing.getOpposite().getVector());

            if (GatewayGunMod.isAreaClear(world, portalArea) &&
                    wallArea.fastStream().allMatch(p -> wallPredicate.test(world, p)) &&
                    !otherGatewayExistsInArea(world, wallArea, wallFacing, descriptor)
            ) {
                return new PortalPlacement(rot, portalArea, wallArea);
            }
        }

        return null;
    }

    private static boolean otherGatewayExistsInArea(
            World world, IntBox wallArea, Direction wallFacing,
            GatewayRecord.GatewayID id
    ) {
        List<Gateway> gateways = McHelper.findEntitiesByBox(
                Gateway.class,
                world,
                wallArea.toRealNumberBox().expand(0.1),
                IPGlobal.maxNormalPortalRadius,
                p -> p.getApproximateFacingDirection() == wallFacing
                        && IntBox.getIntersect(p.wallBox, wallArea) != null
                        && !Objects.equals(p.id, id)
        );
        return !gateways.isEmpty();
    }

    public record PortalPlacement(AARotation rotation, IntBox areaBox, IntBox wallBox) {
    }
}
