package com.cbcrs.index;

import com.cbcrs.CBSReference;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tag keys declared by this mod.
 *
 * <p>Phase 0 declares only the caliber tag skeleton (feasibility report,
 * design decisions 2 and 7). The tag <em>contents</em> live under
 * {@code data/cbc_rs/tags/...} and are intentionally left empty for now.
 */
public final class CBSTags {

    private CBSTags() {
    }

    /**
     * Big-cannon calibers.
     *
     * <p>Three tiers, and these tags are the single source of truth for every
     * "only usable by X caliber" check. Code queries the tag rather than
     * inspecting block types, so supporting a new material later is a
     * data-only change.
     */
    public static final class CannonCaliber {

        private CannonCaliber() {
        }

        public static final TagKey<Block> SMALL = block("cannon_caliber/small");
        public static final TagKey<Block> MEDIUM = block("cannon_caliber/medium");
        public static final TagKey<Block> LARGE = block("cannon_caliber/large");

        public static final TagKey<Item> SMALL_ITEM = item("cannon_caliber/small");
        public static final TagKey<Item> MEDIUM_ITEM = item("cannon_caliber/medium");
        public static final TagKey<Item> LARGE_ITEM = item("cannon_caliber/large");

        /** First-phase material (feasibility report, design decision 4). */
        public static final String TUNGSTEN_STEEL = "tungsten_steel";

        /**
         * Per-material sub-tags, provided as an extension point for later
         * material-specific restrictions. The tier tags above remain the
         * primary lookup path.
         */
        public static TagKey<Block> material(String caliber, String material) {
            return block("cannon_caliber/" + caliber + "/" + material);
        }

        public static TagKey<Block> smallTungstenSteel() {
            return material("small", TUNGSTEN_STEEL);
        }

        public static TagKey<Block> mediumTungstenSteel() {
            return material("medium", TUNGSTEN_STEEL);
        }

        public static TagKey<Block> largeTungstenSteel() {
            return material("large", TUNGSTEN_STEEL);
        }
    }

    /**
     * Autocannon calibers.
     *
     * <p>Kept in a separate class with separate tag paths on purpose: the words
     * "small caliber" appear in both halves of the mod, and sharing a tag would
     * make them impossible to tell apart.
     */
    public static final class AutocannonCaliber {

        private AutocannonCaliber() {
        }

        public static final TagKey<Block> SMALL = block("autocannon_caliber/small");
        public static final TagKey<Block> LARGE = block("autocannon_caliber/large");

        public static final TagKey<Item> SMALL_ITEM = item("autocannon_caliber/small");
        public static final TagKey<Item> LARGE_ITEM = item("autocannon_caliber/large");
    }

    // ---- helpers -------------------------------------------------------

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, CBSReference.loc(path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, CBSReference.loc(path));
    }
}
