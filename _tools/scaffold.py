# -*- coding: utf-8 -*-
"""Phase-0 scaffold generator for 机械动力火炮：钢铁轰鸣.

Writes the complete NeoForge 1.21.1 addon project skeleton.

Design note: file contents are stored as PLAIN triple-quoted strings and the
identity tokens below are substituted at the end. This avoids f-string brace
escaping, which is error-prone when emitting Java and JSON.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

TOKENS = {
    "@@MOD_ID@@": "createbigcannons_theroarofsteel",
    "@@NS@@": "cbc_rs",
    "@@PKG@@": "com.cbcrs",
    "@@JPKG@@": "com/cbcrs",
    "@@MOD_NAME_ZH@@": "机械动力火炮：钢铁轰鸣",
    "@@MOD_NAME_EN@@": "Create Big Cannons: The Roar of Steel",
}

FILES = {}


def f(rel, content):
    FILES[rel] = content.lstrip("\n")


# ======================================================================
# Root build configuration
# ======================================================================
f("settings.gradle", """
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { name = 'NeoForged'; url = 'https://maven.neoforged.net/releases' }
    }
}

plugins {
    // Lets Gradle auto-provision the Java 21 toolchain required by Minecraft 1.21.1.
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}

rootProject.name = 'createbigcannons-theroarofsteel'
""")

f("gradle.properties", """
# =====================================================================
#  机械动力火炮：钢铁轰鸣 / Create Big Cannons: The Roar of Steel
#  Phase 0 skeleton - Gradle properties
#  Single source of truth for versions and mod metadata.
# =====================================================================

# ---- Gradle / JVM ---------------------------------------------------
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false
org.gradle.parallel=true
org.gradle.caching=true

# ---- Mod metadata (read by build.gradle) ----------------------------
# mod_id        = identity in mods.toml, jar name, dependency declarations
# mod_namespace = the asset/data namespace actually used by resources
#                 (deliberately shorter than mod_id - see README section 3)
mod_id=@@MOD_ID@@
mod_namespace=@@NS@@
mod_name=@@MOD_NAME_EN@@
mod_name_zh=@@MOD_NAME_ZH@@
mod_group=@@PKG@@
mod_version=0.1.0
mod_authors=
mod_description=An addon for Create Big Cannons: bar-based chamber pressure, tungsten steel ordnance and wireless detonators.
mod_license=MIT
mod_issues=

# ---- NeoForge / Minecraft -------------------------------------------
minecraft_version=1.21.1
minecraft_version_range=[1.21.1,1.22)
neo_version=21.1.228
neo_version_range=[21.1.228,)
loader_version_range=[4,)

# ---- Third-party mod dependencies -----------------------------------
create_version=6.0.10
create_version_range=[6.0.7,6.1.0)
rpl_version=2.1.2
rpl_version_range=[2.1.2,)
cbc_version=5.11.7
cbc_version_range=[5.11.7,)
sable_version_range=[2.0.0,3.0.0)
aeronautics_version_range=[1.3.0,)
simulated_version_range=[1.3.0,)
offroad_version_range=[1.3.0,)
copycats_version_range=[3.0.0,)
framedblocks_version_range=[10.0.0,)
curios_version_range=[9.2.0,)
""")

f("build.gradle", """
// =====================================================================
//  机械动力火炮：钢铁轰鸣 / Create Big Cannons: The Roar of Steel
//  Phase 0 skeleton - Gradle build script (NeoForge 1.21.1)
// =====================================================================
//
//  READ BEFORE BUILDING
//
//  This workspace has no network access, so the two mod dependencies
//  (Create / Create Big Cannons / RPL / Sable / Aeronautics) are wired up
//  from the local `libs/` directory instead of from Maven. That is a
//  workspace workaround, not the normal mod-development setup. Before real
//  development or publishing, switch to the Maven coordinates in the marked
//  "NORMAL SETUP" block below (see README.md section 4).
//
//  Gradle itself still needs network access once, to resolve
//  NeoForge / Minecraft / Mixin / the Gradle plugins.
// =====================================================================

plugins {
    id 'java-library'
    id 'maven-publish'
    id 'net.neoforged.moddev' version '2.0.78'
}

// ---- read gradle.properties -----------------------------------------
def p = { String key -> project.findProperty(key) ?: '' }

version = p('mod_version')
group = p('mod_group')

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

