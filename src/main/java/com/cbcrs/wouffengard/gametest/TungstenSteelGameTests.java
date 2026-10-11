package com.cbcrs.wouffengard.gametest;

import com.cbcrs.wouffengard.TheRoarOfSteel;
import com.cbcrs.wouffengard.index.CBSTungstenSteel;
import com.cbcrs.wouffengard.index.CBSTungstenSteelFluids;
import com.cbcrs.wouffengard.index.CBSItems;
import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import rbasamoyai.createbigcannons.crafting.casting.FluidCastingTimeHandler;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Game tests for the tungsten steel / tungsten alloy materials and the molten
 * tungsten steel fluid.
 *
 * <p>These cover registration and data wiring only. Recipe behaviour is
 * deliberately untested here because the recipes are written in a later step.
 */
@GameTestHolder(TheRoarOfSteel.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TungstenSteelGameTests {

    private TungstenSteelGameTests() {
    }

    /**
     * The three solids exist under their exact registry ids.
     *
     * <p>Asserting the id as well as the instance catches the case where an item
     * is registered but under a name the recipes and lang files do not use.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void tungstenSteelMaterialsAreRegistered(GameTestHelper helper) {
        Map<String, Item> expected = new LinkedHashMap<>();
        expected.put("tungsten_steel_ingot", CBSTungstenSteel.TUNGSTEN_STEEL_INGOT.get());
        expected.put("tungsten_alloy_billet", CBSTungstenSteel.TUNGSTEN_ALLOY_BILLET.get());
        expected.put("tungsten_alloy_ingot", CBSTungstenSteel.TUNGSTEN_ALLOY_INGOT.get());

        for (Map.Entry<String, Item> e : expected.entrySet()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    TheRoarOfSteel.NAMESPACE, e.getKey());
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                helper.fail("Item not registered: " + id);
                return;
            }
            if (BuiltInRegistries.ITEM.get(id) != e.getValue()) {
                helper.fail("Registry id " + id + " resolves to a different instance");
                return;
            }
        }
        helper.succeed();
    }

    /**
     * Each new solid is usable as an item stack, i.e. it is a real registered
     * item and not an entry that points at a null holder.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void newMaterialsStackProperly(GameTestHelper helper) {
        Item[] items = {
                CBSTungstenSteel.TUNGSTEN_STEEL_INGOT.get(),
                CBSTungstenSteel.TUNGSTEN_ALLOY_BILLET.get(),
                CBSTungstenSteel.TUNGSTEN_ALLOY_INGOT.get(),
        };
        for (Item item : items) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                helper.fail("Item produced an empty stack: "
                        + BuiltInRegistries.ITEM.getKey(item));
                return;
            }
            if (stack.getMaxStackSize() != 64) {
                helper.fail("Unexpected max stack size " + stack.getMaxStackSize()
                        + " for " + BuiltInRegistries.ITEM.getKey(item));
                return;
            }
        }
        helper.succeed();
    }

    /**
     * The molten fluid exists as a source and a flowing fluid under the expected
     * ids, and both are the same fluid type.
     *
     * <p>This is the part that silently fails if {@code standardFluid} is wired
     * wrongly: the source registers, the flowing variant does not, and the fluid
     * simply will not flow or will not be found by Create's pipes.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void moltenTungstenSteelFluidIsRegistered(GameTestHelper helper) {
        ResourceLocation sourceId = ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "molten_tungsten_steel");
        ResourceLocation flowingId = ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "flowing_molten_tungsten_steel");

        if (!BuiltInRegistries.FLUID.containsKey(sourceId)) {
            helper.fail("Source fluid not registered: " + sourceId);
            return;
        }
        if (!BuiltInRegistries.FLUID.containsKey(flowingId)) {
            helper.fail("Flowing fluid not registered: " + flowingId);
            return;
        }

        Fluid source = BuiltInRegistries.FLUID.get(sourceId);
        Fluid flowing = BuiltInRegistries.FLUID.get(flowingId);

        if (!(source instanceof BaseFlowingFluid.Source)) {
            helper.fail("Source fluid is not a Source: " + source.getClass().getName());
            return;
        }
        if (!(flowing instanceof BaseFlowingFluid.Flowing)) {
            helper.fail("Flowing fluid is not a Flowing: " + flowing.getClass().getName());
            return;
        }
        if (source.getFluidType() != flowing.getFluidType()) {
            helper.fail("Source and flowing fluid do not share one FluidType");
            return;
        }
        if (source.getFluidType() == null) {
            helper.fail("Fluid has no FluidType");
            return;
        }
        helper.succeed();
    }

    /**
     * The casting time is wired in, not left at the 1000-tick default.
     *
     * <p>{@code FluidCastingTimeHandler} reads
     * {@code data/<ns>/fluid_casting_time/**.json}. If that file is missing or
     * named wrongly the lookup silently falls back to 1000, which would make
     * casting produce parts instantly and be easy to miss by hand.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void moltenTungstenSteelCastingTimeIsConfigured(GameTestHelper helper) {
        Fluid source = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "molten_tungsten_steel"));

        int castingTime = FluidCastingTimeHandler.getCastingTime(source);
        if (castingTime == 1000) {
            helper.fail("Casting time is the 1000-tick default - the "
                    + "fluid_casting_time data file is missing or not loaded");
            return;
        }
        if (castingTime != 3600) {
            helper.fail("Expected a casting time of 3600 ticks but got " + castingTime);
            return;
        }
        helper.succeed();
    }

    /**
     * The molten fluid can actually be placed in the world as a liquid block.
     *
     * <p>Without the block the fluid can be held in a tank but cannot be poured,
     * which breaks the foundry casting step this fluid exists for. The check asks
     * the FluidType whether it may place at a real position, rather than looking
     * up a block id, so it exercises the same path the game uses.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void moltenTungstenSteelCanBePlacedInWorld(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "molten_tungsten_steel");
        Fluid source = BuiltInRegistries.FLUID.get(id);

        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        if (!source.getFluidType().canBePlacedInLevel(
                helper.getLevel(), pos, source.defaultFluidState())) {
            helper.fail("FluidType refuses to place the molten fluid in the level");
            return;
        }
        if (source.defaultFluidState().createLegacyBlock().isAir()) {
            helper.fail("Fluid has no liquid block (legacy block is air)");
            return;
        }
        helper.succeed();
    }

    /**
     * The bucket item exists and is wired to this fluid.
     *
     * <p>Two distinct things can go wrong and both look like "no bucket in game":
     * the item may not be registered at all, or it may be a bucket for the wrong
     * fluid. Asserting the contained fluid catches the second case, which is the
     * one that silently picks up an empty/none fluid.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void moltenTungstenSteelBucketIsRegistered(GameTestHelper helper) {
        ResourceLocation bucketId = ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "molten_tungsten_steel_bucket");

        if (!BuiltInRegistries.ITEM.containsKey(bucketId)) {
            helper.fail("Bucket item not registered: " + bucketId);
            return;
        }

        Fluid source = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "molten_tungsten_steel"));

        // The builder's getBucket() must agree with the registry.
        Item fromEntry = CBSTungstenSteelFluids.MOLTEN_TUNGSTEN_STEEL.getBucket()
                .orElse(null);
        if (fromEntry == null) {
            helper.fail("Fluid entry reports no bucket even though one is registered");
            return;
        }
        if (fromEntry != BuiltInRegistries.ITEM.get(bucketId)) {
            helper.fail("Registered bucket differs from the one the FluidEntry reports");
            return;
        }

        if (!(fromEntry instanceof net.minecraft.world.item.BucketItem bucket)) {
            helper.fail("Bucket item is not a BucketItem: " + fromEntry.getClass().getName());
            return;
        }
        if (bucket.content != source) {
            helper.fail("Bucket is not filled with molten_tungsten_steel");
            return;
        }
        // Molten metal buckets are single-use and leave an empty bucket behind.
        if (new ItemStack(fromEntry).getMaxStackSize() != 1) {
            helper.fail("Bucket should stack to 1 but stacks to "
                    + new ItemStack(fromEntry).getMaxStackSize());
            return;
        }
        helper.succeed();
    }

    /**
     * Every item this mod registers must appear in the mod's creative tab.
     *
     * <p>This exists because the tab is built from an explicit list in
     * {@code CBSTabs.addItems()}, so registering an item and forgetting to list it
     * produces an item that works but cannot be found in the creative menu. That
     * actually happened with the molten tungsten steel bucket.
     *
     * <p>The check is written as "everything except these known exceptions",
     * rather than a hard-coded list of expected items, so a future item is caught
     * automatically instead of requiring this test to be updated too.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 200)
    public static void everyItemAppearsInTheCreativeTab(GameTestHelper helper) {
        // CreativeModeTab.getDisplayItems() is empty in the test server (tabs are
        // baked lazily, for the client), so instead drive the tab's own item
        // provider and inspect what it hands over. This is the same method the tab
        // builder uses at runtime.
        java.util.List<ItemStack> collected = new java.util.ArrayList<>();
        CreativeModeTab.Output collecting = new CreativeModeTab.Output() {
            @Override
            public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
                collected.add(stack);
            }

            @Override
            public void accept(ItemStack stack) {
                collected.add(stack);
            }
        };

        CreativeModeTab.ItemDisplayParameters params =
                new CreativeModeTab.ItemDisplayParameters(
                        net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS, false,
                        helper.getLevel().registryAccess());

        try {
            com.cbcrs.wouffengard.index.CBSTabs.addItems(params, collecting);
        } catch (RuntimeException e) {
            helper.fail("CBSTabs.addItems() threw: " + e);
            return;
        }

        Set<String> inTab = new LinkedHashSet<>();
        for (ItemStack stack : collected) {
            if (!stack.isEmpty()) {
                inTab.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
            }
        }

        // Items intentionally kept out of the tab: the molten fluid's liquid
        // block and its flowing counterpart, matching CBC's molten metals.
        Set<String> excluded = Set.of(
                "molten_tungsten_steel",
                "flowing_molten_tungsten_steel");

        Set<String> missing = new LinkedHashSet<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if (!TheRoarOfSteel.NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            String path = id.getPath();
            if (excluded.contains(path) || inTab.contains(path)) {
                continue;
            }
            missing.add(path);
        }

        if (!missing.isEmpty()) {
            helper.fail("Registered items missing from the creative tab " + missing
                    + " - add them to CBSTabs.addItems() or to the exclusion list");
            return;
        }
        if (!inTab.contains("molten_tungsten_steel_bucket")) {
            helper.fail("molten_tungsten_steel_bucket is not added to the creative tab");
            return;
        }
        helper.succeed();
    }

    /**
     * The tungsten steel ingot is pressed from a tungsten ingot and a steel ingot
     * under SUPERHEATED heat (design item 27).
     *
     * <p>Two compacting recipes produce this item - one casts it from molten metal
     * and this one presses it - so the heat condition is what distinguishes them
     * and is asserted explicitly. Create's HeatCondition has only NONE / HEATED /
     * SUPERHEATED, and SUPERHEATED is the one that maps to a SEETHING blaze burner.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void tungstenSteelIngotIsPressedSuperheated(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "compacting/tungsten_steel_ingot_from_pressing");
        var holder = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        if (holder == null) {
            helper.fail("Recipe not loaded: " + id);
            return;
        }
        if (!(holder.value() instanceof CompactingRecipe compacting)) {
            helper.fail("Recipe is not a create:compacting recipe");
            return;
        }
        if (compacting.getRequiredHeat() != HeatCondition.SUPERHEATED) {
            helper.fail("Expected SUPERHEATED (a SEETHING blaze burner) but the recipe "
                    + "requires " + compacting.getRequiredHeat());
            return;
        }
        if (compacting.getFluidIngredients().size() != 0) {
            helper.fail("The pressing recipe should take no fluid, but it takes "
                    + compacting.getFluidIngredients().size());
            return;
        }
        if (compacting.getIngredients().size() != 2) {
            helper.fail("Expected 2 item ingredients (tungsten + steel) but found "
                    + compacting.getIngredients().size());
            return;
        }
        // The tungsten ingot must be accepted; the steel ingot comes from whichever
        // mod provides the conventional c:ingots/steel tag.
        if (!compacting.getIngredients().get(0).test(new ItemStack(CBSItems.TUNGSTEN_INGOT.get()))
                && !compacting.getIngredients().get(1).test(
                        new ItemStack(CBSItems.TUNGSTEN_INGOT.get()))) {
            helper.fail("Neither ingredient accepts the tungsten ingot");
            return;
        }
        var results = compacting.getRollableResults();
        if (results.isEmpty()
                || results.get(0).getStack().getItem() != CBSTungstenSteel.TUNGSTEN_STEEL_INGOT.get()) {
            helper.fail("Recipe does not produce tungsten_steel_ingot");
            return;
        }
        helper.succeed();
    }

    /**
     * Melting a tungsten steel ingot yields one ingot's worth of molten metal.
     *
     * <p>90 mB keeps the melting and casting recipes symmetric, so an ingot can be
     * melted and re-cast without gain or loss.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void tungstenSteelIngotMeltsBackToMoltenMetal(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "melting/molten_tungsten_steel_from_ingot");
        var holder = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        if (holder == null) {
            helper.fail("Recipe not loaded: " + id);
            return;
        }
        if (!(holder.value() instanceof rbasamoyai.createbigcannons.crafting.foundry.MeltingRecipe melting)) {
            helper.fail("Recipe is not a createbigcannons:melting recipe: "
                    + holder.value().getClass().getName());
            return;
        }
        if (melting.getRequiredHeat() != HeatCondition.HEATED) {
            helper.fail("Expected HEATED but the recipe requires " + melting.getRequiredHeat());
            return;
        }
        if (melting.getFluidResults().isEmpty()) {
            helper.fail("Melting recipe produces no fluid");
            return;
        }
        var produced = melting.getFluidResults().get(0);
        Fluid molten = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "molten_tungsten_steel"));
        if (produced.getFluid() != molten) {
            helper.fail("Melting produces " + produced.getFluid() + " instead of molten_tungsten_steel");
            return;
        }
        if (produced.getAmount() != 90) {
            helper.fail("Expected 90 mB (one ingot) but melting yields " + produced.getAmount());
            return;
        }
        if (melting.getIngredients().size() != 1
                || !melting.getIngredients().get(0).test(
                        new ItemStack(CBSTungstenSteel.TUNGSTEN_STEEL_INGOT.get()))) {
            helper.fail("Melting recipe does not accept the tungsten steel ingot");
            return;
        }
        helper.succeed();
    }

    /**
     * The tungsten alloy ingot is sintered from the billet.
     *
     * <p>Expressed as a vanilla blasting recipe because Create's encased fan
     * applies vanilla blasting/smoking; the fan only implements splashing and
     * haunting itself. So this one recipe serves both a blast furnace and a fan.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void tungstenAlloyIngotIsSinteredFromBillet(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "blasting/tungsten_alloy_ingot_from_sintering");
        var holder = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        if (holder == null) {
            helper.fail("Recipe not loaded: " + id);
            return;
        }
        if (!(holder.value() instanceof net.minecraft.world.item.crafting.BlastingRecipe blasting)) {
            helper.fail("Recipe is not a minecraft:blasting recipe: "
                    + holder.value().getClass().getName());
            return;
        }
        if (blasting.getCookingTime() != 200) {
            helper.fail("Expected a cooking time of 200 ticks but found "
                    + blasting.getCookingTime());
            return;
        }
        if (blasting.getIngredients().size() != 1
                || !blasting.getIngredients().get(0).test(
                        new ItemStack(CBSTungstenSteel.TUNGSTEN_ALLOY_BILLET.get()))) {
            helper.fail("Sintering recipe does not accept the tungsten alloy billet");
            return;
        }
        var result = blasting.getResultItem(helper.getLevel().registryAccess());
        if (result.getItem() != CBSTungstenSteel.TUNGSTEN_ALLOY_INGOT.get()) {
            helper.fail("Sintering produces " + BuiltInRegistries.ITEM.getKey(result.getItem())
                    + " instead of tungsten_alloy_ingot");
            return;
        }
        helper.succeed();
    }
}
