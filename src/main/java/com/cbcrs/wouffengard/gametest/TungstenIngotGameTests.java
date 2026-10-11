package com.cbcrs.wouffengard.gametest;

import com.cbcrs.wouffengard.TheRoarOfSteel;
import com.cbcrs.wouffengard.index.CBSItems;
import com.cbcrs.wouffengard.index.CBSMaterials;
import com.cbcrs.wouffengard.index.CBSOres;
import com.cbcrs.wouffengard.index.CBSTungstenSteel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-game verification for the tungsten content.
 *
 * <p>These tests prove that every registered item and ore block actually exists
 * <em>inside a running game with Create / Create Big Cannons loaded</em> - not
 * merely that the classes compiled or that a texture file is on disk. They also
 * exercise the real ore loot tables and confirm the worldgen data is registered.
 *
 * <p>The template is a tiny structure at
 * {@code data/cbc_rs/structure/empty.nbt}; {@code @PrefixGameTestTemplate(false)}
 * keeps NeoForge from prefixing it with the mod id, and the namespace is supplied
 * explicitly.
 *
 * <p>The class is registered manually from {@link CBSGameTests}; see that class
 * for why the NeoForge event alone is not enough here.
 */
@GameTestHolder(TheRoarOfSteel.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TungstenIngotGameTests {

    private TungstenIngotGameTests() {
    }

    /** The one item that existed before the ore work, kept as a regression guard. */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void tungstenIngotIsRegistered(GameTestHelper helper) {
        if (!checkItem(helper, "tungsten_ingot", CBSItems.TUNGSTEN_INGOT.get())) {
            return;
        }
        helper.succeed();
    }

    /**
     * Every item added by decisions 15/16 must be registered under the mod's
     * namespace, be the same instance the code holds, and survive a registry
     * round-trip (which is what recipe and tag JSON rely on).
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void tungstenMaterialsAreRegistered(GameTestHelper helper) {
        Map<String, Item> expected = new LinkedHashMap<>();
        expected.put("tungsten_nugget", CBSMaterials.TUNGSTEN_NUGGET.get());
        expected.put("raw_scheelite", CBSMaterials.RAW_SCHEELITE.get());
        expected.put("raw_wolframite", CBSMaterials.RAW_WOLFRAMITE.get());
        expected.put("crushed_raw_scheelite", CBSMaterials.CRUSHED_RAW_SCHEELITE.get());
        expected.put("crushed_raw_wolframite", CBSMaterials.CRUSHED_RAW_WOLFRAMITE.get());

        for (Map.Entry<String, Item> e : expected.entrySet()) {
            if (!checkItem(helper, e.getKey(), e.getValue())) {
                return;
            }
        }
        helper.succeed();
    }

    /** Both ore blocks must be registered and must have a working block item. */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void tungstenOreBlocksAreRegistered(GameTestHelper helper) {
        Map<String, Block> expected = new LinkedHashMap<>();
        expected.put("scheelite_ore", CBSOres.SCHEELITE_ORE.get());
        expected.put("wolframite_ore", CBSOres.WOLFRAMITE_ORE.get());

        for (Map.Entry<String, Block> e : expected.entrySet()) {
            ResourceLocation id =
                    ResourceLocation.fromNamespaceAndPath(TheRoarOfSteel.NAMESPACE, e.getKey());
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                helper.fail("Block not registered: " + id);
                return;
            }
            if (BuiltInRegistries.BLOCK.get(id) != e.getValue()) {
                helper.fail("Registry entry " + id + " does not match the code reference");
                return;
            }
            Item blockItem = e.getValue().asItem();
            if (blockItem == Items.AIR) {
                helper.fail("Ore block has no block item: " + id);
                return;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(blockItem);
            if (!id.equals(itemId)) {
                helper.fail("Block item id mismatch for " + id + ": got " + itemId);
                return;
            }
        }
        helper.succeed();
    }

    /**
     * All tungsten content must produce a usable stack: this catches an item
     * registered with broken or missing Item.Properties.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void allTungstenItemsStackProperly(GameTestHelper helper) {
        Item[] all = {
                CBSItems.TUNGSTEN_INGOT.get(),
                CBSMaterials.TUNGSTEN_NUGGET.get(),
                CBSMaterials.RAW_SCHEELITE.get(),
                CBSMaterials.RAW_WOLFRAMITE.get(),
                CBSMaterials.CRUSHED_RAW_SCHEELITE.get(),
                CBSMaterials.CRUSHED_RAW_WOLFRAMITE.get(),
                CBSOres.SCHEELITE_ORE.get().asItem(),
                CBSOres.WOLFRAMITE_ORE.get().asItem(),
        };
        for (Item item : all) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                helper.fail("Empty stack for " + BuiltInRegistries.ITEM.getKey(item));
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
     * Guards the design rule from decision 16: exactly one tungsten ingot must
     * exist, so both ore chains converge instead of producing two metals.
     *
     * <p>The predicate is deliberately an exact id match, not a substring test.
     * A substring test on "tungsten" + "ingot" also matches the refined
     * materials (tungsten_steel_ingot, tungsten_alloy_ingot), which are
     * different substances and are supposed to exist. What must not exist is a
     * second plain tungsten ingot, e.g. a duplicate registered under another
     * name by a later phase.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void exactlyOneTungstenIngotExists(GameTestHelper helper) {
        // Any item whose id ends in "ingot" and mentions tungsten, but which is
        // NOT one of the two deliberately distinct refined grades.
        Set<String> permittedExtraIngots = Set.of(
                "tungsten_steel_ingot",   // decision 17 material chain
                "tungsten_alloy_ingot");  // decision 17 material chain

        Set<String> plainTungstenIngots = new java.util.LinkedHashSet<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            String path = id.getPath();
            if (!path.contains("tungsten") || !path.contains("ingot")) {
                continue;
            }
            if (permittedExtraIngots.contains(path)) {
                continue;
            }
            plainTungstenIngots.add(path);
        }

        if (plainTungstenIngots.size() != 1
                || !plainTungstenIngots.contains("tungsten_ingot")) {
            helper.fail("Expected exactly one plain tungsten ingot, found "
                    + plainTungstenIngots + " (design decision 16 forbids a second)");
            return;
        }
        helper.succeed();
    }

    // =================================================================
    // Ore drops - the rule: exactly 1 raw ore with no Fortune, scaling
    // with Fortune, and the block itself under Silk Touch.
    // =================================================================

    /** Without Silk Touch or Fortune each ore must drop exactly one raw ore. */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void oresDropExactlyOneRawOreWithoutFortune(GameTestHelper helper) {
        if (!checkNoFortune(helper, "scheelite_ore", CBSMaterials.RAW_SCHEELITE.get())) {
            return;
        }
        if (!checkNoFortune(helper, "wolframite_ore", CBSMaterials.RAW_WOLFRAMITE.get())) {
            return;
        }
        helper.succeed();
    }

    /** Silk Touch must drop the ore block itself, never the raw ore. */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void oresDropThemselvesWithSilkTouch(GameTestHelper helper) {
        if (!checkSilkTouch(helper, "scheelite_ore")) {
            return;
        }
        if (!checkSilkTouch(helper, "wolframite_ore")) {
            return;
        }
        helper.succeed();
    }

    /**
     * Fortune must scale the drop count, verified over a large sample.
     *
     * <p>Vanilla's {@code ore_drops} formula is
     * {@code total = count * (nextInt(level + 2) - 1 clamped at 0, plus 1)}, so a
     * single roll is meaningless: at Fortune II the legal outcomes are 1, 2 and 3.
     * The check therefore samples {@link #FORTUNE_SAMPLES} rolls and asserts
     * probabilistic facts that hold with overwhelming likelihood rather than
     * asserting a single value.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 400)
    public static void oresScaleWithFortune(GameTestHelper helper) {
        for (String orePath : new String[]{"scheelite_ore", "wolframite_ore"}) {
            int plain = maxOver(helper, orePath, bareTool(), FORTUNE_SAMPLES);
            if (plain != 1) {
                helper.fail(orePath + " without Fortune dropped up to " + plain
                        + ", but it must always be exactly 1");
                return;
            }

            // Fortune III permits 1..4. Over many samples the maximum must exceed
            // the plain maximum; with 1/5 chance of staying at 1 every roll, the
            // probability of never exceeding 1 across FORTUNE_SAMPLES rolls is
            // (1/5)^N, which is negligible for N = 200.
            int fortune = maxOver(helper, orePath, enchantedTool(helper, "fortune", 3),
                    FORTUNE_SAMPLES);
            if (fortune <= plain) {
                helper.fail(orePath + " never exceeded " + plain + " drops across "
                        + FORTUNE_SAMPLES + " Fortune III rolls (max was " + fortune
                        + "), which means Fortune is not being applied");
                return;
            }

            // And it must never drop less than the single base item.
            int min = minOver(helper, orePath, enchantedTool(helper, "fortune", 3),
                    FORTUNE_SAMPLES);
            if (min < 1) {
                helper.fail(orePath + " dropped " + min + " with Fortune III, below the base 1");
                return;
            }
        }
        helper.succeed();
    }

    /**
     * The configured and placed features must exist in the server's dynamic
     * registries. A malformed worldgen JSON would fail to load or be absent
     * here, so this is the meaningful check that generation is wired up.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void oreWorldGenIsRegistered(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        HolderLookup.RegistryLookup<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>>
                configured = registries.lookupOrThrow(Registries.CONFIGURED_FEATURE);
        for (String name : new String[]{"scheelite_ore", "wolframite_ore"}) {
            ResourceLocation id =
                    ResourceLocation.fromNamespaceAndPath(TheRoarOfSteel.NAMESPACE, name);
            if (configured.get(ResourceKey.create(Registries.CONFIGURED_FEATURE, id)).isEmpty()) {
                helper.fail("Configured feature not registered: " + id);
                return;
            }
        }

        HolderLookup.RegistryLookup<net.minecraft.world.level.levelgen.placement.PlacedFeature>
                placed = registries.lookupOrThrow(Registries.PLACED_FEATURE);
        for (String name : new String[]{"scheelite_ore_main", "scheelite_ore_buried",
                "wolframite_ore_main", "wolframite_ore_secondary"}) {
            ResourceLocation id =
                    ResourceLocation.fromNamespaceAndPath(TheRoarOfSteel.NAMESPACE, name);
            if (placed.get(ResourceKey.create(Registries.PLACED_FEATURE, id)).isEmpty()) {
                helper.fail("Placed feature not registered: " + id);
                return;
            }
        }
        helper.succeed();
    }

    /**
     * Every recipe shipped by the mod must be loaded and parse without error.
     *
     * <p>{@code RecipeManager#hadErrorsLoading} is the authoritative signal: a
     * malformed JSON is reported at load, not at datagen time. The expected total
     * is 16:
     * <ul>
     *   <li>6 smelting and 6 blasting: ore / raw / crushed for both ores</li>
     *   <li>4 crushing: both ores and both raw ores</li>
     *   <li>2 splashing: both crushed ores</li>
     * </ul>
     * The two 9-in-1 nugget crafting recipes are counted separately below.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void allRecipesLoadWithoutErrors(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        if (manager.hadErrorsLoading()) {
            helper.fail("RecipeManager reported errors while loading recipes");
            return;
        }

        int ours = 0;
        for (var holder : manager.getRecipes()) {
            if (TheRoarOfSteel.NAMESPACE.equals(holder.id().getNamespace())) {
                ours++;
            }
        }
        // 6 smelting + 6 blasting + 4 crushing + 2 splashing + 2 crafting = 20
        int expected = 25;
        if (ours != expected) {
            helper.fail("Expected " + expected + " recipes under "
                    + TheRoarOfSteel.NAMESPACE + " but found " + ours);
            return;
        }

        // Measured baseline: an unmodified instance loaded 3091 recipes, so every
        // one of ours is present when the total is 3091 + expected (3126 at 25).
        // A file that silently failed to parse would drop this number and be
        // caught here.
        int total = manager.getRecipes().size();
        if (total < 3091 + expected) {
            helper.fail("Total recipe count is " + total + ", expected at least "
                    + (3091 + expected) + " - some of our recipe files did not load");
            return;
        }

        // The nugget round-trip must exist under an exact id, so a rename or a
        // dropped file is caught rather than merely changing the count. The
        // subfolder is part of the id: data/cbc_rs/recipe/crafting/x.json has the
        // id cbc_rs:crafting/x.
        for (String path : new String[]{
                "crafting/tungsten_ingot_from_nuggets", "crafting/tungsten_nugget_from_ingot",
                "crushing/scheelite_ore", "crushing/wolframite_ore",
                "crushing/raw_scheelite", "crushing/raw_wolframite",
                "splashing/crushed_raw_scheelite", "splashing/crushed_raw_wolframite",
                "smelting/tungsten_ingot_from_raw_scheelite",
                "blasting/tungsten_ingot_from_crushed_wolframite",
                // 0.1.1: basin casting and heated alloying
                "compacting/tungsten_steel_ingot",
                "mixing/tungsten_alloy_billet",
                // melting, super-heated pressing, fan sintering
                "melting/molten_tungsten_steel_from_ingot",
                "compacting/tungsten_steel_ingot_from_pressing",
                "blasting/tungsten_alloy_ingot_from_sintering"}) {
            ResourceLocation id =
                    ResourceLocation.fromNamespaceAndPath(TheRoarOfSteel.NAMESPACE, path);
            if (manager.byKey(id).isEmpty()) {
                helper.fail("Recipe not loaded: " + id);
                return;
            }
        }
        helper.succeed();
    }

    /**
     * The nugget chain must actually convert: 9 nuggets craft an ingot and an
     * ingot crafts back into 9 nuggets.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void nuggetAndIngotConvertBothWays(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var level = helper.getLevel();

        ItemStack nugget = new ItemStack(CBSMaterials.TUNGSTEN_NUGGET.get());
        ItemStack ingot = new ItemStack(CBSItems.TUNGSTEN_INGOT.get());

        // 9 nuggets -> 1 ingot
        net.minecraft.world.item.crafting.CraftingInput grid =
                net.minecraft.world.item.crafting.CraftingInput.of(3, 3,
                        java.util.List.of(nugget, nugget, nugget, nugget, nugget, nugget,
                                nugget, nugget, nugget));
        var shaped = manager.getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, grid, level);
        if (shaped.isEmpty()) {
            helper.fail("9 tungsten nuggets do not craft anything");
            return;
        }
        ItemStack result = shaped.get().value().assemble(grid, level.registryAccess());
        if (result.getItem() != CBSItems.TUNGSTEN_INGOT.get() || result.getCount() != 1) {
            helper.fail("9 nuggets produced " + result.getCount() + "x"
                    + BuiltInRegistries.ITEM.getKey(result.getItem())
                    + " instead of 1 tungsten ingot");
            return;
        }

        // 1 ingot -> 9 nuggets
        net.minecraft.world.item.crafting.CraftingInput single =
                net.minecraft.world.item.crafting.CraftingInput.of(1, 1, java.util.List.of(ingot));
        var shapeless = manager.getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, single, level);
        if (shapeless.isEmpty()) {
            helper.fail("A tungsten ingot does not craft anything back");
            return;
        }
        ItemStack back = shapeless.get().value().assemble(single, level.registryAccess());
        if (back.getItem() != CBSMaterials.TUNGSTEN_NUGGET.get() || back.getCount() != 9) {
            helper.fail("1 ingot produced " + back.getCount() + "x"
                    + BuiltInRegistries.ITEM.getKey(back.getItem())
                    + " instead of 9 tungsten nuggets");
            return;
        }
        helper.succeed();
    }

    /**
     * Control: the test tool must be honoured, and our ore must scale the same
     * way vanilla iron ore does.
     *
     * <p>Both sides are sampled, because {@code ore_drops} is random: comparing a
     * single roll to a single roll fails by chance roughly half the time. The
     * assertion is that over {@link #FORTUNE_SAMPLES} rolls with Fortune III both
     * ores reach the same maximum, which for level 3 is 4.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 400)
    public static void fortuneToolAndVanillaControlBehave(GameTestHelper helper) {
        ItemStack tool = enchantedTool(helper, "fortune", 3);

        HolderLookup.RegistryLookup<Enchantment> enchants =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> fortune = enchants.getOrThrow(
                ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.withDefaultNamespace("fortune")));
        int level = EnchantmentHelper.getItemEnchantmentLevel(fortune, tool);
        if (level != 3) {
            helper.fail("enchantedTool produced fortune level " + level + " instead of 3");
            return;
        }

        int vanillaMax = 0;
        for (int i = 0; i < FORTUNE_SAMPLES; i++) {
            vanillaMax = Math.max(vanillaMax, totalCount(Block.getDrops(
                    net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState(),
                    helper.getLevel(), helper.absolutePos(BlockPos.ZERO), null,
                    helper.makeMockPlayer(GameType.SURVIVAL), tool)));
        }
        if (vanillaMax < 4) {
            helper.fail("vanilla iron_ore peaked at " + vanillaMax + " across "
                    + FORTUNE_SAMPLES + " Fortune III rolls instead of 4, so the test tool "
                    + "is not being honoured");
            return;
        }

        for (String orePath : new String[]{"scheelite_ore", "wolframite_ore"}) {
            int ours = maxOver(helper, orePath, tool, FORTUNE_SAMPLES);
            if (ours != vanillaMax) {
                helper.fail(orePath + " peaked at " + ours + " while vanilla iron_ore peaked at "
                        + vanillaMax + " with the same tool, so the ore loot tables differ");
                return;
            }
        }
        helper.succeed();
    }

    // =================================================================
    // Helpers
    // =================================================================

    /** Shared item assertion used by the tests above. */
    private static boolean checkItem(GameTestHelper helper, String path, Item expected) {
        ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(TheRoarOfSteel.NAMESPACE, path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            helper.fail("Item not registered: " + id);
            return false;
        }
        Item registered = BuiltInRegistries.ITEM.get(id);
        if (registered != expected) {
            helper.fail("Registry entry " + id + " does not match the code reference");
            return false;
        }
        if (!id.equals(BuiltInRegistries.ITEM.getKey(registered))) {
            helper.fail("Registry id mismatch: expected " + id
                    + " but got " + BuiltInRegistries.ITEM.getKey(registered));
            return false;
        }
        return true;
    }

    private static Block oreBlock(String path) {
        return BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath(TheRoarOfSteel.NAMESPACE, path));
    }

    private static ItemStack bareTool() {
        return new ItemStack(Items.NETHERITE_PICKAXE);
    }

    /**
     * Build a pickaxe carrying an enchantment at the given level.
     *
     * <p>{@code ItemStack#enchant} refuses to apply an enchantment the item does
     * not support, so it silently does nothing here and the loot function then
     * sees level 0 - which showed up as a confusing "dropped 1 but expected 2".
     * The enchantments component is therefore set directly.
     */
    private static ItemStack enchantedTool(GameTestHelper helper, String enchantPath, int level) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        HolderLookup.RegistryLookup<Enchantment> enchants =
                helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> enchantment = enchants.getOrThrow(
                ResourceKey.create(Registries.ENCHANTMENT,
                        ResourceLocation.withDefaultNamespace(enchantPath)));

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(enchantment, level);
        tool.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        return tool;
    }

    /** All drop stacks for the given ore with the given tool. */
    private static List<ItemStack> drops(GameTestHelper helper, String orePath, ItemStack tool) {
        Block block = oreBlock(orePath);
        return Block.getDrops(block.defaultBlockState(), helper.getLevel(),
                helper.absolutePos(BlockPos.ZERO), null,
                helper.makeMockPlayer(GameType.SURVIVAL), tool);
    }

    private static int totalCount(List<ItemStack> stacks) {
        int total = 0;
        for (ItemStack s : stacks) {
            total += s.getCount();
        }
        return total;
    }

    /** No Fortune: exactly one raw ore, and nothing else. */
    private static boolean checkNoFortune(GameTestHelper helper, String orePath, Item raw) {
        List<ItemStack> got = drops(helper, orePath, bareTool());
        if (got.isEmpty()) {
            helper.fail(orePath + " dropped nothing without Fortune");
            return false;
        }
        if (got.size() != 1 || got.get(0).getItem() != raw || got.get(0).getCount() != 1) {
            helper.fail(orePath + " without Fortune dropped " + describe(got)
                    + " but expected exactly 1 " + BuiltInRegistries.ITEM.getKey(raw));
            return false;
        }
        return true;
    }

    /** Number of rolls used for the statistical Fortune checks. */
    private static final int FORTUNE_SAMPLES = 200;

    private static int maxOver(GameTestHelper helper, String orePath, ItemStack tool, int rolls) {
        int max = 0;
        for (int i = 0; i < rolls; i++) {
            max = Math.max(max, totalCount(drops(helper, orePath, tool)));
        }
        return max;
    }

    private static int minOver(GameTestHelper helper, String orePath, ItemStack tool, int rolls) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < rolls; i++) {
            min = Math.min(min, totalCount(drops(helper, orePath, tool)));
        }
        return min;
    }

    private static boolean checkSilkTouch(GameTestHelper helper, String orePath) {
        List<ItemStack> got = drops(helper, orePath, enchantedTool(helper, "silk_touch", 1));
        Block block = oreBlock(orePath);
        if (got.size() != 1 || got.get(0).getItem() != block.asItem()) {
            helper.fail(orePath + " with Silk Touch dropped " + describe(got)
                    + " but expected the block itself, "
                    + BuiltInRegistries.ITEM.getKey(block.asItem()));
            return false;
        }
        return true;
    }

    private static String describe(List<ItemStack> stacks) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < stacks.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(stacks.get(i).getCount()).append("x")
                    .append(BuiltInRegistries.ITEM.getKey(stacks.get(i).getItem()));
        }
        return sb.append("]").toString();
    }

    /**
     * The basin casting recipe consumes exactly one ingot's worth of molten
     * tungsten steel and yields the tungsten steel ingot.
     *
     * <p>90 mB is CBC's own ingot unit (CannonCastShape.INGOT_SIZE_MB == 90), and
     * CBC casts its own metals with a create:compacting recipe of exactly this
     * shape. Asserting the fluid id and amount catches a copy-paste that left the
     * fluid pointing at CBC's metal, or the amount at a whole block's worth (810).
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void tungstenSteelIngotIsCastFromMoltenMetal(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "compacting/tungsten_steel_ingot");
        var holder = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        if (holder == null) {
            helper.fail("Recipe not loaded: " + id);
            return;
        }
        if (!(holder.value() instanceof CompactingRecipe compacting)) {
            helper.fail("Recipe is not a create:compacting recipe: "
                    + holder.value().getClass().getName());
            return;
        }

        var fluids = ((ProcessingRecipe<?, ?>) compacting).getFluidIngredients();
        if (fluids.size() != 1) {
            helper.fail("Expected exactly 1 fluid ingredient but found " + fluids.size());
            return;
        }
        SizedFluidIngredient fluid = fluids.get(0);
        Fluid molten = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "molten_tungsten_steel"));
        if (!fluid.test(new FluidStack(molten, fluid.amount()))) {
            helper.fail("The compacting recipe does not accept molten_tungsten_steel");
            return;
        }
        if (fluid.amount() != 90) {
            helper.fail("Expected 90 mB (one ingot) but the recipe wants " + fluid.amount());
            return;
        }

        var results = ((ProcessingRecipe<?, ?>) compacting).getRollableResults();
        if (results.isEmpty()) {
            helper.fail("Recipe has no result");
            return;
        }
        net.minecraft.world.item.Item produced = results.get(0).getStack().getItem();
        if (produced != CBSTungstenSteel.TUNGSTEN_STEEL_INGOT.get()) {
            helper.fail("Recipe produces " + BuiltInRegistries.ITEM.getKey(produced)
                    + " instead of tungsten_steel_ingot");
            return;
        }
        helper.succeed();
    }

    /**
     * The alloy billet recipe is a heated mixing recipe taking molten tungsten
     * steel plus an obsidian dust.
     *
     * <p>The heat requirement is the part a copy-paste is most likely to lose, so
     * it is asserted explicitly rather than assumed from the JSON.
     */
    @GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
    public static void tungstenAlloyBilletNeedsHeat(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "mixing/tungsten_alloy_billet");
        var holder = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        if (holder == null) {
            helper.fail("Recipe not loaded: " + id);
            return;
        }
        if (!(holder.value() instanceof MixingRecipe mixing)) {
            helper.fail("Recipe is not a create:mixing recipe: "
                    + holder.value().getClass().getName());
            return;
        }

        // Heated (blaze burner) - the planner's requirement for this step.
        if (mixing.getRequiredHeat() != HeatCondition.HEATED) {
            helper.fail("Expected HEATED but the recipe requires " + mixing.getRequiredHeat()
                    + " (a missing heat_requirement deserialises as NONE)");
            return;
        }

        var fluids = mixing.getFluidIngredients();
        if (fluids.size() != 1) {
            helper.fail("Expected 1 fluid ingredient but found " + fluids.size());
            return;
        }
        Fluid molten = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(
                TheRoarOfSteel.NAMESPACE, "molten_tungsten_steel"));
        if (!fluids.get(0).test(new FluidStack(molten, fluids.get(0).amount()))) {
            helper.fail("The mixing recipe does not accept molten_tungsten_steel");
            return;
        }
        if (fluids.get(0).amount() != 90) {
            helper.fail("Expected 90 mB of molten metal but found " + fluids.get(0).amount());
            return;
        }

        // The item side must be the obsidian dust (Create's powdered obsidian).
        if (mixing.getIngredients().size() != 1) {
            helper.fail("Expected exactly 1 item ingredient (obsidian dust) but found "
                    + mixing.getIngredients().size());
            return;
        }
        net.minecraft.world.item.ItemStack dust = new net.minecraft.world.item.ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath("create", "powdered_obsidian")));
        if (!mixing.getIngredients().get(0).test(dust)) {
            helper.fail("The mixing recipe does not accept create:powdered_obsidian");
            return;
        }

        var results = mixing.getRollableResults();
        if (results.isEmpty()
                || results.get(0).getStack().getItem() != CBSTungstenSteel.TUNGSTEN_ALLOY_BILLET.get()) {
            helper.fail("Recipe does not produce tungsten_alloy_billet");
            return;
        }
        // The planner asked for a processing time to be set explicitly.
        if (mixing.getProcessingDuration() != 200) {
            helper.fail("Expected a processing time of 200 ticks but found "
                    + mixing.getProcessingDuration());
            return;
        }
        helper.succeed();
    }
}
