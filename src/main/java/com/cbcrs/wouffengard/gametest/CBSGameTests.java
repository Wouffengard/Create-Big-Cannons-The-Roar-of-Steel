package com.cbcrs.wouffengard.gametest;

import com.cbcrs.wouffengard.TheRoarOfSteel;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.neoforged.bus.api.IEventBus;

/**
 * Central registration point for this mod's game tests.
 *
 * <p>NeoForge does not discover {@code @GameTestHolder} classes on its own in
 * this project: the annotation scan-data lookup comes up empty, so the game test
 * server aborts with {@code IllegalArgumentException: No test functions were
 * given!} even though the test methods exist and the namespace is enabled
 * ({@code neoforge.enabledGameTestNamespaces} is set on the run configuration).
 *
 * <p>The tests are therefore registered explicitly, straight into vanilla's
 * {@link GameTestRegistry}, from the mod constructor. This was verified by
 * instrumenting the mod: the direct registration lands first, and NeoForge's
 * {@code RegisterGameTestsEvent} fires afterwards. Because the event fires after
 * the registry has already been consumed for this run, registering in both
 * places only duplicates every test (observed: 6 tests instead of 3), so there
 * is deliberately exactly one registration route here.
 *
 * <p>If a future NeoForge version starts discovering the annotations, the
 * explicit call below can be dropped - but do not simply move it into
 * {@code RegisterGameTestsEvent} without re-testing, because that configuration
 * failed with "No test functions were given!".
 *
 * <p>The list is kept in one place so a new test class only has to be added
 * here, mirroring how {@code CBSIndex} centralises content registration.
 */
public final class CBSGameTests {

    private CBSGameTests() {
    }

    /** Called from the mod constructor, on the mod event bus. */
    public static void register(IEventBus modEventBus) {
        registerAll();
    }

    private static void registerAll() {
        GameTestRegistry.register(TungstenIngotGameTests.class);
        TheRoarOfSteel.LOGGER.info("[{}] registered game test class {} directly",
                TheRoarOfSteel.MOD_ID, TungstenIngotGameTests.class.getSimpleName());

        GameTestRegistry.register(TungstenSteelGameTests.class);
        TheRoarOfSteel.LOGGER.info("[{}] registered game test class {} directly",
                TheRoarOfSteel.MOD_ID, TungstenSteelGameTests.class.getSimpleName());

        // Later phases append their test classes here, e.g.:
        //   GameTestRegistry.register(PropellantGameTests.class);
    }
}
