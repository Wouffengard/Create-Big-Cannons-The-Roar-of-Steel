package com.cbcrs;

import com.mojang.logging.LogUtils;
import com.cbcrs.gametest.CBSGameTests;
import com.cbcrs.index.CBSIndex;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Main mod entry point - 机械动力火炮：钢铁轰鸣 / Create Big Cannons: The Roar of Steel.
 *
 * <p>Phase 0 responsibilities, deliberately minimal:
 * <ol>
 *   <li>Create the registrate holder for the mod's resource namespace.</li>
 *   <li>Register the dedicated creative tab.</li>
 *   <li>Declare the caliber tag keys.</li>
 *   <li>Hook into common setup for logging.</li>
 * </ol>
 *
 * <p>Not in Phase 0: the tungsten material chain, propellant data, the
 * replacement spread-angle mechanism, the feeding mechanisms, the detonator,
 * and every change that touches Create Big Cannons internals. Those belong to
 * later phases of the feasibility report.
 */
@Mod(TheRoarOfSteel.MOD_ID)
public class TheRoarOfSteel {

    /** modId: identity in mods.toml, jar naming and dependency declarations. */
    public static final String MOD_ID = "createbigcannons_theroarofsteel";

    /**
     * Resource namespace actually used by {@code assets/} and {@code data/}.
     *
     * <p>Deliberately shorter than {@link #MOD_ID}. Every ResourceLocation this
     * mod creates for its own content must use this namespace, never MOD_ID.
     */
    public static final String NAMESPACE = "cbc_rs";

    public static final Logger LOGGER = LogUtils.getLogger();

    public TheRoarOfSteel(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[{}] bootstrapping - resource namespace {}", MOD_ID, NAMESPACE);

        // 1) Registrate must exist before any content class registers anything.
        TheRoarOfSteelRegistrate.bootstrap();

        // 2) Dedicated creative tab, and point registrate at it.
        CBSIndex.registerTabs(modEventBus);

        // 3) Attach registrate's own event listeners.
        TheRoarOfSteelRegistrate.registerEventListeners(modEventBus);

        // 4) Content registration. Ordering is centralised in CBSIndex so it
        //    stays auditable in one place.
        CBSIndex.registerContent();

        // 5) Game tests. NeoForge requires explicit registration; the
        //    @GameTestHolder annotation on its own is not enough.
        CBSGameTests.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[{}] common setup complete", MOD_ID);
    }

    /** Builds a ResourceLocation inside this mod's resource namespace. */
    public static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }
}
