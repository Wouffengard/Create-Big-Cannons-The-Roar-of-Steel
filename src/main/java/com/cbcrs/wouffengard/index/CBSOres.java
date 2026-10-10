package com.cbcrs.wouffengard.index;

import com.cbcrs.wouffengard.TheRoarOfSteelRegistrate;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Ore block registration.
 *
 * <p>Design decisions 15 and 16 add two tungsten ores: scheelite (stone layer
 * only) and wolframite (deepslate layer only). This class registers the
 * <b>blocks</b> only - world generation, loot content and the processing recipes
 * are deliberately excluded and are tracked separately in the feasibility report
 * (sections 9.11 and 9.12).
 *
 * <p>Both are plain full-cube blocks, so Registrate's defaults produce the
 * blockstate, the block model and the block item model. Their textures are fully
 * opaque 16x16 images, matching the vanilla ore convention (item icons are
 * cut-out sprites, block textures are opaque).
 *
 * <p>Mining requires a stone tool, matching iron and copper ore.
 */
public final class CBSOres {

    private CBSOres() {
    }

    /**
     * Shared ore properties.
     *
     * <p>{@code requiresCorrectToolForDrops()} mirrors vanilla ore: without a
     * pickaxe of the right tier the block yields nothing.
     */
    private static Block.Properties oreProperties() {
        return Block.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0F, 3.0F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops();
    }

    /**
     * Both ores use vanilla's own ore drop table, via
     * {@link RegistrateBlockLootTables#createOreDrop}: Silk Touch yields the ore
     * block, otherwise exactly one raw ore with {@code minecraft:ore_drops} so
     * Fortune scales it.
     *
     * <p>The table is produced by datagen into {@code src/generated/resources},
     * which Minecraft reads BEFORE {@code src/main/resources}. Hand-writing the
     * table under {@code src/main/resources} therefore does not work - it is
     * silently shadowed, which is exactly the bug a game test caught.
     */
    private static NonNullBiConsumer<RegistrateBlockLootTables, Block> oreDrops(
            NonNullSupplier<Item> rawOre) {
        return (loot, block) -> loot.add(block, loot.createOreDrop(block, rawOre.get()));
    }

    /** Whitish tungsten ore, generated in the stone layer. */
    public static final BlockEntry<Block> SCHEELITE_ORE = TheRoarOfSteelRegistrate.get()
            .block("scheelite_ore", Block::new)
            .properties(p -> oreProperties())
            .defaultBlockstate()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
            .lang("Scheelite Ore")
            .item()
            .build()
            .loot(oreDrops(() -> CBSMaterials.RAW_SCHEELITE.get()))
            .register();

    /** Dark tungsten ore, generated in the deepslate layer. */
    public static final BlockEntry<Block> WOLFRAMITE_ORE = TheRoarOfSteelRegistrate.get()
            .block("wolframite_ore", Block::new)
            .properties(p -> oreProperties())
            .defaultBlockstate()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_STONE_TOOL)
            .lang("Wolframite Ore")
            .item()
            .build()
            .loot(oreDrops(() -> CBSMaterials.RAW_WOLFRAMITE.get()))
            .register();

    /** Forces class initialisation so the static entries above actually register. */
    public static void register() {
        // Intentionally empty: the static initialiser does the work.
    }
}