neoForge {
    version = p('neo_version')

    runs {
        client {
            client()
            systemProperty 'neoforge.enabledGameTestNamespaces', p('mod_id')
        }
        server {
            server()
            programArgument '--nogui'
            systemProperty 'neoforge.enabledGameTestNamespaces', p('mod_id')
        }
        // Datagen run. Phase 0 only creates the placeholder package tree, so
        // this run currently produces nothing. It is declared now so later
        // phases only add providers and never touch build.gradle.
        data {
            clientData()
            programArguments.addAll(
                    '--mod', p('mod_id'),
                    '--all',
                    '--output', file('src/generated/resources/').getAbsolutePath(),
                    '--existing', file('src/main/resources/').getAbsolutePath())
        }
        configureEach {
            systemProperty 'forge.logging.markers', 'REGISTRIES'
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        "${p('mod_id')}" {
            sourceSet(sourceSets.main)
        }
    }
}

// ---- repositories ---------------------------------------------------
repositories {
    mavenCentral()
    maven { name = 'NeoForged'; url = 'https://maven.neoforged.net/releases' }
    maven { name = 'BlameJared'; url = 'https://maven.blamejared.com' }   // JEI
    maven { name = 'Create'; url = 'https://maven.createmod.net' }        // Create, Ponder, Flywheel
    maven { name = 'tterrag'; url = 'https://maven.tterrag.com' }         // Registrate

    // --- workspace workaround: local jars, no network ------------------
    flatDir { dirs 'libs' }
    // ------------------------------------------------------------------
}

// ---- dependencies ---------------------------------------------------
dependencies {
    // =================================================================
    //  NORMAL SETUP - use this once network access is available, and
    //  delete the local-file block further down.
    // =================================================================
    // compileOnly "com.simibubi.create:create-${p('minecraft_version')}:${p('create_version')}-slim"
    // compileOnly "net.createmod.ponder:ponder-neoforge:1.0.82+mc${p('minecraft_version')}"
    // compileOnly "dev.engine-room.flywheel:flywheel-neoforge-api-${p('minecraft_version')}:1.0.6"
    // runtimeOnly "dev.engine-room.flywheel:flywheel-neoforge-${p('minecraft_version')}:1.0.6"
    // compileOnly "com.rbasamoyai:createbigcannons:${p('cbc_version')}"
    // compileOnly "com.rbasamoyai:ritchiesprojectilelib:${p('rpl_version')}"
    // =================================================================

    // --- workspace workaround: compile against the shipped local jars --
    // Filenames are exact so the dependency versions cannot drift silently.
    compileOnly files('libs/create-1.21.1-6.0.10.jar')
    compileOnly files('libs/createbigcannons-5.11.7+mc.1.21.1.jar')
    compileOnly files('libs/ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar')
    compileOnly files('libs/sable-neoforge-1.21.1-2.0.6.jar')
    compileOnly files('libs/create-aeronautics-bundled-1.21.1-1.3.2.jar')

    // Dev-runtime copies so `runClient` loads the dependencies.
    localRuntime files('libs/create-1.21.1-6.0.10.jar')
    localRuntime files('libs/createbigcannons-5.11.7+mc.1.21.1.jar')
    localRuntime files('libs/ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar')
    localRuntime files('libs/sable-neoforge-1.21.1-2.0.6.jar')
    localRuntime files('libs/create-aeronautics-bundled-1.21.1-1.3.2.jar')
}

// ---- jar manifest ---------------------------------------------------
tasks.named('jar', Jar).configure {
    manifest {
        attributes([
                'Specification-Title'   : p('mod_id'),
                'Specification-Vendor'  : p('mod_authors') ?: 'cbcrs',
                'Specification-Version' : '1',
                'Implementation-Title'  : project.name,
                'Implementation-Version': project.version,
                'Implementation-Vendor' : p('mod_authors') ?: 'cbcrs',
        ])
    }
}

// ---- expand mods.toml / mixins.json into the built resources --------
tasks.withType(ProcessResources).configureEach {
    def expandProps = [
            mod_id                    : p('mod_id'),
            mod_name                  : p('mod_name'),
            mod_name_zh               : p('mod_name_zh'),
            mod_license               : p('mod_license'),
            mod_version               : p('mod_version'),
            mod_authors               : p('mod_authors'),
            mod_description           : p('mod_description'),
            mod_issues                : p('mod_issues'),
            mod_group                 : p('mod_group'),
            minecraft_version         : p('minecraft_version'),
            minecraft_version_range   : p('minecraft_version_range'),
            neo_version               : p('neo_version'),
            neo_version_range         : p('neo_version_range'),
            loader_version_range      : p('loader_version_range'),
            create_version_range      : p('create_version_range'),
            rpl_version_range         : p('rpl_version_range'),
            cbc_version_range         : p('cbc_version_range'),
            sable_version_range       : p('sable_version_range'),
            aeronautics_version_range : p('aeronautics_version_range'),
            simulated_version_range   : p('simulated_version_range'),
            offroad_version_range     : p('offroad_version_range'),
            copycats_version_range    : p('copycats_version_range'),
            framedblocks_version_range: p('framedblocks_version_range'),
            curios_version_range      : p('curios_version_range'),
    ]

    inputs.properties(expandProps)
    filesMatching(['META-INF/neoforge.mods.toml']) {
        expand expandProps
    }
    filesMatching(['**/*.mixins.json']) {
        expand expandProps
    }
}

// ---- reproducible archives ------------------------------------------
tasks.withType(AbstractArchiveTask).configureEach {
    preserveFileTimestamps = false
    reproducibleFileOrder = true
}

// ---- local publishing (for a future multi-project setup) -------------
publishing {
    publications {
        register('mavenJava', MavenPublication) {
            from components.java
        }
    }
    repositories {
        maven { url "file://${project.projectDir}/repo" }
    }
}
""")

# ======================================================================
# Java sources
# ======================================================================
f("src/main/java/@@JPKG@@/TheRoarOfSteel.java", """
package @@PKG@@;

import com.mojang.logging.LogUtils;
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
    public static final String MOD_ID = "@@MOD_ID@@";

    /**
     * Resource namespace actually used by {@code assets/} and {@code data/}.
     *
     * <p>Deliberately shorter than {@link #MOD_ID}. Every ResourceLocation this
     * mod creates for its own content must use this namespace, never MOD_ID.
     */
    public static final String NAMESPACE = "@@NS@@";

    public static final Logger LOGGER = LogUtils.getLogger();

    public TheRoarOfSteel(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[{}] bootstrapping (namespace {})", MOD_ID, NAMESPACE);

        // 1) Registrate must exist before any content class registers anything.
        TheRoarOfSteelRegistrate.bootstrap();

        // 2) Dedicated creative tab, and point registrate at it.
        CBSIndex.registerTabs(modEventBus);

        // 3) Attach registrate's own event listeners.
        TheRoarOfSteelRegistrate.registerEventListeners(modEventBus);

        // 4) Phase 0 declares no content of its own. Later phases add their
        //    index classes here, in dependency order, e.g.:
        //      CBSBlocks.register();
        //      CBSItems.register();
        //      CBSFluids.register();

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
""")

f("src/main/java/@@JPKG@@/TheRoarOfSteelRegistrate.java", """
package @@PKG@@;

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
 * generated resources land in {@code assets/@@NS@@} and {@code data/@@NS@@}.
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
""")

f("src/main/java/@@JPKG@@/CBSReference.java", """
package @@PKG@@;

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
""")

f("src/main/java/@@JPKG@@/index/CBSIndex.java", """
package @@PKG@@.index;

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
     * <p>Phase 0: intentionally empty.
     */
    public static void registerContent() {
        // Later phases, in dependency order:
        //   CBSFluids.register();
        //   CBSBlocks.register();
        //   CBSItems.register();
        //   CBSBlockEntities.register();
        //   CBSMenus.register();
    }
}
""")

f("src/main/java/@@JPKG@@/index/CBSTabs.java", """
package @@PKG@@.index;

import @@PKG@@.CBSReference;
import @@PKG@@.TheRoarOfSteel;
import @@PKG@@.TheRoarOfSteelRegistrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * The mod's dedicated creative tab.
 *
 * <p>Follows the pattern Create Big Cannons itself uses: register the tab with
 * a plain {@link DeferredRegister}, then point registrate at the tab key via
 * {@code defaultCreativeTab}, so every registrate-registered item added in
 * later phases appears in this tab without per-item wiring.
 */
public final class CBSTabs {

    private CBSTabs() {
    }

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheRoarOfSteel.MOD_ID);

    /** Lang key for the tab title; provided in both locale files. */
    public static final String MAIN_TAB_LANG_KEY = "itemGroup.@@NS@@";

    public static final ResourceKey<CreativeModeTab> MAIN_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, CBSReference.loc("main"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            TABS.register("main", CBSTabs::buildMainTab);

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
        // Route every registrate-registered item into our own tab.
        TheRoarOfSteelRegistrate.get().defaultCreativeTab(MAIN_TAB_KEY);
    }

    private static CreativeModeTab buildMainTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable(MAIN_TAB_LANG_KEY))
                // Phase 0 has no items of its own, so the icon is a placeholder.
                // Later phases should swap this for the first real item, e.g.
                //   .icon(() -> CBSItems.TUNGSTEN_INGOT.asStack())
                .icon(() -> new ItemStack(Items.IRON_INGOT))
                .displayItems((parameters, output) -> {
                    // Phase 0: nothing to display yet. Later phases either push
                    // entries here explicitly or rely on the registrate binding
                    // configured in register(...).
                })
                .build();
    }

    /** The tab as a supplier, for later phases that need one. */
    public static Supplier<CreativeModeTab> mainTabSupplier() {
        return MAIN_TAB;
    }
}
""")

f("src/main/java/@@JPKG@@/index/CBSTags.java", """
package @@PKG@@.index;

