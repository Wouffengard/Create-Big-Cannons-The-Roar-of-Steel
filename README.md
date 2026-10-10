# 机械动力火炮：钢铁轰鸣
# Create Big Cannons: The Roar of Steel

An addon for **Create Big Cannons 5.11.7** on **Minecraft 1.21.1 / NeoForge 21.1.228**.

> **Status: Phase 1 - the tungsten ingot exists and is verified in-game.**
> Phase 0 established the build, registration plumbing, namespace policy and
> caliber tag skeleton. Phase 1 adds the mod's first item: the tungsten ingot
> (feasibility report item 26), the root of the tungsten steel material chain.
> The feature roadmap and per-item feasibility conclusions live in
> `机械动力火炮附属开发大纲_可行性报告_v4.docx` in the workspace root.
>
> Verified on this machine:
>
> * `gradlew build` succeeds and produces a jar with the expanded
>   `neoforge.mods.toml`, the item model, both lang files and the texture.
> * `gradlew runData` generates `models/item/tungsten_ingot.json` and the
>   en_us/en_ud lang entries from Registrate.
> * `gradlew runServer` reaches `Done (0.709s)!` with this addon plus Create
>   6.0.10, Create Big Cannons 5.11.7, Ritchie's Projectile Library 2.1.2,
>   Sable 2.0.6 and Create Aeronautics 1.3.2 (incl. Simulated / Offroad / Veil)
>   all loaded.
> * `gradlew runGameTestServer` reports **All 3 required tests passed**, proving
>   the item is registered under `cbc_rs:tungsten_ingot`, is the same instance
>   the code references, and produces a valid ItemStack.
> * `gradlew runClient` starts with all 15 mods loaded and logs three
>   `ASSET OK` lines (texture resource found, model baked with 26 quads, pixel
>   fingerprint matching the source PNG byte-for-byte). Zero errors and zero
>   warnings were emitted by this mod. The item was then inspected by eye in the
>   creative tab and rendered correctly. Exact numbers and hashes are recorded in
>   [TESTING.md](TESTING.md) section 4.

> **Testing:** see [TESTING.md](TESTING.md) for the rendering/item test
> procedure, the success criteria, the recorded baseline, and the known
> compatibility noise that must not be misdiagnosed as a regression.

---

## 1. Identity

| | |
| --- | --- |
| Display name (zh) | 机械动力火炮：钢铁轰鸣 |
| Display name (en) | Create Big Cannons: The Roar of Steel |
| modId | `createbigcannons_theroarofsteel` |
| Resource / data namespace | `cbc_rs` |
| Base package | `com.cbcrs.wouffengard` |
| Author | Wouffengard |
| mod version | 0.1.0 |

### Content currently in the jar

| Content | Id | Texture | Notes |
| --- | --- | --- | --- |
| Tungsten Ingot | `cbc_rs:tungsten_ingot` | `textures/item/tungsten_ingot.png` | `CBSItems`. The final product of both ore chains; also the creative tab icon. |
| Tungsten Nugget | `cbc_rs:tungsten_nugget` | `textures/item/tungsten_nugget.png` | `CBSMaterials`. Carries Create's "crushed ore -> 9 nuggets" convention. |
| Raw Scheelite | `cbc_rs:raw_scheelite` | `textures/item/raw_scheelite.png` | `CBSMaterials`. Dropped by scheelite ore. |
| Raw Wolframite | `cbc_rs:raw_wolframite` | `textures/item/raw_wolframite.png` | `CBSMaterials`. Dropped by wolframite ore. |
| Crushed Scheelite | `cbc_rs:crushed_raw_scheelite` | `textures/item/crushed_raw_scheelite.png` | `CBSMaterials`. |
| Crushed Wolframite | `cbc_rs:crushed_raw_wolframite` | `textures/item/crushed_raw_wolframite.png` | `CBSMaterials`. |
| Scheelite Ore | `cbc_rs:scheelite_ore` | `textures/block/scheelite_ore.png` | `CBSOres`. Full-cube block, stone tool, drops itself. |
| Wolframite Ore | `cbc_rs:wolframite_ore` | `textures/block/wolframite_ore.png` | `CBSOres`. Full-cube block, stone tool, drops itself. |

