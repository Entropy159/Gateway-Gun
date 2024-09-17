package com.entropy.items;

import com.entropy.CoreData;
import com.entropy.client.renderer.GatewayCoreRenderer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GatewayCore extends Item implements GeoItem {

    public final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, true);

    private static final RawAnimation SPIN_ANIM = RawAnimation.begin().thenLoop("idle");

    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    public GatewayCore() {
        super(new FabricItemSettings().fireproof().maxCount(1).rarity(Rarity.EPIC));

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private final GatewayCoreRenderer renderer = new GatewayCoreRenderer();

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

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "gatewayCoreController", 1, state -> state.setAndContinue(SPIN_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, World level, @NotNull List<Text> tooltip, @NotNull TooltipContext isAdvanced) {
        super.appendTooltip(stack, level, tooltip, isAdvanced);

        CoreData.fromTag(stack.getOrCreateNbt(), true).setTooltip(tooltip);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (!world.isClient) {
            if (!stack.hasNbt()) {
                stack.setNbt(new CoreData(true).toTag());
            }
            CoreData data = CoreData.fromTag(stack.getOrCreateNbt(), true);
            if (data.code <= 0) {
                data.code = Random.create().nextInt(Integer.MAX_VALUE);
                stack.setNbt(data.toTag());
            }
        }
    }
}