import @@PKG@@.CBSReference;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tag keys declared by this mod.
 *
 * <p>Phase 0 declares only the caliber tag skeleton (feasibility report,
 * design decisions 2 and 7). The tag <em>contents</em> live under
 * {@code data/@@NS@@/tags/...} and are intentionally left empty for now.
 */
public final class CBSTags {

    private CBSTags() {
    }

    /**
     * Big-cannon calibers.
     *
     * <p>Three tiers, and these tags are the single source of truth for every
     * "only usable by X caliber" check. Code queries the tag rather than
     * inspecting block types, so supporting a new material later is a
     * data-only change.
     */
    public static final class CannonCaliber {

        private CannonCaliber() {
        }

        public static final TagKey<Block> SMALL = block("cannon_caliber/small");
        public static final TagKey<Block> MEDIUM = block("cannon_caliber/medium");
        public static final TagKey<Block> LARGE = block("cannon_caliber/large");

        public static final TagKey<Item> SMALL_ITEM = item("cannon_caliber/small");
        public static final TagKey<Item> MEDIUM_ITEM = item("cannon_caliber/medium");
        public static final TagKey<Item> LARGE_ITEM = item("cannon_caliber/large");

        /** First-phase material (feasibility report, design decision 4). */
        public static final String TUNGSTEN_STEEL = "tungsten_steel";

        /**
         * Per-material sub-tags, provided as an extension point for later
         * material-specific restrictions. The tier tags above remain the
         * primary lookup path.
         */
        public static TagKey<Block> material(String caliber, String material) {
            return block("cannon_caliber/" + caliber + "/" + material);
        }

        public static TagKey<Block> smallTungstenSteel() {
            return material("small", TUNGSTEN_STEEL);
        }

        public static TagKey<Block> mediumTungstenSteel() {
            return material("medium", TUNGSTEN_STEEL);
        }

        public static TagKey<Block> largeTungstenSteel() {
            return material("large", TUNGSTEN_STEEL);
        }
    }

    /**
     * Autocannon calibers.
     *
     * <p>Kept in a separate class with separate tag paths on purpose: the words
     * "small caliber" appear in both halves of the mod, and sharing a tag would
     * make them impossible to tell apart.
     */
    public static final class AutocannonCaliber {