The two ores are **layer-exclusive** by design (decision 15): scheelite replaces
only the stone layer, wolframite only the deepslate layer. Both chains converge
on the single `tungsten_ingot`, and a game test asserts that exactly one tungsten
ingot item exists (decision 16).

Ore **world generation**, the **ore loot tables** and every **recipe** are now
in place (decisions 15-17). See the sections below.

---

## 2. Toolchain

| Requirement | Notes |
| --- | --- |
| **JDK 21** | Minecraft 1.21.1 / NeoForge 21.1.x target Java 21. Any JDK 21 works; the build only needs `JAVA_HOME` (or toolchain auto-provisioning) to point at one. |
| **Gradle 8.10+** | `gradle/wrapper/gradle-wrapper.jar` is committed, so `gradlew` is enough. |
| **Network (first build only)** | NeoForge, Minecraft, Create, Flywheel, Ponder and Registrate are resolved from Maven. |

### Build and run

```bash
# populate libs/ from the workspace jars (see section 4)
powershell -ExecutionPolicy Bypass -File ./_tools/fetch_local_libs.ps1

# offline structural self-check - no Gradle needed
python ./_tools/verify_scaffold.py

# build
./gradlew build

# regenerate assets (item models, lang) into src/generated/resources
./gradlew runData

# in-game verification of the items/machines
./gradlew runGameTestServer

# client-side asset assertions (texture + model) and a visual check
./gradlew runClient      # then grep run/logs/debug.log for "ASSET"

# run a dev server / client (stages the external mods automatically)
./gradlew runServer
./gradlew runClient
```

All run configurations share the `run/` game directory, so `syncMods` stages the
external mods once and worlds/configs are reused.

Expected artifacts: `build/libs/createbigcannons-theroarofsteel-0.1.0.jar`
and a `-sources.jar`.

### Content conventions

* **Item ids are plain** (`tungsten_ingot`), not prefixed. The namespace already
  separates this mod from Create Big Cannons. The prefix rule in section 8
  applies to the big-cannon / autocannon split, not to materials.
* **`en_us` is generated; `zh_cn` is hand-written.** Registrate's lang provider
  owns `assets/cbc_rs/lang/en_us.json` (and `en_ud`), so English strings for
  items live in their `.lang(...)` call and any non-item key is injected with
  `addRawLang(...)` in `CBSTabs.register`. Chinese strings are maintained by hand
  in `assets/cbc_rs/lang/zh_cn.json`, because the added content is
  Chinese-first.
* **`src/generated/resources` is part of the main source set**, so datagen
  output reaches both the jar and the dev runs. Because a hand-written file and
  a generated file can share a path, `processResources` sets
  `duplicatesStrategy = INCLUDE`.
* **Do not put non-ASCII text into a file that goes through `expand()`** (see
  section 3a).

### Game tests

Tests live in `src/main/java/com/cbcrs/gametest`. Two things are easy to get
wrong and are handled deliberately:

1. **Registration is explicit.** `@GameTestHolder` / `@GameTest` alone are not
   enough: the class must be handed to the registry, which `CBSGameTests` does
   through `RegisterGameTestsEvent`. Using *both* the event and
   `GameTestRegistry.register` makes every test run twice.
2. **A structure template is required.** `@GameTest(template = "empty")` needs
   `data/cbc_rs/structure/empty.nbt`, a single-GZIP-compressed NBT structure.
   The file here is generated and round-trip validated by
   `_tools/make_empty_structure.py`, because hand-writing NBT is error prone
   (an early attempt double-compressed it, and a second one omitted the
   `TAG_END` terminators, producing `ReportedNbtException`).

