package com.cbcrs.index;

import com.cbcrs.TheRoarOfSteel;
import com.cbcrs.TheRoarOfSteelRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Raw, crushed and nugget tungsten materials.
 *
 * <p>Design decisions 15 and 16 split the tungsten chain by ore type, because
 * the two crushed products have different recipes and loot tables:
 *
 * <pre>
 *   scheelite ore   -&gt; raw_scheelite   -&gt; crushed_raw_scheelite
 *   wolframite ore  -&gt; raw_wolframite  -&gt; crushed_raw_wolframite
 * </pre>
 *
 * <p>Both chains converge on the already existing {@code tungsten_ingot}; the
 * tungsten nugget carries Create's "crushed ore -&gt; 9 nuggets" washing
 * convention. No new metal ingot is registered here or anywhere else -
 * re-registering one would produce two competing tungsten ingots, which the
 * design explicitly forbids.
 *
 * <p>This change registers the items only. World generation, loot tables and the
 * crushing / washing / smelting recipes are tracked separately in the
 * feasibility report (sections 9.11 and 9.12) and are intentionally absent.
 */
public final class CBSMaterials {

    private CBSMaterials() {
    }

    private static ItemEntry<Item> simpleItem(String name, String englishName) {
        return TheRoarOfSteelRegistrate.get()
                .item(name, Item::new)
                .lang(englishName)
                .model((ctx, prov) -> prov.withExistingParent(
                        ctx.getName(),
                        ResourceLocation.withDefaultNamespace("item/generated"))
                        .texture("layer0", ResourceLocation.fromNamespaceAndPath(
                                TheRoarOfSteel.NAMESPACE, "item/" + ctx.getName())))
                .register();
    }

    /** Tungsten nugget: 9 of these make an ingot. */
    public static final ItemEntry<Item> TUNGSTEN_NUGGET =
            simpleItem("tungsten_nugget", "Tungsten Nugget");

    /** Dropped by scheelite ore. */
    public static final ItemEntry<Item> RAW_SCHEELITE =
            simpleItem("raw_scheelite", "Raw Scheelite");

    /** Dropped by wolframite ore. */
    public static final ItemEntry<Item> RAW_WOLFRAMITE =
            simpleItem("raw_wolframite", "Raw Wolframite");

    /** Crushing product of raw scheelite or scheelite ore. */
    public static final ItemEntry<Item> CRUSHED_RAW_SCHEELITE =
            simpleItem("crushed_raw_scheelite", "Crushed Scheelite");

    /** Crushing product of raw wolframite or wolframite ore. */
    public static final ItemEntry<Item> CRUSHED_RAW_WOLFRAMITE =
            simpleItem("crushed_raw_wolframite", "Crushed Wolframite");

    /** Forces class initialisation so the static entries above actually register. */
    public static void register() {
        // Intentionally empty: the static initialiser does the work.
    }
}