        private AutocannonCaliber() {
        }

        public static final TagKey<Block> SMALL = block("autocannon_caliber/small");
        public static final TagKey<Block> LARGE = block("autocannon_caliber/large");

        public static final TagKey<Item> SMALL_ITEM = item("autocannon_caliber/small");
        public static final TagKey<Item> LARGE_ITEM = item("autocannon_caliber/large");
    }

    // ---- helpers -------------------------------------------------------

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, CBSReference.loc(path));
    }

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, CBSReference.loc(path));
    }
}
""")

f("src/main/java/@@JPKG@@/index/package-info.java", """
/**
 * Content registration index classes.
 *
 * <p>Conventions used from Phase 1 onward:
 * <ul>
 *   <li>Every index class registers into the single shared
 *       {@link @@PKG@@.TheRoarOfSteelRegistrate} instance.</li>
 *   <li>Big-cannon content lives in {@code Cannon*} classes and uses the
 *       {@code cannon_} id prefix.</li>
 *   <li>Autocannon content lives in {@code Autocannon*} classes and uses the
 *       {@code autocannon_} id prefix.</li>
 *   <li>Registration is driven explicitly from
 *       {@link @@PKG@@.index.CBSIndex}; nothing is registered from static
 *       initialisers that could run before the registrate bootstrap.</li>
 * </ul>
 */
package @@PKG@@.index;
""")

f("src/main/java/@@JPKG@@/client/TheRoarOfSteelClient.java", """
package @@PKG@@.client;

import @@PKG@@.TheRoarOfSteel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only entry point.
 *
 * <p>Declared with {@code value = Dist.CLIENT} so the class is only loaded on
 * the client. Phase 0 has nothing to initialise; it exists so later phases
 * (renderers, Ponder scenes, key mappings, muzzle-brake tooltips) have a place
 * to hook in without restructuring.
 */
@Mod(value = TheRoarOfSteel.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = TheRoarOfSteel.MOD_ID, value = Dist.CLIENT)
public final class TheRoarOfSteelClient {

    public TheRoarOfSteelClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        TheRoarOfSteel.LOGGER.info("[{}] client setup complete", TheRoarOfSteel.MOD_ID);
    }
}
""")

f("src/main/java/@@JPKG@@/mixin/package-info.java", """
/**
 * Mixin package.
 *
 * <p>Phase 0 declares the package and the (currently empty) mixin
 * configurations {@code @@NS@@.mixins.json} and
 * {@code @@NS@@.client.mixins.json}, so the Mixin service is wired up and later
 * phases can add mixins without touching the build script.
 *
 * <p>Rules for later phases, from the feasibility report's engineering
 * discipline:
 * <ul>
 *   <li>Injection points into Create Big Cannons internals are listed in
 *       chapter 8 of the report. Do not invent new ones without updating that
 *       list.</li>
 *   <li>Any mixin changing CBC behaviour must be justified by a report item;
 *       unused features must not alter CBC's behaviour.</li>
 *   <li>Client-only mixins belong in the {@code .client.} config, not in the
 *       common one.</li>
 * </ul>
 */
package @@PKG@@.mixin;
""")

f("src/main/java/@@JPKG@@/datagen/package-info.java", """
/**
 * Data generation providers.
 *
 * <p>Phase 0 ships this package empty. The {@code data} run configuration is
 * already declared in build.gradle and writes to
 * {@code src/generated/resources}, so later phases only need to add a
 * {@code DataGenerator} entry point plus providers here.
 *
 * <p>Planned providers, in the order they become useful:
 * <ol>
 *   <li>Block states and models for the tungsten steel barrels and breeches
 *       (feasibility report, design decision 5).</li>
 *   <li>Block and item tags, including the caliber tags declared in
 *       {@link @@PKG@@.index.CBSTags}.</li>
 *   <li>Recipes: pressing, melting, casting and boring for the new cannon cast
 *       moulds (design decision 6).</li>
 *   <li>Loot tables, including the boring scrap drop (report item 28).</li>
 *   <li>Lang for en_us. zh_cn is maintained by hand, since the added content is
 *       Chinese-first.</li>
 * </ol>
 */
package @@PKG@@.datagen;
""")

# ======================================================================
# Resources
# ======================================================================
f("src/main/resources/META-INF/neoforge.mods.toml", """
modLoader = "javafml"
loaderVersion = "${loader_version_range}"
license = "${mod_license}"
# issueTrackerURL = "${mod_issues}"

[[mods]]
modId = "${mod_id}"
version = "${mod_version}"
displayName = "${mod_name}"
# Phase 0 ships without an icon. Add src/main/resources/icon.png (128x128)
# and uncomment the next line before the first release.
# logoFile = "icon.png"
authors = "${mod_authors}"
description = '''
${mod_name_zh} (${mod_name})

${mod_description}

Resource namespace: @@NS@@
'''

# =====================================================================
#  Dependencies
#
#  NeoForge and Minecraft are hard requirements.
#  Create, Ritchie's Projectile Library and Create Big Cannons are declared
#  required because every planned feature builds on them.
#  The remaining CBC companions stay optional, mirroring how Create Big
#  Cannons itself declares them.
# =====================================================================

