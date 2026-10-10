package com.cbcrs.wouffengard.client;

import com.cbcrs.wouffengard.TheRoarOfSteel;
import com.cbcrs.wouffengard.index.CBSItems;
import com.cbcrs.wouffengard.index.CBSMaterials;
import com.cbcrs.wouffengard.index.CBSOres;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Client-only entry point plus the mod's client-side asset assertions.
 *
 * <p>Absence of errors in the log is <em>not</em> evidence that a texture was
 * found: Minecraft silently substitutes the missing-texture sprite and the
 * missing model, so the checks below assert positively that each texture
 * resource resolves and that each item's model baked with real geometry and our
 * own sprite.
 *
 * <p>Timing matters and was learned the hard way: item models are <b>not</b> baked
 * yet when {@link TextureAtlasStitchedEvent} fires, so calling
 * {@code ItemRenderer#getModel} from that event throws a NullPointerException
 * inside vanilla and aborts the entire resource reload. Texture checks run at
 * stitch time (the resource manager is populated by then); model checks are
 * deferred to a later client tick.
 *
 * <p>Sprite ids use the atlas form {@code cbc_rs:item/<name>} (or
 * {@code cbc_rs:block/<name>} for blocks), not the file path form, so the checks
 * compare the logical ids.
 */
@Mod(value = TheRoarOfSteel.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TheRoarOfSteel.MOD_ID, value = Dist.CLIENT)
public final class TheRoarOfSteelClient {

    /** item id -> "item" or "block" texture folder. */
    private static final Map<String, String> CONTENT = new LinkedHashMap<>();

    static {
        CONTENT.put("tungsten_ingot", "item");
        CONTENT.put("tungsten_nugget", "item");
        CONTENT.put("raw_scheelite", "item");
        CONTENT.put("raw_wolframite", "item");
        CONTENT.put("crushed_raw_scheelite", "item");
        CONTENT.put("crushed_raw_wolframite", "item");
        // Ore blocks are verified through their block item, whose model
        // ultimately references the block texture.
        CONTENT.put("scheelite_ore", "block");
        CONTENT.put("wolframite_ore", "block");
    }

    private static boolean textureChecked = false;
    private static boolean modelCheckPending = false;
    private static int modelCheckDelay = 0;

    public TheRoarOfSteelClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        TheRoarOfSteel.LOGGER.info("[{}] client setup complete", TheRoarOfSteel.MOD_ID);
    }

    /** Texture resource check: the resource manager is populated by stitch time. */
    @SubscribeEvent
    static void onAtlasStitched(TextureAtlasStitchedEvent event) {
        if (textureChecked) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourceManager() == null) {
            return;
        }
        textureChecked = true;

        int ok = 0;
        int missing = 0;
        for (Map.Entry<String, String> e : CONTENT.entrySet()) {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(
                    TheRoarOfSteel.NAMESPACE,
                    "textures/" + e.getValue() + "/" + e.getKey() + ".png");
            Optional<Resource> found = minecraft.getResourceManager().getResource(tex);
            if (found.isPresent()) {
                ok++;
                TheRoarOfSteel.LOGGER.info("[{}] ASSET OK   texture found: {} (pack: {})",
                        TheRoarOfSteel.MOD_ID, tex, found.get().sourcePackId());
            } else {
                missing++;
                TheRoarOfSteel.LOGGER.error("[{}] ASSET FAIL texture MISSING: {}",
                        TheRoarOfSteel.MOD_ID, tex);
            }
        }
        TheRoarOfSteel.LOGGER.info("[{}] ASSET SUMMARY textures: {} present, {} missing",
                TheRoarOfSteel.MOD_ID, ok, missing);
        modelCheckPending = true;
        modelCheckDelay = 20;
    }

    /**
     * Model check, deferred until baked models are actually available.
     *
     * <p>Waits a short grace period after stitching so the model manager has
     * finished applying the reload.
     */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (!modelCheckPending) {
            return;
        }
        if (modelCheckDelay > 0) {
            modelCheckDelay--;
            return;
        }
        modelCheckPending = false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getModelManager() == null) {
            TheRoarOfSteel.LOGGER.warn("[{}] ASSET SKIP model manager unavailable",
                    TheRoarOfSteel.MOD_ID);
            return;
        }

        int ok = 0;
        int failed = 0;

        // Control: a known-good vanilla block item. If this also reports "no
        // quads" then the check itself is wrong for block items, not the asset.
        BakedModel control = minecraft.getItemRenderer()
                .getModel(new ItemStack(net.minecraft.world.item.Items.STONE), null, null, 0);
        TheRoarOfSteel.LOGGER.info(
                "[{}] ASSET CONTROL vanilla stone: class={} nullSideQuads={} downQuads={}",
                TheRoarOfSteel.MOD_ID, control.getClass().getSimpleName(),
                control.getQuads(null, null, null).size(),
                control.getQuads(null, net.minecraft.core.Direction.DOWN, null).size());

        for (Map.Entry<String, Item> e : itemsToCheck().entrySet()) {
            String name = e.getKey();
            String folder = CONTENT.get(name);
            ResourceLocation expectedSprite = ResourceLocation.fromNamespaceAndPath(
                    TheRoarOfSteel.NAMESPACE, folder + "/" + name);

            BakedModel model = minecraft.getItemRenderer()
                    .getModel(new ItemStack(e.getValue()), null, null, 0);
            if (model == null || model == minecraft.getModelManager().getMissingModel()) {
                failed++;
                TheRoarOfSteel.LOGGER.error("[{}] ASSET FAIL {} resolved to the MISSING model",
                        TheRoarOfSteel.MOD_ID, name);
                continue;
            }
            // Block items bake as cube models whose quads are keyed by face, so
            // collect the null-side list plus every face before judging.
            java.util.List<BakedQuad> quads =
                    new java.util.ArrayList<>(model.getQuads(null, null, null));
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                quads.addAll(model.getQuads(null, dir, null));
            }
            if (quads.isEmpty()) {
                failed++;
                TheRoarOfSteel.LOGGER.error("[{}] ASSET FAIL {} baked with no quads",
                        TheRoarOfSteel.MOD_ID, name);
                continue;
            }
            ResourceLocation sprite = quads.get(0).getSprite().contents().name();
            // A block model's sprite id is <ns>:block/<name>; the particle icon of
            // a cube_all model carries the same texture, so compare on the sprite
            // set rather than only the first quad.
            java.util.Set<ResourceLocation> sprites = new java.util.LinkedHashSet<>();
            for (BakedQuad q : quads) {
                sprites.add(q.getSprite().contents().name());
            }
            if (!sprites.contains(expectedSprite)) {
                failed++;
                TheRoarOfSteel.LOGGER.error("[{}] ASSET FAIL {} sprites {} do not include {}",
                        TheRoarOfSteel.MOD_ID, name, sprites, expectedSprite);
                continue;
            }
            ok++;
            TheRoarOfSteel.LOGGER.info(
                    "[{}] ASSET OK   model baked: {} class={} quads={} sprites={}",
                    TheRoarOfSteel.MOD_ID, name, model.getClass().getSimpleName(),
                    quads.size(), sprites);
        }
        TheRoarOfSteel.LOGGER.info("[{}] ASSET SUMMARY models: {} ok, {} failed",
                TheRoarOfSteel.MOD_ID, ok, failed);
    }

    /** Every block item and item under test, keyed by registry path. */
    private static Map<String, Item> itemsToCheck() {
        Map<String, Item> m = new LinkedHashMap<>();
        m.put("tungsten_ingot", CBSItems.TUNGSTEN_INGOT.get());
        m.put("tungsten_nugget", CBSMaterials.TUNGSTEN_NUGGET.get());
        m.put("raw_scheelite", CBSMaterials.RAW_SCHEELITE.get());
        m.put("raw_wolframite", CBSMaterials.RAW_WOLFRAMITE.get());
        m.put("crushed_raw_scheelite", CBSMaterials.CRUSHED_RAW_SCHEELITE.get());
        m.put("crushed_raw_wolframite", CBSMaterials.CRUSHED_RAW_WOLFRAMITE.get());
        m.put("scheelite_ore", CBSOres.SCHEELITE_ORE.get().asItem());
        m.put("wolframite_ore", CBSOres.WOLFRAMITE_ORE.get().asItem());
        return m;
    }
}
