package com.cbcrs;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.neoforged.bus.api.IEventBus;

/**
 * The single shared {@link CreateRegistrate} instance for the whole mod.
 *
 * <p>Everything registered through Registrate (blocks, items, fluids, block
 * entities, menus, ...) must attach to this one instance. Several competing
 * instances would each run their own registration pipeline and the generated
 * lang / models / blockstates would be produced inconsistently.
 *
 * <p>The instance is created against {@link TheRoarOfSteel#NAMESPACE}, so all
 * generated resources land in {@code assets/cbc_rs} and {@code data/cbc_rs}.
 */
public final class TheRoarOfSteelRegistrate {

    private static CreateRegistrate instance;

    private TheRoarOfSteelRegistrate() {
    }

    /** Creates the instance. Must run before any content class is touched. */
    public static void bootstrap() {
        if (instance == null) {
            instance = CreateRegistrate.create(TheRoarOfSteel.NAMESPACE);
        }
    }

    public static CreateRegistrate get() {
        if (instance == null) {
            throw new IllegalStateException("CreateRegistrate accessed before bootstrap(); "
                    + "check the constructor ordering in TheRoarOfSteel");
        }
        return instance;
    }

    /**
     * Attaches the registrate event listeners to the mod event bus.
     *
     * <p>Ordering: {@link #bootstrap()} and all content registration must have
     * happened before the bus starts firing, which is why Phase 0 keeps this as
     * an explicit call from the mod constructor instead of a static block.
     */
    public static void registerEventListeners(IEventBus modEventBus) {
        get().registerEventListeners(modEventBus);
    }
}