### Note on the Gradle distribution URL

`gradle/wrapper/gradle-wrapper.properties` points at
`https://services.gradle.org/distributions/gradle-8.12-bin.zip`, the canonical
URL. On this machine that host redirects to GitHub and stalls, so the wrapper
was generated with a mirror. If your first `./gradlew` run hangs while
downloading, change `distributionUrl` to a reachable mirror (for example
`https://mirrors.cloud.tencent.com/gradle/gradle-8.12-bin.zip`) or install
Gradle 8.12 locally and run `gradle wrapper` once.

---

## 3. modId vs. namespace (important)

Two different identifiers are used **on purpose**:

* `modId = createbigcannons_theroarofsteel` - the mod's identity:
  `neoforge.mods.toml`, dependency declarations, the jar name, `@Mod(...)`.
* namespace `= cbc_rs` - the asset/data folder name and the namespace of every
  `ResourceLocation` this mod creates for its own content.

Rationale: the mod id is descriptive and collision-safe, while the namespace is
short so tag and registry paths stay readable, e.g. `cbc_rs:cannon_caliber/small`.

Rules that follow:

1. Resources live under `assets/cbc_rs/` and `data/cbc_rs/` only - never under
   `assets/createbigcannons_theroarofsteel/`.
2. Never build a `ResourceLocation` from `MOD_ID`. Use
   `TheRoarOfSteel.resource(path)` or `CBSReference.loc(path)`.
3. `mods.toml` declares no namespace, so nothing extra is needed there.
4. If a loader or tool ever assumes `namespace == modId`, the fix is a
   single-point change: the `NAMESPACE` constant in `TheRoarOfSteel` plus
   renaming the two resource folders. That is by design.

### 3a. `mods.toml` is intentionally ASCII-only

`META-INF/neoforge.mods.toml` goes through Gradle `expand()` template
expansion. On Windows, Gradle's template engine reads the template with the
platform default charset, so any non-ASCII text inside an expanded template ends
up mangled into mojibake in the built jar. Neither `filteringCharset = 'UTF-8'`
nor `-Dfile.encoding=UTF-8` fixes it (both were tried).

Therefore: **do not put non-ASCII text into a file that goes through
`expand()`.** Localized strings belong in `assets/cbc_rs/lang/` (which is copied
verbatim, UTF-8 preserved, and is where the Chinese display name actually
lives).

---

## 4. Dependency strategy

Create Big Cannons, Ritchie's Projectile Library, Sable and Create Aeronautics
are **not published to any public Maven** (checked against Maven Central,
modmaven, maven.createmod.net and maven.tterrag.com), so they are always taken
from `libs/`:

| File | Role |
| --- | --- |
| `createbigcannons-5.11.7+mc.1.21.1.jar` | Create Big Cannons 5.11.7 |
| `ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar` | Ritchie's Projectile Library 2.1.2 |
| `sable-neoforge-1.21.1-2.0.6.jar` | Sable 2.0.6 (optional dependency) |
| `create-aeronautics-bundled-1.21.1-1.3.2.jar` | Create Aeronautics (bundled: aeronautics + simulated + offroad) |
| `create-1.21.1-6.0.10.jar`, `Registrate-*.jar`, `ponder-*.jar`, `flywheel-*.jar` | offline (MODE B) only |

Populate with:

```powershell
powershell -ExecutionPolicy Bypass -File ./_tools/fetch_local_libs.ps1
```

The script copies the four mod jars from the workspace and extracts the three
libraries Create bundles in `META-INF/jarjar/`.

`imgui-mc` is intentionally **not** a dependency: it is not required by any of
the six mods in this workspace (feasibility report, chapter 3).

### Compile-time

`build.gradle` compiles against Maven for Create / Registrate / Ponder /
Flywheel and against `libs/` for the four mods above. The Create dependency uses
the **`slim` classifier**, requested through an artifact selector:

