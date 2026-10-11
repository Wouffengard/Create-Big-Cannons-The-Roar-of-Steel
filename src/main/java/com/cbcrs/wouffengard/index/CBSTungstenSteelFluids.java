package com.cbcrs.wouffengard.index;

import com.cbcrs.wouffengard.TheRoarOfSteel;
import com.cbcrs.wouffengard.TheRoarOfSteelRegistrate;
import com.simibubi.create.AllFluids.TintedFluidType;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import com.tterrag.registrate.util.entry.FluidEntry;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Source;
import org.joml.Vector3f;

/**
 * Molten tungsten steel.
 *
 * <p>Implemented with Create's own {@code standardFluid} builder and the same
 * shape Create Big Cannons uses for its molten metals
 * ({@code CBCFluids.MOLTEN_CAST_IRON} / {@code MOLTEN_STEEL} / ...), so it
 * behaves and renders like the fluids it sits alongside:
 *
 * <ul>
 *   <li>{@code standardFluid("molten_tungsten_steel")} derives the textures as
 *       {@code cbc_rs:fluid/molten_tungsten_steel_still} and
 *       {@code ..._flow}, which is exactly where the supplied PNGs are placed.</li>
 *   <li>{@code TintedFluidType} supplies per-fluid fog; the tint itself stays
 *       white (as CBC does) so the texture's own colour is what you see.</li>
 *   <li>A {@code LiquidBlock} is created by {@code .block(...)}, so the fluid can
 *       exist as a source block in world.</li>
 * </ul>
 *
 * <p>Properties are taken from CBC's molten steel as the reference point, with a
 * slightly higher temperature because this is the tungsten grade. The casting
 * time lives in {@code data/createbigcannons/fluid_casting_time/} and is
 * deliberately a separate file.
 *
 * <p>A bucket item is registered, so the fluid can be picked up and carried.
 * The builder derives the icon from {@code textures/item/<name>_bucket.png} and
 * the lang key as {@code item.cbc_rs.<name>_bucket}, matching CBC's
 * {@code molten_steel_bucket} convention.
 */
public final class CBSTungstenSteelFluids {

    /** Fluid id, also the basis for the texture and data file names. */
    public static final String MOLTEN_TUNGSTEN_STEEL_ID = "molten_tungsten_steel";

    /**
     * Fog colour, matching the blue-biased steel tint of the textures
     * (mean RGB ~ (92, 94, 110) before the fluid renders its own alpha).
     */
    private static final Vector3f FOG_COLOR = new Vector3f(0.36F, 0.37F, 0.43F);

    /** How far the fog reaches relative to water's 96 blocks. */
    private static final float FOG_DISTANCE = 3.0F;

    public static final FluidEntry<Flowing> MOLTEN_TUNGSTEN_STEEL =
            TheRoarOfSteelRegistrate.get()
                    .standardFluid(MOLTEN_TUNGSTEN_STEEL_ID,
                            MoltenMetalFluidType.create(FOG_COLOR, FOG_DISTANCE))
                    // The fluid name only; the FluidType has no lang entry of its own.
                    .lang("Molten Tungsten Steel")
                    .properties(p -> p
                            .viscosity(1500)
                            .density(8000)
                            .temperature(1500))
                    .fluidProperties(p -> p
                            .levelDecreasePerBlock(2)
                            .tickRate(25)
                            .slopeFindDistance(3)
                            .explosionResistance(100.0F))
                    .source(Source::new)
                    .block()
                    .build()
                    // bucket() returns the bucket's ItemBuilder, which supplies its
                    // own generated model from item/<name>_bucket; build() hands the
                    // FluidBuilder back so the fluid itself is registered last.
                    // (This is the same chain CBC uses, .bucket()....build().register().)
                    .bucket()
                    .build()
                    .register();

    private CBSTungstenSteelFluids() {
    }

    /** Forces class initialisation so the static entry above actually registers. */
    public static void register() {
        // Intentionally empty: the static initialiser does the work.
    }

    /**
     * Solid-rendered molten metal fluid type: opaque, no tint, custom fog.
     *
     * <p>Mirrors CBC's {@code SolidRenderedPlaceableFluidType} but computes the
     * fog vector directly instead of going through Create's {@code Color} helper,
     * so the addon does not depend on an additional utility class for one
     * conversion. CBC's cfg-driven transparency multiplier is intentionally not
     * copied - that is a CBC config concern, not ours.
     */
    private static final class MoltenMetalFluidType extends TintedFluidType {

        private final Vector3f fogColor;
        private final float fogDistance;

        private MoltenMetalFluidType(net.neoforged.neoforge.fluids.FluidType.Properties properties,
                                     ResourceLocation stillTexture,
                                     ResourceLocation flowingTexture,
                                     Vector3f fogColor,
                                     float fogDistance) {
            super(properties, stillTexture, flowingTexture);
            this.fogColor = fogColor;
            this.fogDistance = fogDistance;
        }

        static FluidTypeFactory create(Vector3f fogColor, float fogDistance) {
            return (properties, stillTexture, flowingTexture) ->
                    new MoltenMetalFluidType(properties, stillTexture, flowingTexture,
                            fogColor, fogDistance);
        }

        @Override
        protected int getTintColor(net.neoforged.neoforge.fluids.FluidStack stack) {
            return NO_TINT;
        }

        @Override
        protected int getTintColor(FluidState state, BlockAndTintGetter level, BlockPos pos) {
            return 0xFFFFFF;
        }

        @Override
        protected Vector3f getCustomFogColor() {
            return this.fogColor;
        }

        @Override
        protected float getFogDistanceModifier() {
            return this.fogDistance;
        }
    }
}
