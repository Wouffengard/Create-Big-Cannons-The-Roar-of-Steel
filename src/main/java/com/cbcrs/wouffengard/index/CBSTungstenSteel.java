package com.cbcrs.wouffengard.index;

import com.cbcrs.wouffengard.TheRoarOfSteelRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

/**
 * Tungsten steel and tungsten alloy solids.
 *
 * <p>These are the metal endpoints of the two ore chains: wolframite and
 * scheelite both smelt to a plain {@code tungsten_ingot}, which is then worked
 * into tungsten steel and, further along, the tungsten alloy used for ordnance.
 * They are plain items - no block forms, no tools, no materials chain beyond
 * the ingot/nugget pair already registered in {@link CBSItems} and
 * {@link CBSMaterials}.
 *
 * <p>All three share one icon-generating model ({@code item/generated}), so the
 * assets are just the three PNGs plus a lang entry each.
 */
public final class CBSTungstenSteel {

    /** The steel grade the ordnance is made from. */
    public static final ItemEntry<Item> TUNGSTEN_STEEL_INGOT =
            simpleItem("tungsten_steel_ingot", "Tungsten Steel Ingot");

    /** Intermediate blank that is later alloyed. */
    public static final ItemEntry<Item> TUNGSTEN_ALLOY_BILLET =
            simpleItem("tungsten_alloy_billet", "Tungsten Alloy Billet");

    /** The finished ordnance alloy. */
    public static final ItemEntry<Item> TUNGSTEN_ALLOY_INGOT =
            simpleItem("tungsten_alloy_ingot", "Tungsten Alloy Ingot");

    private CBSTungstenSteel() {
    }

    /**
     * Registers a plain item with its model and an English display name.
     *
     * <p>The English name is set here rather than generated from the registry id
     * so wording stays under our control; {@code zh_cn} is maintained by hand in
     * {@code assets/cbc_rs/lang/zh_cn.json}.
     */
    private static ItemEntry<Item> simpleItem(String name, String englishName) {
        return TheRoarOfSteelRegistrate.get()
                .item(name, Item::new)
                .lang(englishName)
                .model((ctx, prov) -> prov.generated(ctx))
                .register();
    }

    /** Forces class loading so the static registrations above run in order. */
    public static void register() {
        // Registration happens in the static initialisers; this method exists so
        // CBSIndex drives the order explicitly.
    }
}