```groovy
compileOnly("com.simibubi.create:create-${minecraft_version}:${create_version}") {
    artifact { classifier = 'slim' }
}
```

`slim` is a classifier, not part of the artifactId - appending `-slim` to the
coordinate asks for a version that does not exist.

---

## 5. Dev runs and external mods

`runServer` / `runClient` need Create plus the four local mods to be **registered
as mods**, not merely present on the classpath.

**How this project does it:** the `syncServerMods` / `syncClientMods` tasks
stage the jars into `run-server/mods` and `run/mods` before launch, and the run
tasks depend on them. That is the same mechanism a player uses.

**Why not the classpath:** FML treats every legacy-classpath entry as a *boot
library*. Mod jars placed there load as libraries but never appear in the mod
list, so the mandatory-dependency checks fail with "create is not installed"
even though the jar is listed in the classpath file. The `fml.modFolders`
system property is FML's dev hook for this, but its format is not a
`modId=path` list - the comma separates entries and an absolute Windows drive
colon cannot be expressed - so a mods folder is used instead.

Also relevant, both already handled in `build.gradle`:

* **Do not put Flywheel / Ponder on `additionalRuntimeClasspath`.** They also
  ship inside Create's `META-INF/jarjar/`; having them in both places creates
  duplicate JPMS modules and the run dies with
  `ResolutionException: modules ... and ... export package ...`.
* **Create's runtime jar is separate** (`createDevRuntime` configuration,
  `transitive = false`) because the compile dependency is the slim artifact,
  which is not a loadable mod file.

---

## 6. Declared dependencies

`create`, `createbigcannons` and `ritchiesprojectilelib` are **required**;
`sable`, `aeronautics`, `simulated`, `offroad`, `copycats`, `framedblocks` and
`curios` are **optional**, mirroring how Create Big Cannons declares them.
`imgui-mc` is not declared at all.

Version ranges live in `gradle.properties` and are single-sourced into
`neoforge.mods.toml` through Gradle `expand`. Change them in one place only.

---

## 7. Layout

```
createbigcannons-theroarofsteel/
|- build.gradle                      ModDevGradle build; MODE A/B deps, run config, workarounds
|- settings.gradle                   plugin repos + Java 21 toolchain resolver
|- gradle.properties                 all versions and mod metadata (single source of truth)
|- gradle/wrapper/                   wrapper (jar committed)
|- gradlew / gradlew.bat
|- libs/                             local dependency jars (see section 4)
|- run/ , run-server/                dev run game dirs (gitignored)
|- _tools/
|  |- fetch_local_libs.ps1           copies workspace jars + extracts Create's bundled libs
|  |- scaffold.py                    generator for this skeleton (kept for re-runs)
|  \- verify_scaffold.py             offline structural self-check
\- src/main/
   |- java/com/cbcrs/
   |  |- TheRoarOfSteel.java         @Mod entry point, MOD_ID + NAMESPACE
   |  |- TheRoarOfSteelRegistrate.java   the single CreateRegistrate instance
   |  |- CBSReference.java           id helpers, cannon_/autocannon_ prefix split
   |  |- index/
   |  |  |- CBSIndex.java            the one place registration order is visible
   |  |  |- CBSItems.java            the tungsten ingot
   |  |  |- CBSMaterials.java        raw/crushed tungsten + tungsten nugget
   |  |  |- CBSOres.java             scheelite/wolframite ore blocks
   |  |  |- CBSTabs.java             dedicated creative tab
   |  |  |- CBSTags.java             caliber tag keys (cannon + autocannon)
   |  |  \- package-info.java        registration conventions for later phases
   |  |- gametest/                   explicit game-test registration + tests
   |  |- client/TheRoarOfSteelClient.java
   |  |- mixin/package-info.java     + cbc_rs(.client).mixins.json
   |  \- datagen/package-info.java   datagen package (data run configured)
   \- resources/
      |- META-INF/neoforge.mods.toml (ASCII-only, see 3a)
      |- pack.mcmeta
      |- cbc_rs.mixins.json / cbc_rs.client.mixins.json
      |- assets/cbc_rs/lang/zh_cn.json   (en_us/en_ud are generated)
      |- assets/cbc_rs/textures/item/*.png    (6 item icons)
      |- assets/cbc_rs/textures/block/*.png   (2 ore textures)
      |- data/cbc_rs/structure/empty.nbt      (game-test template)
      |- data/cbc_rs/tags/{block,item}/{cannon_caliber,autocannon_caliber}/*.json
      |- data/c/tags/item/{raw_materials,crushed_raw_materials,nuggets}/...
      \- data/minecraft/tags/{block,item}/..._ores.json
```