[[dependencies.${mod_id}]]
modId = "neoforge"
type = "required"
versionRange = "${neo_version_range}"
ordering = "NONE"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "minecraft"
type = "required"
versionRange = "${minecraft_version_range}"
ordering = "NONE"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "create"
type = "required"
versionRange = "${create_version_range}"
ordering = "AFTER"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "ritchiesprojectilelib"
type = "required"
versionRange = "${rpl_version_range}"
ordering = "AFTER"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "createbigcannons"
type = "required"
versionRange = "${cbc_version_range}"
ordering = "AFTER"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "sable"
type = "optional"
versionRange = "${sable_version_range}"
ordering = "AFTER"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "aeronautics"
type = "optional"
versionRange = "${aeronautics_version_range}"
ordering = "AFTER"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "simulated"
type = "optional"
versionRange = "${simulated_version_range}"
ordering = "AFTER"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "offroad"
type = "optional"
versionRange = "${offroad_version_range}"
ordering = "AFTER"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "copycats"
type = "optional"
versionRange = "${copycats_version_range}"
ordering = "AFTER"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "framedblocks"
type = "optional"
versionRange = "${framedblocks_version_range}"
ordering = "AFTER"
side = "BOTH"

[[dependencies.${mod_id}]]
modId = "curios"
type = "optional"
versionRange = "${curios_version_range}"
ordering = "AFTER"
side = "BOTH"

# =====================================================================
#  Mixin configurations
# =====================================================================
[[mixins]]
config = "@@NS@@.mixins.json"

[[mixins]]
config = "@@NS@@.client.mixins.json"
""")

f("src/main/resources/@@NS@@.mixins.json", """
{
  "required": true,
  "minVersion": "0.8",
  "package": "@@PKG@@.mixin",
  "compatibilityLevel": "JAVA_21",
  "refmap": "@@NS@@.refmap.json",
  "mixins": [],
  "injectors": {
    "defaultRequire": 1
  }
}
""")

f("src/main/resources/@@NS@@.client.mixins.json", """
{
  "required": true,
  "minVersion": "0.8",
  "package": "@@PKG@@.mixin.client",
  "compatibilityLevel": "JAVA_21",
  "refmap": "@@NS@@.refmap.json",
  "client": [],
  "injectors": {
    "defaultRequire": 1
  }
}
""")

f("src/main/resources/pack.mcmeta", """
{
  "pack": {
    "description": "@@MOD_NAME_ZH@@ resources (namespace: @@NS@@)",
    "pack_format": 34,
    "supported_formats": [34, 35]
  }
}
""")

f("src/main/resources/assets/@@NS@@/lang/en_us.json", """
{
  "itemGroup.@@NS@@": "@@MOD_NAME_EN@@",

  "_comment_phase0": "Phase 0 ships no content. The keys below are shared strings that later phases reuse.",
  "cbc_rs.tooltip.caliber.small": "Small Caliber",
  "cbc_rs.tooltip.caliber.medium": "Medium Caliber",
  "cbc_rs.tooltip.caliber.large": "Large Caliber",
  "cbc_rs.tooltip.spread_reducer": "Reduces spread only; does not affect chamber pressure."
}
""")

f("src/main/resources/assets/@@NS@@/lang/zh_cn.json", """
{
  "itemGroup.@@NS@@": "@@MOD_NAME_ZH@@",

  "_comment_phase0": "第 0 期尚无实际内容，以下为后续阶段会复用的共享文本。",
  "cbc_rs.tooltip.caliber.small": "小口径",
  "cbc_rs.tooltip.caliber.medium": "中口径",
  "cbc_rs.tooltip.caliber.large": "大口径",
  "cbc_rs.tooltip.spread_reducer": "只降低散布，不影响膛压。"
}
""")

# Caliber tag skeleton (empty contents on purpose)
for _caliber in ("small", "medium", "large"):
    for _kind in ("block", "item"):
        f("src/main/resources/data/@@NS@@/tags/%s/cannon_caliber/%s.json" % (_kind, _caliber), """
{
  "replace": false,
  "values": []
}
""")
for _caliber in ("small", "large"):
    for _kind in ("block", "item"):
        f("src/main/resources/data/@@NS@@/tags/%s/autocannon_caliber/%s.json" % (_kind, _caliber), """
{
  "replace": false,
  "values": []
}
""")

# ======================================================================
# Wrapper descriptor + housekeeping
# ======================================================================
f("gradle/wrapper/gradle-wrapper.properties", """
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
# ModDevGradle 2.0.x requires Gradle 8.10 or newer; 8.12 is a safe choice.
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.12-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
""")

f(".gitignore", """
# Gradle
.gradle/
build/
out/
repo/

# ModDevGradle / NeoForge run artifacts
run/
runs/
run-data/
logs/

# IDE
.idea/
*.iml
*.ipr
*.iws
.vscode/
.settings/
.classpath
.project
bin/

