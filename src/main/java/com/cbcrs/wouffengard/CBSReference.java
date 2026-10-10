package com.cbcrs.wouffengard;

import net.minecraft.resources.ResourceLocation;

/** Central place for ids, namespaces and the shared id-building helpers. */
public final class CBSReference {

    private CBSReference() {
    }

    public static final String MOD_ID = TheRoarOfSteel.MOD_ID;
    public static final String NAMESPACE = TheRoarOfSteel.NAMESPACE;

    /**
     * Id prefixes that keep the autocannon and big-cannon halves of the mod
     * strictly separated (feasibility report, design decision 7).
     *
     * <p>These are hard constants rather than conventions: code must build ids
     * through the helpers below, so a typo cannot silently cross the boundary.
     */
    public static final String CANNON_PREFIX = "cannon_";
    public static final String AUTOCANNON_PREFIX = "autocannon_";

    /** Big-cannon id, e.g. {@code cannon_caliber/small}. */
    public static ResourceLocation cannon(String path) {
        return loc(CANNON_PREFIX + path);
    }

    /** Autocannon id, e.g. {@code autocannon_caliber/small}. */
    public static ResourceLocation autocannon(String path) {
        return loc(AUTOCANNON_PREFIX + path);
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
    }
}