### Ore world generation, loot and recipes

| Piece | Where | Notes |
| --- | --- | --- |
| Configured features | `data/cbc_rs/worldgen/configured_feature/` | `minecraft:ore`; scheelite targets `stone_ore_replaceables` only, wolframite `deepslate_ore_replaceables` only (hard layer exclusion) |
| Placed features | `data/cbc_rs/worldgen/placed_feature/` | scheelite main + buried, wolframite main + secondary |
| Biome modifier | `data/cbc_rs/neoforge/biome_modifier/` | `neoforge:add_features`, `#minecraft:is_overworld`, step `underground_ores` |
| Ore loot | generated from `CBSOres` | vanilla ore table via `createOreDrop`: Silk Touch -> block, otherwise exactly 1 raw ore, `ore_drops` scales with Fortune |
| Recipes | `data/cbc_rs/recipe/` | 20 files: 6 smelting, 6 blasting, 4 crushing, 2 splashing, 2 crafting |

Generation values (from decisions 15/17):

| Feature | Count | Height |
| --- | --- | --- |
| `scheelite_ore_main` | 2 | trapezoid -32..32 |
| `scheelite_ore_buried` | 4 | uniform -64..64 |
| `wolframite_ore_main` | 4 | trapezoid -64..32 |
| `wolframite_ore_secondary` | 10 | uniform -48..-16 |

> **The height provider type is `minecraft:trapezoid`, not `minecraft:triangle`.**
> Vanilla's `HeightRangePlacement.triangle(...)` serialises as `minecraft:trapezoid`;
> writing `minecraft:triangle` fails registry loading with
> `Unknown registry key .../height_provider_type: minecraft:triangle`.

### Loot tables must be generated, not hand-written

`src/generated/resources` is an **earlier** resource root than
`src/main/resources`, so a hand-written loot table under
`src/main/resources/data/cbc_rs/loot_table/...` is silently shadowed by whatever
datagen produces. That is not a guess - the ore dropped itself while the
hand-written table was present, and a game test caught it. The ore tables are
therefore produced by `CBSOres` via `.loot(...)` /
`loot.createOreDrop(...)`, and no hand-written ore loot table exists.

Two related traps in that code:

* **Do not call `BuiltInRegistries.ITEM` inside the loot lambda's setup path** if
  it runs during registration - resolving an item eagerly triggered
  `ExceptionInInitializerError` at mod load. Pass a lazy `NonNullSupplier` and
  call `.get()` inside the consumer instead.
* `RegistrateBlockLootTables#createOreDrop` needs the raw ore as an `Item`, and
  taking it from `CBSMaterials` directly would force that class to initialise
  during `CBSOres`'s own initialisation.

### Content conventions for ores and materials

* **Item icons are cut-out sprites in `textures/item/`; block textures are fully
  opaque 16x16 images in `textures/block/`.** Both the game tests and the client
  asset assertion check this, so an item icon without transparency or a block
  texture that is mostly transparent is a mistake.
* **The crushed items follow Create's `crushed_raw_*` naming**
  (`crushed_raw_scheelite` / `crushed_raw_wolframite`), so the registry ids, the
  texture file names and the workspace source PNGs all use the same spelling.
  This was changed from the earlier `crushed_<material>` ids to match Create.