# Local dependency jars: a workspace workaround for having no network.
# Do not commit them long-term; see README section 4.
libs/*.jar

# OS
.DS_Store
Thumbs.db
""")

f("libs/README.md", """
# libs/ - local dependency jars

**Workspace-only workaround.** This machine has no network access, so
`build.gradle` compiles against the jars in this folder instead of resolving
Create / Create Big Cannons from Maven.

Expected files (filenames must match `build.gradle` exactly):

| File | Role |
| --- | --- |
| `create-1.21.1-6.0.10.jar` | Create 6.0.10 |
| `createbigcannons-5.11.7+mc.1.21.1.jar` | Create Big Cannons 5.11.7 |
| `ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar` | Ritchie's Projectile Library 2.1.2 |
| `sable-neoforge-1.21.1-2.0.6.jar` | Sable 2.0.6 |
| `create-aeronautics-bundled-1.21.1-1.3.2.jar` | Create Aeronautics (bundled: aeronautics + simulated + offroad) |

`imguimc` is intentionally **not** listed: it is not required by any of the six
mods in this workspace and is not a runtime requirement for this addon (see the
feasibility report, chapter 3).

Populate this folder with:

```powershell
pwsh -File ./_tools/fetch_local_libs.ps1
```

Before real development or publication, switch `build.gradle` back to the Maven
coordinates in its "NORMAL SETUP" block and delete this folder.
""")

# ======================================================================
# Phase-0 tooling
# ======================================================================
f("_tools/fetch_local_libs.ps1", """
<#
    Copies the workspace mod jars into .\\libs so the Gradle build can resolve
    its dependencies without network access.

    Usage (from the project root):
        pwsh -File .\\_tools\\fetch_local_libs.ps1
#>
[CmdletBinding()]
param(
    # Folder holding the source jars. Defaults to the parent of this project,
    # which is where the workspace keeps them.
    [string]$SourceDir = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $SourceDir)) {
    throw "Source directory not found: $SourceDir"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$libs = Join-Path $projectRoot 'libs'
New-Item -ItemType Directory -Force -Path $libs | Out-Null

$wanted = @(
    'create-1.21.1-6.0.10.jar',
    'createbigcannons-5.11.7+mc.1.21.1.jar',
    'ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar',
    'sable-neoforge-1.21.1-2.0.6.jar',
    'create-aeronautics-bundled-1.21.1-1.3.2.jar'
)

$missing = @()
foreach ($name in $wanted) {
    $src = Join-Path $SourceDir $name
    if (-not (Test-Path -LiteralPath $src)) { $missing += $name; continue }
    Copy-Item -LiteralPath $src -Destination (Join-Path $libs $name) -Force
    Write-Host "copied  $name"
}

if ($missing.Count -gt 0) {
    Write-Warning ("Missing source jars: " + ($missing -join ', '))
    exit 1
}

Write-Host ""
Write-Host "libs/ is ready. Next: gradlew build  (see README.md section 2)."
""")

f("_tools/verify_scaffold.py", r'''#!/usr/bin/env python3
"""Offline structural self-check for the Phase-0 scaffold.

Needs neither Gradle nor network. Verifies:
  * every file referenced by build.gradle exists;
  * each .java file's package matches its folder, with balanced braces/parens;
  * every JSON resource parses, and all resources sit in the declared namespace;
  * modId and namespace are used where the design requires, and the modId never
    leaks into a resource path.

Exit code 0 = everything consistent.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MOD_ID = "@@MOD_ID@@"
NS = "@@NS@@"
BASE_PKG = "@@PKG@@"
PKG_DIR = Path(*BASE_PKG.split("."))

problems = []
checks = 0


def ok(msg):
    global checks
    checks += 1
    print("  ok    " + msg)


def bad(msg):
    problems.append(msg)
    print("  FAIL  " + msg)


def read(p):
    return p.read_text(encoding="utf-8")


print("== top-level files ==")
required = [
    "settings.gradle",
    "build.gradle",
    "gradle.properties",
    "gradle/wrapper/gradle-wrapper.properties",
    "README.md",
    "src/main/resources/META-INF/neoforge.mods.toml",
    "src/main/resources/pack.mcmeta",
    "src/main/resources/" + NS + ".mixins.json",
    "src/main/resources/" + NS + ".client.mixins.json",
    "src/main/java/" + "/".join(PKG_DIR.parts) + "/TheRoarOfSteel.java",
    "src/main/java/" + "/".join(PKG_DIR.parts) + "/TheRoarOfSteelRegistrate.java",
    "src/main/java/" + "/".join(PKG_DIR.parts) + "/CBSReference.java",
    "src/main/java/" + "/".join(PKG_DIR.parts) + "/index/CBSIndex.java",
    "src/main/java/" + "/".join(PKG_DIR.parts) + "/index/CBSTabs.java",
    "src/main/java/" + "/".join(PKG_DIR.parts) + "/index/CBSTags.java",
    "src/main/java/" + "/".join(PKG_DIR.parts) + "/client/TheRoarOfSteelClient.java",
]
for rel in required:
    (ok if (ROOT / rel).is_file() else bad)("present: " + rel)

print("== gradle.properties vs build.gradle ==")
props = read(ROOT / "gradle.properties")
gradle = read(ROOT / "build.gradle")
used = set(re.findall(r"p\('([a-z_0-9]+)'\)", gradle))
declared = set(re.findall(r"(?m)^([a-z_0-9]+)=", props))
for key in sorted(used):
    (ok if key in declared else bad)("property declared: " + key)
for key in ("mod_id", "mod_namespace", "mod_group", "neo_version",
            "minecraft_version", "create_version", "cbc_version"):
    (ok if key in declared else bad)("required property present: " + key)

print("== local jars referenced by build.gradle ==")
for m in re.findall(r"files\('(libs/[^']+)'\)", gradle):
    (ok if (ROOT / m).is_file() else bad)(
        "libs present (else run _tools/fetch_local_libs.ps1): " + m)

print("== java: package matches folder, balanced delimiters ==")
src_root = ROOT / "src" / "main" / "java"
java_files = sorted(src_root.rglob("*.java"))
if not java_files:
    bad("no java sources found")
for jf in java_files:
    rel = jf.relative_to(src_root).as_posix()
    expect = str(Path(rel).parent).replace("\\", ".").replace("/", ".")
    body = read(jf)
    m = re.search(r"(?m)^package\s+([\w.]+)\s*;", body)
    if not m:
        bad("missing package declaration: " + rel)
        continue
    if m.group(1) != expect:
        bad("package {} != folder {} ({})".format(m.group(1), expect, rel))
    else:
        ok("package matches folder: " + rel)
    if body.count("{") != body.count("}"):
        bad("unbalanced braces: " + rel)
    if body.count("(") != body.count(")"):
        bad("unbalanced parens: " + rel)
    # crude guard against unsubstituted template tokens
    if "@@" in body:
        bad("unsubstituted @@TOKEN@@ in " + rel)
ok("java checks complete ({} files)".format(len(java_files)))

print("== json resources parse ==")
json_files = sorted((ROOT / "src" / "main" / "resources").rglob("*.json"))
if not json_files:
    bad("no json resources found")
for jf in json_files:
    try:
        json.loads(read(jf))
    except Exception as exc:
        bad("invalid JSON {}: {}".format(jf.relative_to(ROOT).as_posix(), exc))
ok("{} json files parsed".format(len(json_files)))

print("== namespace discipline ==")
res_root = ROOT / "src" / "main" / "resources"
for jf in json_files:
    rel = jf.relative_to(res_root).as_posix()
    if rel.startswith("assets/") or rel.startswith("data/"):
        top = rel.split("/")[1]
        if top != NS:
            bad("resource namespace is '{}' not '{}': {}".format(top, NS, rel))
ok("all resources live in the declared namespace")
for jf in json_files:
    if MOD_ID + ":" in read(jf):
        bad("modId leaked into a resource path: " + jf.name)
ok("modId / namespace separation respected")

print("== mods.toml ==")
toml = read(res_root / "META-INF" / "neoforge.mods.toml")
for needed in ['modLoader = "javafml"', "modId = \"${mod_id}\"",
               'modId = "create"', 'modId = "createbigcannons"',
               'modId = "ritchiesprojectilelib"', 'modId = "sable"',
               'modId = "aeronautics"']:
    (ok if needed in toml else bad)("contains: " + needed)
if "imgui" in toml.lower():
    bad("imgui-mc must not be declared as a dependency")
else:
    ok("imgui-mc is not declared as a dependency")

print("== caliber tag skeleton ==")
for tier in ("small", "medium", "large"):
    for kind in ("block", "item"):
        p = res_root / "data" / NS / "tags" / kind / "cannon_caliber" / (tier + ".json")
        (ok if p.is_file() else bad)("cannon caliber tag: {}/{}".format(kind, tier))
for tier in ("small", "large"):
    for kind in ("block", "item"):
        p = res_root / "data" / NS / "tags" / kind / "autocannon_caliber" / (tier + ".json")
        (ok if p.is_file() else bad)("autocannon caliber tag: {}/{}".format(kind, tier))

print()
print("checks passed: {}".format(checks))
if problems:
    print("problems: {}".format(len(problems)))
    for pr in problems:
        print("  - " + pr)
    sys.exit(1)
print("RESULT: scaffold structure is consistent.")
''')

f("README.md", """
# 机械动力火炮：钢铁轰鸣
# Create Big Cannons: The Roar of Steel

An addon for **Create Big Cannons 5.11.7** on **Minecraft 1.21.1 / NeoForge**.

> **Status: Phase 0 (skeleton only).** This project currently contains no
> gameplay content. It establishes the build, the registration plumbing, the
> namespace policy and the caliber tag skeleton that every later phase builds
> on. The feature roadmap and the per-item feasibility conclusions live in
> `机械动力火炮附属开发大纲_可行性报告.docx` in the workspace root.

---

## 1. Identity

| | |
| --- | --- |
| Display name (zh) | @@MOD_NAME_ZH@@ |
| Display name (en) | @@MOD_NAME_EN@@ |
| modId | `@@MOD_ID@@` |
| Resource / data namespace | `@@NS@@` |
| Base package | `@@PKG@@` |
| mod version | 0.1.0 |

---

## 2. Build prerequisites

| Requirement | Why | This machine |
| --- | --- | --- |
| **JDK 21** | Minecraft 1.21.1 / NeoForge 21.1.x target Java 21 | **missing** (only JDK 18 + JRE 8) |
| **Gradle 8.10+** | ModDevGradle 2.0.x requires it | **missing**, but `gradle/wrapper/gradle-wrapper.properties` is provided |
| **Network access (first run)** | to resolve NeoForge / Minecraft / Mixin / Gradle plugins | **unavailable** |

Because of the two missing pieces, this scaffold **cannot be compiled on this
machine yet**. The sources and configuration follow the standard layout, so on
a networked machine with JDK 21 the sequence is:

```bash
# one time: generate the wrapper jar (needs Gradle 8.10+ on PATH, or network)
gradle wrapper --gradle-version 8.12

# populate libs/ from the workspace jars (no network needed)
pwsh -File ./_tools/fetch_local_libs.ps1

# offline structural self-check - runs right now, no Gradle needed
python ./_tools/verify_scaffold.py

# build
./gradlew build
```

`gradle/wrapper/gradle-wrapper.jar` is deliberately **not** committed, because
it is a binary that could not be fetched here. Generate it once, then commit it.

---

## 3. modId vs. namespace (important)

The project uses **two different identifiers on purpose**:

* `modId = @@MOD_ID@@` — the mod's identity: `neoforge.mods.toml`, dependency
  declarations, the jar name, and `@Mod(...)`.
* namespace `= @@NS@@` — the asset/data folder name and the namespace of every
  `ResourceLocation` this mod creates for its own content.

Rationale: the mod id is descriptive and collision-safe, while the namespace is
short so tag and registry paths stay readable, e.g.
`@@NS@@:cannon_caliber/small`.

Rules that follow:

1. Resources live under `assets/@@NS@@/` and `data/@@NS@@/` only — never under
   `assets/@@MOD_ID@@/`.
2. Never build a `ResourceLocation` from `MOD_ID`. Use
   `TheRoarOfSteel.resource(path)` or `CBSReference.loc(path)`.
3. `mods.toml` declares no namespace, so nothing extra is needed there.
4. If a loader or tool ever assumes `namespace == modId`, the fix is a
   single-point change: the `NAMESPACE` constant in `TheRoarOfSteel` plus
   renaming the two resource folders. That is by design.

---

## 4. Local-jar dependency workaround

`build.gradle` compiles against jars in `libs/` rather than Maven, because this
machine has no network. See `libs/README.md` for the expected filenames, and
`_tools/fetch_local_libs.ps1` to populate the folder.

**Before real development or release, switch back to the Maven setup** — the
exact coordinates are in the "NORMAL SETUP" block of `build.gradle`. The
local-jar path is a scaffold convenience, not a supported configuration.

`imguimc` is intentionally not a dependency: it is not required by any of the
six mods in this workspace (feasibility report, chapter 3).

---

## 5. Declared dependencies

`create`, `createbigcannons` and `ritchiesprojectilelib` are **required**;
`sable`, `aeronautics`, `simulated`, `offroad`, `copycats`, `framedblocks` and
`curios` are **optional**, mirroring how Create Big Cannons declares them.

Version ranges live in `gradle.properties` and are single-sourced into
`neoforge.mods.toml` through Gradle `expand`. Change them in one place only.

---

## 6. Layout

```
createbigcannons-theroarofsteel/
|- build.gradle                      ModDevGradle build (Java 21, NeoForge 21.1.228)
|- settings.gradle                   plugin repos + Java 21 toolchain resolver
|- gradle.properties                 all versions and mod metadata (single source of truth)
|- gradle/wrapper/                   wrapper descriptor (jar must be generated once)
|- libs/                             local dependency jars (workspace workaround)
|- _tools/
|  |- fetch_local_libs.ps1           copies the workspace jars into libs/
|  |- scaffold.py                    this scaffold's generator (kept for re-runs)
|  \\- verify_scaffold.py            offline structural self-check (no Gradle needed)
\\- src/main/
   |- java/com/cbcrs/
   |  |- TheRoarOfSteel.java         @Mod entry point, MOD_ID + NAMESPACE
   |  |- TheRoarOfSteelRegistrate.java   the single CreateRegistrate instance
   |  |- CBSReference.java           id helpers, cannon_/autocannon_ prefix split
   |  |- index/
   |  |  |- CBSIndex.java            the one place registration order is visible
   |  |  |- CBSTabs.java             dedicated creative tab
   |  |  |- CBSTags.java             caliber tag keys (cannon + autocannon)
   |  |  \\- package-info.java        registration conventions for later phases
   |  |- client/TheRoarOfSteelClient.java
   |  |- mixin/package-info.java     + @@NS@@(.client).mixins.json
   |  \\- datagen/package-info.java   datagen package (data run already configured)
   \\- resources/
      |- META-INF/neoforge.mods.toml
      |- pack.mcmeta
      |- @@NS@@.mixins.json / @@NS@@.client.mixins.json
      |- assets/@@NS@@/lang/{en_us,zh_cn}.json
      \\- data/@@NS@@/tags/{block,item}/{cannon_caliber,autocannon_caliber}/*.json
```

---

## 7. Namespace separation policy (design decision 7)

`CBSReference` enforces the split at the id level:

* big-cannon content -> `CBSReference.cannon("...")` -> `@@NS@@:cannon_*`
* autocannon content -> `CBSReference.autocannon("...")` -> `@@NS@@:autocannon_*`

Caliber tags follow the same split: `cannon_caliber/*` and
`autocannon_caliber/*` never share entries. The words "small caliber" exist in
both halves of the mod, and this is what keeps them distinguishable.

---

## 8. What Phase 0 does **not** include

Per the feasibility report, these are later phases and are intentionally absent:

* tungsten material chain, molten tungsten steel, tungsten steel cast moulds
* the bar-based pressure model and the replacement spread-angle mechanism
* charge slots and the charge-bonus component
* auxiliary rammer, clip, chain loader
* the wireless-redstone detonator
* any mixin into Create Big Cannons internals (the mixin configs are empty)
* caliber *contents* (blocks/items) — only the tag skeleton exists

---

## 9. License

MIT, matching Create Big Cannons' code license. Create Big Cannons' assets are
CC BY-NC-SA: do not copy its textures or models into this project.
""")

# ======================================================================
# Write
# ======================================================================
written = 0
for rel, content in FILES.items():
    for token, value in TOKENS.items():
        content = content.replace(token, value)
        rel_resolved = rel.replace(token, value)
    # rel may itself contain tokens; resolve separately
    rel_resolved = rel
    for token, value in TOKENS.items():
        rel_resolved = rel_resolved.replace(token, value)
    target = ROOT / rel_resolved
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8", newline="\n")
    written += 1

print("wrote {} files".format(written))
for rel in sorted(FILES):
    r = rel
    for token, value in TOKENS.items():
        r = r.replace(token, value)
    print("  " + r)
