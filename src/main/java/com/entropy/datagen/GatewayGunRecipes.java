package com.entropy.datagen;

import com.entropy.GatewayGunMod;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;

public class GatewayGunRecipes extends FabricRecipeProvider {
    public GatewayGunRecipes(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        ShapelessRecipeJsonBuilder.create(RecipeCategory.REDSTONE, GatewayGunMod.GATEGRID).input(Items.QUARTZ_BLOCK).input(Items.REDSTONE).criterion(FabricRecipeProvider.hasItem(Items.QUARTZ_BLOCK), FabricRecipeProvider.conditionsFromItem(Items.QUARTZ_BLOCK)).offerTo(exporter);
        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, GatewayGunMod.GATEWAY_GUN).pattern(" G ").pattern("QRQ").pattern(" B ").input('G', Items.GLASS_PANE).input('Q', Items.QUARTZ).input('R', Items.REDSTONE).input('B', Items.GILDED_BLACKSTONE).criterion(FabricRecipeProvider.hasItem(Items.QUARTZ), FabricRecipeProvider.conditionsFromItem(Items.QUARTZ)).offerTo(exporter);
        ShapedRecipeJsonBuilder.create(RecipeCategory.TOOLS, GatewayGunMod.GATEWAY_CORE).pattern("QRQ").pattern("RNR").pattern("QRQ").input('N', Items.NETHER_STAR).input('Q', Items.QUARTZ).input('R', Items.REDSTONE).criterion(FabricRecipeProvider.hasItem(Items.NETHER_STAR), FabricRecipeProvider.conditionsFromItem(Items.NETHER_STAR)).offerTo(exporter);
    }
}