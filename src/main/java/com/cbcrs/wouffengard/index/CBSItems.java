package com.cbcrs.wouffengard.index;

import com.cbcrs.wouffengard.TheRoarOfSteel;
import com.cbcrs.wouffengard.TheRoarOfSteelRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Item registration for 机械动力火炮：钢铁轰鸣.
 *
 * <p>Phase 1 opens with a single item, the tungsten ingot, which is the root of
 * the tungsten steel material chain described in the feasibility report
 * (report item 26). Later phases extend this class with tungsten nuggets, raw
 * tungsten, tungsten steel ingot and the munition items; the autocannon half of
 * the mod must go into a separate {@code Autocannon*} class so the two
 * namespaces stay isolated (design decision 7).
 *
 * <p>Registration style follows Create / Create Big Cannons: entries are
 * declared as static fields whose initialiser builds and registers them, and
 * {@link #register()} exists purely to force this class to initialise at a
 * controlled point in the mod constructor.
 */
public final class CBSItems {

    private CBSItems() {
    }

    /**
     * Tungsten ingot - the mod's first item.
     *
     * <p>The model is a plain {@code item/generated} sprite using the supplied
     * {@code cbc_rs:item/tungsten_ingot} texture. Both the model and the en_us
     * display name are produced by Registrate's data generators (see
     * {@code runData}); zh_cn is written by hand in
     * {@code assets/cbc_rs/lang/zh_cn.json} because the added content is
     * Chinese-first.
     */
    public static final ItemEntry<Item> TUNGSTEN_INGOT = TheRoarOfSteelRegistrate.get()
            .item("tungsten_ingot", Item::new)
            .lang("Tungsten Ingot")
            .model((ctx, prov) -> prov.withExistingParent(
                            ctx.getName(),
                            ResourceLocation.withDefaultNamespace("item/generated"))
                    .texture("layer0",
                            ResourceLocation.fromNamespaceAndPath(
                                    TheRoarOfSteel.NAMESPACE,
                                    "item/" + ctx.getName())))
            .register();

    /**
     * Forces class initialisation so the static entries above actually register.
     *
     * <p>Called from {@code CBSIndex.registerContent()} during mod construction,
     * after the creative tab exists.
     */
    public static void register() {
        // Intentionally empty: the static initialiser does the work.
    }
}