* **Never register a second tungsten ingot.** Both ore chains must converge on
  `cbc_rs:tungsten_ingot`; the `exactlyOneTungstenIngotExists` game test guards
  this.
* The block mining tags (`mineable/pickaxe`, `needs_stone_tool`) are produced by
  datagen via `BlockBuilder.tag(...)`. The `*_ores` tag files under
  `data/minecraft/tags/**` are hand-written. Shared namespaces (`minecraft`, `c`)
  are permitted **for tags only**, and the verifier enforces that.
* The client asset assertion runs on every `runClient`; grep the log for
  `ASSET` and expect `ASSET SUMMARY textures: 8 present, 0 missing` plus
  `ASSET SUMMARY models: 8 ok, 0 failed`. Note that **block items report zero
  null-side quads** - block models key their quads by face, so a check that only
  reads `getQuads(null, null, null)` wrongly fails every block item (vanilla
  stone included).

---

## 8. Namespace separation policy (design decision 7)

`CBSReference` enforces the split at the id level:

* big-cannon content -> `CBSReference.cannon("...")` -> `cbc_rs:cannon_*`
* autocannon content -> `CBSReference.autocannon("...")` -> `cbc_rs:autocannon_*`

Caliber tags follow the same split: `cannon_caliber/*` and
`autocannon_caliber/*` never share entries. The words "small caliber" exist in
both halves of the mod, and this is what keeps them distinguishable.

---

## 9. What is **not** included yet

Per the feasibility report, these are later phases and are intentionally absent:

* the rest of the tungsten chain (ore, raw tungsten, nuggets, tungsten steel,
  molten tungsten steel, cast moulds) and every crafting recipe
* the powder-charge rename and the tungsten material registration
* the bar-based pressure model and the replacement spread-angle mechanism
* charge slots and the charge-bonus component
* auxiliary rammer, clip, chain loader
* the wireless-redstone detonator
* any mixin into Create Big Cannons internals (the mixin configs are empty)
* caliber *contents* (blocks/items) - only the tag skeleton exists

---

## 10. Known risks

Risks that are outside this mod's own code but will affect how it must be
developed and tested. Each entry says what to look for and when to revisit it.

### K1 - Sable and Flywheel conflict on shaders and lighting

Sable declares this itself at mod loading:

> NOTE: Sable is loaded with Flywheel. Sable contains extensive shader overrides
> and a full light-storage replacement. Expect this to cause compatibility
> issues. If issues arise, please report them to the Sable issue tracker instead
> of the Flywheel issue tracker.

Both mods are mandatory for this project (Sable backs Create Aeronautics), so
the conflict cannot be removed by configuration.

* **Impact:** any in-game visual or lighting anomaly must be triaged against this
  first, before our own rendering code is suspected.
* **Observed symptom:** one `ERROR` from Sable on joining a world -
  "Received a sub-level movement packet for a non-existent sub-level". It did not
  crash the session.
* **Revisit:** Phase 4, where the feasibility report requires a Sable sub-level
  regression pass (the pressure/spread rework touches cannon behaviour inside
  contraptions). Escalate if the error repeats or correlates with a crash.

### K2 - Optional-integration mixins log warnings when absent

Create Big Cannons and Veil guard their optional-compat mixins; with
`framedblocks` / `iris` not installed they log `ClassNotFoundException` warnings
at startup. Harmless, expected, and **not** a reason to add those mods.

### K3 - Datagen template expansion is not Unicode-safe

Files processed through Gradle `expand()` must stay ASCII-only (see section 3a).
This is not a runtime risk but it silently corrupts content if ignored, so it is
tracked here as a standing constraint.

---

## 11. License

MIT, matching Create Big Cannons' code license. Create Big Cannons' assets are
CC BY-NC-SA: do not copy its textures or models into this project.
