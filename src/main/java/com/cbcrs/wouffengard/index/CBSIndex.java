package com.cbcrs.wouffengard.index;

import net.neoforged.bus.api.IEventBus;

/**
 * Single entry point that drives all content registration, in a fixed order.
 *
 * <p>Later phases extend the {@code register*} methods rather than adding new
 * calls in the mod constructor, so the ordering stays visible in one file.
 */
public final class CBSIndex {

    private CBSIndex() {
    }

    /** Registers the creative tabs. Must run before any item registration. */
    public static void registerTabs(IEventBus modEventBus) {
        CBSTabs.register(modEventBus);
    }

    /**
     * Registers blocks, items, fluids and block entities.
     *
     * <p>Order matters: fluids before blocks (blocks may reference them), blocks
     * before items (block items need their blocks), and the creative tab must
     * already exist before any item is registered so registrate can bind it.
     */
    public static void registerContent() {
        // Tungsten ingot (Phase 1).
        CBSItems.register();

        // Tungsten ore blocks (decisions 15/16). Blocks register before the
        // items that reference them, matching the ordering note above.
        CBSOres.register();

        // Raw / crushed tungsten plus the tungsten nugget (decisions 15/16).
        CBSMaterials.register();

        // Later phases, in dependency order:
        //   CBSFluids.register();
        //   CBSBlockEntities.register();
        //   CBSMenus.register();
    }
}
