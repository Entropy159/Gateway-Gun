package com.entropy.items;

import com.entropy.CoreData;
import com.entropy.client.renderer.GatewayCoreRenderer;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

import static com.entropy.items.GatewayGunComponents.GATEWAY_DATA;

public class GatewayCore extends Item implements GeoItem {

    public final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, true);

    private static final RawAnimation SPIN_ANIM = RawAnimation.begin().thenLoop("idle");

    public GatewayCore() {
        super(new Item.Settings().fireproof().maxCount(1).rarity(Rarity.EPIC).component(GATEWAY_DATA, new CoreData(true)));

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GatewayCoreRenderer renderer;

            @Override
            public @NotNull BuiltinModelItemRenderer getGeoItemRenderer() {
                if (renderer == null) {
                    renderer = new GatewayCoreRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "gatewayCoreController", 1, state -> state.setAndContinue(SPIN_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        CoreData.get(stack, true).setTooltip(tooltip);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (!world.isClient) {
            if (!stack.contains(GATEWAY_DATA)) {
                stack.set(GATEWAY_DATA, new CoreData(true));
            }
            CoreData data = CoreData.get(stack, true);
            if (data.code() <= 0) {
                data = data.withCode(Random.create().nextInt(Integer.MAX_VALUE));
                stack.set(GATEWAY_DATA, data);
            }
        }
    }
}
