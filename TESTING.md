# 测试流程与基准
# Testing guide and baselines

This document is the reference for **every rendering- and item-related test** in
this project. It records the procedure, the success criteria, the exact
environment a baseline was measured in, and the known compatibility noise so
that later failures can be attributed correctly instead of being investigated
from scratch.

It exists because the first item (the tungsten ingot) required several rounds of
trial and error to test properly. Everything learned in that round is captured
below.

---

## 1. Why this file exists

"Nothing appeared in the log" is **not** evidence that a texture or model is
correct. Minecraft silently substitutes the missing-texture sprite and the
missing model, so a broken asset can look error-free. Every asset test in this
project must therefore assert **positively** that the asset was found.

The checks described here are implemented in
`src/main/java/com/cbcrs/client/TheRoarOfSteelClient.java` and run automatically
on every client start.

---

## 2. Test layers

Use the cheapest layer that can actually answer the question.

| Layer | Command | Answers | Needs |
| --- | --- | --- | --- |
| Structural | `python ./_tools/verify_scaffold.py` | Are files present, ids consistent, JSON valid, textures real PNGs | nothing |
| Build | `./gradlew build` | Does it compile, and does the jar contain the assets | JDK 21 |
| Datagen | `./gradlew runData` | Do models and en_us lang get generated correctly | JDK 21 |
| Headless in-game | `./gradlew runGameTestServer` | Is the item registered and usable in a running server | JDK 21 |
| Client assets | `./gradlew runClient` | Do the texture and model actually load and bake | JDK 21 + display |
| Manual | in the client window | Does it *look* right | a human |

`runServer`, `runData` and `runGameTestServer` all share the `run/` game
directory, and `syncMods` stages the external mods into `run/mods`
automatically, so no manual setup is needed for any of them.

### Baseline command sequence

```bash
python ./_tools/verify_scaffold.py
./gradlew clean build runData
./gradlew runGameTestServer      # expect: All 3 required tests passed
./gradlew runClient              # expect: the ASSET lines below, then inspect by eye
```

---

## 3. Client asset assertions

On client start the mod asserts three things and logs them. **Grep for `ASSET`
after a client run.**

| Line | Meaning | Pass condition |
| --- | --- | --- |
| `ASSET OK texture resource found: <path> (pack: <pack>)` | The PNG is served by the resource manager | path is `cbc_rs:textures/item/<name>.png`, pack is `mod/createbigcannons_theroarofsteel` |
| `ASSET OK model baked: class=..., quads=N, sprite=...` | The item model baked with real geometry | class is not the missing model, `quads > 0`, sprite id is `<namespace>:item/<name>` |
| `ASSET PIXELS size=WxH decodedRGBA_sha256=... pngBytes_sha256=...` | Pixel fingerprint for offline comparison | see section 4 |

Any `ASSET FAIL` line is a hard failure: the asset fell back to the
missing-texture or missing-model path.

### Why the pixel fingerprint exists

To prove the rendered sprite really comes from our file and not from a
same-named fallback, the hash of the sprite's decoded pixels is logged next to
the hash of the PNG bytes on disk. The two are related by a known, reproducible
transformation:

* `pngBytes_sha256` must equal `sha256(<the .png file>)` — computed with any
  external tool.
* `decodedRGBA_sha256` is the hash of the 16x16 pixel stream in **BGRA** order,
  even though the log label says RGBA.

> **Pitfall — channel order.** `NativeImage#getPixelRGBA` returns an
> ABGR-packed int, so extracting it as `R,G,B,A` produces a **BGRA** byte
> stream. Comparing that hash against a PIL-computed **RGBA** hash will
> "mismatch" even though the texture is byte-identical. To verify offline,
> compute the BGRA-ordered hash:
>
> ```python
> from PIL import Image; import hashlib
> im = Image.open("tungsten_ingot.png").convert("RGBA")
> buf = bytearray()
> for y in range(im.height):
>     for x in range(im.width):
>         r, g, b, a = im.getpixel((x, y))
>         buf += bytes((b, g, r, a))          # BGRA
> print(hashlib.sha256(bytes(buf)).hexdigest())
> ```

### Pitfalls that cost time (do not repeat)

1. **Item models are not baked when `TextureAtlasStitchedEvent` fires.** Calling
   `ItemRenderer#getModel` from that event throws an NPE *inside vanilla* and
   aborts the entire resource reload. The texture check runs at stitch time; the
   model check waits for a later client tick (`ClientTickEvent.Post`).
2. **Sprite ids use the atlas form**, `cbc_rs:item/tungsten_ingot`, not the file
   path form `textures/item/tungsten_ingot.png`. Comparing the wrong form
   produces a false failure.
3. **Block items report zero null-side quads.** A cube model keys its quads by
   face, so `BakedModel#getQuads(null, null, null)` returns an empty list for
   every block item - vanilla stone included. A check that reads only that list
   wrongly fails every ore block. Collect the null-side list **plus all six
   `Direction` values** before judging. The client assertion logs a control for
   vanilla stone for exactly this reason:
   `ASSET CONTROL vanilla stone: ... nullSideQuads=0 downQuads=1`.
4. **`server.properties` resets** if you delete the run directory; the game test
   server needs `eula.txt` with `eula=true` and will otherwise fail at start.

---

## 4. Baseline: client session 2026-10-10 (tungsten ingot)

Measured with the first item and no other content.

| | |
| --- | --- |
| Client launch | `./gradlew runClient` |
| Session window | 08:39:16 – 08:47:04 (clean exit, `Minecraft/]: Stopping!`) |
| JDK | Microsoft OpenJDK 21.0.3+9-LTS |
| Gradle | 8.12 |
| NeoForge | 21.1.228 |
| GL | 4.6.0 NVIDIA 595.97 (RTX 4060 Laptop) |
| Renderer mixins | `Using Vanilla renderer mixins` (Sable) |
| Mods loaded | 15 (Create 6.0.10, CBC 5.11.7, RPL 2.1.2, Sable 2.0.6, Aeronautics 1.3.2, Flywheel 1.0.6, Ponder 1.0.82, Veil 4.3.2, + Simulated/Offroad/Sable Companion/aeronautics_bundled, Minecraft, NeoForge) |

### Recorded asset lines

```
ASSET OK   texture resource found: cbc_rs:textures/item/tungsten_ingot.png
           (pack: mod/createbigcannons_theroarofsteel)
ASSET OK   model baked: class=SimpleBakedModel, quads=26, sprite=cbc_rs:item/tungsten_ingot
ASSET PIXELS size=16x16
           decodedRGBA_sha256=a6676ed665552f198b1455aa5d95a7e16daf8223cd94349779bd86613881f85f
           pngBytes_sha256=2a241c6a42068d5da12ae248d6bd2f6203001b184dd31acb54e334d8d7f68cb0
```

### Error budget for this baseline

| Metric | Observed | Expected for every later session |
| --- | --- | --- |
| Errors from this mod | **0** | 0 |
| Warnings from this mod | **0** (only the two dev-only refmap notices) | same |
| TOTAL errors in session | 1, from Sable (see section 5) | should stay at most the known noise |
| Crash reports | none from this run | none |

### Reference hashes for the tungsten ingot texture

| What | Value |
| --- | --- |
| `pngBytes_sha256` | `2a241c6a42068d5da12ae248d6bd2f6203001b184dd31acb54e334d8d7f68cb0` |
| `decodedRGBA_sha256` (BGRA order) | `a6676ed665552f198b1455aa5d95a7e16daf8223cd94349779bd86613881f85f` |
| decodedRGBA in true RGBA order (PIL) | `f81b0e1aa50c50cc9c5abcdc96478813e41f67d7bc03a808b6ba8b4fe65557e3` |

If a future change alters the rendered texture, `decodedRGBA_sha256` changes. If
only the PNG's compression changes, `pngBytes_sha256` changes but the decoded
hash does not.

**Manual confirmation:** the tungsten ingot was inspected in the creative tab in
this session and rendered correctly.

---

## 4b. Baseline: tungsten ores and materials (decisions 15/16)

Eight items and two ore blocks were added on top of the section 4 baseline.

| | |
| --- | --- |
| Content | `tungsten_ingot`, `tungsten_nugget`, `raw_scheelite`, `raw_wolframite`, `crushed_raw_scheelite`, `crushed_raw_wolframite`, `scheelite_ore` (block), `wolframite_ore` (block) |
| Game tests | `./gradlew runGameTestServer` -> **All 5 required tests passed** |
| Client assets | `./gradlew runClient` -> `ASSET SUMMARY textures: 8 present, 0 missing` and `ASSET SUMMARY models: 8 ok, 0 failed` |
| Errors from this mod | 0 (only the two dev-only refmap notices) |
| `raw_wolframite.png` sha256 | `ad86532a9a91593558c4c2a2ae50e9dd5607e3032a04308ad47f08ae32c5fc99` (refreshed texture) |

### Recorded asset lines

```
ASSET OK   texture found: cbc_rs:textures/item/tungsten_ingot.png    (pack: mod/createbigcannons_theroarofsteel)
ASSET OK   texture found: cbc_rs:textures/item/tungsten_nugget.png
ASSET OK   texture found: cbc_rs:textures/item/raw_scheelite.png
ASSET OK   texture found: cbc_rs:textures/item/raw_wolframite.png
ASSET OK   texture found: cbc_rs:textures/item/crushed_raw_scheelite.png
ASSET OK   texture found: cbc_rs:textures/item/crushed_raw_wolframite.png
ASSET OK   texture found: cbc_rs:textures/block/scheelite_ore.png
ASSET OK   texture found: cbc_rs:textures/block/wolframite_ore.png
ASSET SUMMARY textures: 8 present, 0 missing

ASSET CONTROL vanilla stone: class=SimpleBakedModel nullSideQuads=0 downQuads=1
ASSET OK   model baked: tungsten_ingot      quads=26 sprite=cbc_rs:item/tungsten_ingot
ASSET OK   model baked: tungsten_nugget     quads=16 sprite=cbc_rs:item/tungsten_nugget
ASSET OK   model baked: raw_scheelite       quads=27 sprite=cbc_rs:item/raw_scheelite
ASSET OK   model baked: raw_wolframite      quads=25 sprite=cbc_rs:item/raw_wolframite
ASSET OK   model baked: crushed_raw_scheelite   quads=19 sprite=cbc_rs:item/crushed_raw_scheelite
ASSET OK   model baked: crushed_raw_wolframite  quads=23 sprite=cbc_rs:item/crushed_raw_wolframite
ASSET OK   model baked: scheelite_ore       quads=6  sprites=[cbc_rs:block/scheelite_ore]
ASSET OK   model baked: wolframite_ore      quads=6  sprites=[cbc_rs:block/wolframite_ore]
ASSET SUMMARY models: 8 ok, 0 failed
```

### Texture conventions verified for these assets

| Kind | Folder | Alpha | Notes |
| --- | --- | --- | --- |
| Item icon | `textures/item/` | has transparent pixels | cut-out sprite on `item/generated` |
| Block texture | `textures/block/` | fully opaque | `cube_all`, `block/<name>` sprite id |

Decoded alpha profile measured for each file: item icons had 121-215 fully
transparent pixels, block textures had 0. The structural verifier re-checks both
the PNG signature and the 16x16 dimensions.

### Pitfall: renaming a registry id breaks existing worlds

The crushed items were renamed from `crushed_scheelite` / `crushed_wolframite` to
Create's `crushed_raw_scheelite` / `crushed_raw_wolframite`. Items already saved
in a world or player inventory under the old ids no longer resolve, and the log
reports one error per stack:

```
[Server thread/ERROR] [minecraft.world.item.ItemStack/]:
Tried to load invalid item: 'Unknown registry key in ResourceKey[
minecraft:root / minecraft:item]: cbc_rs:crushed_scheelite'
```

This is expected fallout from a registry-id rename and is **not** a mod defect -
the code, assets and tags are all consistent under the new ids. It matters only
for **worlds that already contain the old items**. Because the content has not
been released, the practical fix is to regenerate the dev world (or delete the
stacks) rather than add a data-fix. If an id rename ever happens after release,
it needs a proper migration instead.

### Deliberately absent (next step)

* Ore **world generation** (configured/placed features, biome modifiers).
* All **recipes**: crushing, washing, smelting/blasting, the 9-in-1 nugget recipe.
* Ore **loot tables**. Until these exist the ore blocks drop nothing, so they
  cannot be obtained in survival - the game tests only prove they are
  registered, placeable and rendered.

---

## 4c. Baseline: ore generation, loot and recipes (decisions 15-17)

| | |
| --- | --- |
| Game tests | `./gradlew runGameTestServer` -> **All 12 required tests passed** |
| Client assets | `ASSET SUMMARY textures: 8 present, 0 missing` / `models: 8 ok, 0 failed` |
| Recipe count | `Loaded 3111 recipes` = 3091 baseline + 20 ours |
| Errors from this mod | 0 |
| Structural verifier | 235 checks pass |

### What the 12 tests cover

| Test | Proves |
| --- | --- |
| `tungstenIngotIsRegistered` | the original ingot still registers |
| `tungstenMaterialsAreRegistered` | nugget + 2 raw + 2 crushed register and round-trip |
| `tungstenOreBlocksAreRegistered` | both ore blocks register **and** have working block items |
| `allTungstenItemsStackProperly` | every item makes a valid 64-stack |
| `exactlyOneTungstenIngotExists` | decision 16: only one tungsten ingot in the game |
| `oresDropExactlyOneRawOreWithoutFortune` | exactly 1 raw ore, no Fortune/Silk Touch |
| `oresDropThemselvesWithSilkTouch` | Silk Touch yields the ore block |
| `oresScaleWithFortune` | Fortune scales, sampled 200x |
| `fortuneToolAndVanillaControlBehave` | our ores match vanilla iron ore's Fortune range |
| `oreWorldGenIsRegistered` | all 2 configured + 4 placed features exist in the registries |
| `allRecipesLoadWithoutErrors` | `hadErrorsLoading()` false, 20 recipes, 3111 total |
| `nuggetAndIngotConvertBothWays` | 9 nuggets -> ingot and 1 ingot -> 9 nuggets |

### Fortune is random - sample it, do not assert one roll

Vanilla's `minecraft:ore_drops` formula is:

```
extra = nextInt(level + 2) - 1   (clamped at 0)
total = count * (extra + 1)
```

| Fortune | `nextInt(n)` | possible totals |
| --- | --- | --- |
| I | 3 | 1 or 2 |
| II | 4 | 1, 2 or 3 |
| III | 5 | 1, 2, 3 or 4 |

So a **single roll proves nothing**: at Fortune II, "dropped 1" is a legal
outcome. An early version of the test asserted one roll and failed by chance,
which looked exactly like a broken loot table. The tests now sample
`FORTUNE_SAMPLES = 200` rolls and assert the maximum, plus a vanilla iron-ore
control on the same tool. If a Fortune assertion ever fails, check the sample
size before suspecting the loot table.

### Traps found while wiring this up (each cost real debugging time)

1. **`src/generated/resources` is ordered before `src/main/resources`.** A
   hand-written ore loot table is silently shadowed by datagen output. The ore
   dropped itself even though the hand-written JSON was correct and present in
   the jar. Loot must be produced by datagen (`CBSOres` `.loot(...)`).
2. **`minecraft:triangle` is not a height provider type.** Vanilla's
   `HeightRangePlacement.triangle()` serialises as `minecraft:trapezoid`.
   Writing `triangle` aborts registry loading.
3. **A relative path that already starts with `data/` must not be prefixed
   again**, or the files land in `data/data/...` and load silently as nothing.
   The symptom was "0 recipes under cbc_rs" plus ores dropping themselves.
4. **Do not resolve an `Item` from `BuiltInRegistries` inside the loot provider
   setup.** It triggers `ExceptionInInitializerError` at mod load. Use a lazy
   supplier and call `.get()` inside the consumer.
5. **`ItemStack#enchant` refuses enchantments the item does not support** and
   fails silently, so the loot function sees level 0. Set
   `DataComponents.ENCHANTMENTS` directly in tests.
6. **A recipe id includes its subfolder**: `data/cbc_rs/recipe/crafting/x.json`
   has the id `cbc_rs:crafting/x`, not `cbc_rs:x`.

---

## 5. Known compatibility noise (do not chase these)

These are present in a healthy run. They are recorded so they are not
misdiagnosed as regressions. Anything not on this list is worth investigating.

### 5.1 Sable + Flywheel shader/lighting conflict — `WARN`, expected

Emitted once at mod loading (verbatim):

```
[modloading-worker-0/WARN] [dev.ryanhcode.sable.Sable/]:
NOTE: Sable is loaded with Flywheel. Sable contains extensive shader overrides
and a full light-storage replacement. Expect this to cause compatibility issues.
If issues arise, please report them to the Sable issue tracker
(https://github.com/ryanhcode/sable/issues) instead of the Flywheel issue tracker.
```

This is Sable declaring the conflict itself. Both mods are required by this
project, so it cannot be avoided. **Any visual or lighting anomaly in-game should
be considered suspect for this cause first**, before blaming our own rendering.

See also README section 11, where this is tracked as known risk K1.

### 5.2 Sable sub-level packet error — `ERROR`, known, not ours

Observed once, at the moment of joining a world:

```
[Render thread/ERROR] [dev.ryanhcode.sable.Sable/]:
Received a sub-level movement packet for a non-existent sub-level
```

It did not crash the session; the game ran and exited normally afterwards. It is
very likely a symptom of 5.1. **Treat as known noise for now**, but if it starts
appearing repeatedly, or correlates with a crash, it becomes a real issue —
revisit it at Phase 4, where the feasibility report requires a Sable sub-level
regression pass.

### 5.3 Mixin refmap notices for our own configs — `WARN`, expected

```
Reference map 'cbc_rs.refmap.json' for cbc_rs.mixins.json could not be read.
If this is a development environment you can ignore this message
```

Expected while our mixin configurations are empty (no refmap is generated). It
should disappear once real mixins exist. Not a defect.

### 5.4 Mixin targets for mods that are not installed — `WARN`, expected

```
Error loading class: xfacthd/framedblocks/... (ClassNotFoundException)
@Mixin target ... was not found createbigcannons.mixins.json:... from mod createbigcannons
Error loading class: net/irisshaders/iris/pipeline/PipelineManager (ClassNotFoundException)
@Mixin target ... was not found veil.neoforge.mixins.json:... from mod veil
```

`framedblocks`, `iris` and `jetbrains annotations` are not installed. Create Big
Cannons and Veil guard their optional-compat mixins and log when the target is
absent. Harmless.

### 5.5 Vanilla / loader chatter — `WARN`, expected

* `Ambiguity between arguments [teleport, ...]` — vanilla command argument
  ambiguity, always present.
* `Assets URL 'union:/...' uses unexpected schema` — dev-environment asset path
  notice.
* `Advanced terminal features are not available in this environment` — Gradle
  console only.

---

## 6. Checklist for a new item or block

1. Put the texture at
   `src/main/resources/assets/cbc_rs/textures/item/<name>.png`
   (16x16, RGBA). For blocks, the block texture goes to `textures/block/`.
2. Register it in the matching `CBS*` index class with `.lang(...)` for the
   English name; add the Chinese name to
   `src/main/resources/assets/cbc_rs/lang/zh_cn.json`.
3. Add a model: either `.model(...)` in the builder, or a hand-written JSON under
   `assets/cbc_rs/models/item/<name>.json` modelled on the generated one.
4. `./gradlew runData` and confirm the model + en_us entries appear under
   `src/generated/resources/`.
5. Add or extend a game test under `src/main/java/com/cbcrs/gametest/` and
   register it in `CBSGameTests` (exactly **one** registration route — using both
   `GameTestRegistry.register` and `RegisterGameTestEvent` runs every test twice).
6. `python ./_tools/verify_scaffold.py` — extend it if the item introduces a new
   asset kind.
7. `./gradlew build runData runGameTestServer` — expect all tests passed.
8. `./gradlew runClient` — grep the log for `ASSET` and confirm three `ASSET OK`
   lines; then look at it by eye.
9. Record a new baseline row in section 4 with the new hashes.

---

## 6b. Repository state and dev-world resets

* **No git repository yet, by decision.** The project directory is intentionally
  unversioned until ore **world generation** and the **recipes** are written; the
  first commit will be made at that point. Until then the only version safety net
  is the `_versions/` folder in the workspace root, which holds the pre-change
  copies of the feasibility report - it does **not** cover the mod source.
* **Dev worlds get deleted after a registry-id rename.** The two dev worlds were
  the client save `run/saves/新的世界` and the integrated-server world `run/world`;
  both held stacks saved under the pre-rename crushed ids. Deleting them is the
  cheap fix. A fresh world is generated on the next launch.
* Deleting `run/world` and `run/saves/**` leaves `eula.txt`, `options.txt`,
  `config/`, `mods/` and `logs/` intact, so the next run needs no re-setup.


---

## 7. Log locations

All under `run/logs/` (gitignored):

| File | Contents |
| --- | --- |
| `latest.log` | the just-finished session |
| `debug.log` | same session with DEBUG detail — use this one when hunting asset problems |
| `debug-N.log.gz`, `<date>-N.log.gz` | rotated history |

`run/crash-reports/` holds crash reports. A crash report from an earlier failed
experiment can linger, so always check its timestamp before attributing it to
the run you are investigating.

---

## 8. Test templates: recipes and loot tables

Copy-paste starting points for the two kinds of test that are easy to get wrong.
Both are written as NeoForge game tests; see sections 2 and 4c for how to register
and run them.

### 8.1 Why these two need templates at all

* A **recipe** can be present but fail to parse, and the failure is only reported
  when the server loads. Asserting on `RecipeManager#hadErrorsLoading()` plus an
  exact id lookup is what actually proves a recipe works.
* A **loot table** is evaluated with a randomised `LootContext`. Asserting one
  roll is meaningless (section "Fortune is random"), and a hand-written table can
  be silently shadowed by datagen output. The template below samples and uses a
  vanilla control.

### 8.2 Recipe template

Counts to assert: the number of our recipe ids, the total recipe count, and the
exact ids. The total is worth asserting because a file that silently fails to
parse disappears from the count.

```java
@GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
public static void allRecipesLoadWithoutErrors(GameTestHelper helper) {
    var manager = helper.getLevel().getRecipeManager();

    // A malformed JSON is reported here, not at datagen time.
    if (manager.hadErrorsLoading()) {
        helper.fail("RecipeManager reported errors while loading recipes");
        return;
    }

    int ours = 0;
    for (var holder : manager.getRecipes()) {
        if (TheRoarOfSteel.NAMESPACE.equals(holder.id().getNamespace())) {
            ours++;
        }
    }
    if (ours != EXPECTED_OUR_RECIPES) {
        helper.fail("Expected " + EXPECTED_OUR_RECIPES + " recipes under "
                + TheRoarOfSteel.NAMESPACE + " but found " + ours);
        return;
    }

    // Measured baseline: an unmodified instance loaded 3091 recipes.
    int total = manager.getRecipes().size();
    if (total < 3091 + EXPECTED_OUR_RECIPES) {
        helper.fail("Total recipe count is " + total + ", expected at least "
                + (3091 + EXPECTED_OUR_RECIPES) + " - some recipe files did not load");
        return;
    }

    // Exact ids, so a rename or a dropped file is caught rather than merely
    // shifting the count. NOTE: the subfolder IS part of the id -
    // data/cbc_rs/recipe/crafting/x.json has the id cbc_rs:crafting/x.
    for (String path : new String[]{"crafting/example", "crushing/example"}) {
        ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath(TheRoarOfSteel.NAMESPACE, path);
        if (manager.byKey(id).isEmpty()) {
            helper.fail("Recipe not loaded: " + id);
            return;
        }
    }
    helper.succeed();
}
```

Recipe counts by kind are also worth asserting, so a whole category cannot
vanish silently:

```java
// 6 smelting + 6 blasting + 4 crushing + 2 splashing + 2 crafting = 20
for (Map.Entry<String, Integer> e : Map.of(
        "smelting", 6, "blasting", 6, "crushing", 4,
        "splashing", 2, "crafting", 2).entrySet()) {
    Path dir = ROOT / "src/main/resources/data/cbc_rs/recipe" / e.getKey();
    long got = Files.list(dir).filter(p -> p.toString().endsWith(".json")).count();
    if (got != e.getValue()) {
        helper.fail(e.getKey() + " has " + got + " recipes, expected " + e.getValue());
        return;
    }
}
```

**Asserting a crafting result** (works for vanilla shaped/shapeless; the
`CraftingInput` must match the recipe's grid, so a 3x3 recipe needs a 3x3 input):

```java
ItemStack nugget = new ItemStack(CBSMaterials.TUNGSTEN_NUGGET.get());
CraftingInput grid = CraftingInput.of(3, 3, List.of(
        nugget, nugget, nugget,
        nugget, nugget, nugget,
        nugget, nugget, nugget));
var found = manager.getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
if (found.isEmpty()) {
    helper.fail("9 nuggets do not craft anything");
    return;
}
ItemStack result = found.get().value().assemble(grid, helper.getLevel().registryAccess());
if (result.getItem() != CBSItems.TUNGSTEN_INGOT.get() || result.getCount() != 1) {
    helper.fail("9 nuggets produced " + result.getCount() + "x"
            + BuiltInRegistries.ITEM.getKey(result.getItem()));
    return;
}
```

For a 1x1 shapeless recipe use `CraftingInput.of(1, 1, List.of(stack))`.

**Create's own machine recipes** (`create:crushing`, `create:splashing`) do not
have a vanilla recipe-input type, so there is no `getRecipeFor` to call. Verify
them the way section 4c does: assert the files exist, that
`hadErrorsLoading()` is false, and that the total recipe count includes them.
For the numbers inside them, assert on the parsed JSON in the structural
verifier (`_tools/verify_scaffold.py`) instead - for example that
`crushing/wolframite_ore.json` contains `"count": 2` and
`"processing_time": 350`, or that `splashing/crushed_raw_wolframite.json`
contains `"chance": 0.15` and `"chance": 0.02`.

### 8.3 Loot table template

The pattern is: build a tool, call `Block.getDrops(...)` with a mock player, and
judge the resulting stacks. `BlockPos.ZERO` is right because the game test
structure's origin is the block under test.

```java
private static List<ItemStack> drops(GameTestHelper helper, String orePath, ItemStack tool) {
    Block block = BuiltInRegistries.BLOCK.get(
            ResourceLocation.fromNamespaceAndPath(TheRoarOfSteel.NAMESPACE, orePath));
    return Block.getDrops(block.defaultBlockState(), helper.getLevel(),
            helper.absolutePos(BlockPos.ZERO), null,
            helper.makeMockPlayer(GameType.SURVIVAL), tool);
}

private static int totalCount(List<ItemStack> stacks) {
    int total = 0;
    for (ItemStack s : stacks) { total += s.getCount(); }
    return total;
}
```

**Building an enchanted tool - do NOT use `ItemStack#enchant`:**

```java
private static ItemStack enchantedTool(GameTestHelper helper, String enchantPath, int level) {
    ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
    HolderLookup.RegistryLookup<Enchantment> enchants =
            helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    Holder<Enchantment> enchantment = enchants.getOrThrow(
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.withDefaultNamespace(enchantPath)));

    // ItemStack#enchant refuses enchantments the item does not support and fails
    // SILENTLY, so the loot function then sees level 0. Set the component instead.
    ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    mutable.set(enchantment, level);
    tool.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    return tool;
}
```

**Deterministic cases** (no Fortune involved, so one roll is fine):

```java
@GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 100)
public static void oresDropExactlyOneRawOreWithoutFortune(GameTestHelper helper) {
    List<ItemStack> got = drops(helper, "scheelite_ore", new ItemStack(Items.NETHERITE_PICKAXE));
    if (got.size() != 1 || got.get(0).getItem() != CBSMaterials.RAW_SCHEELITE.get()
            || got.get(0).getCount() != 1) {
        helper.fail("without Fortune dropped " + describe(got) + ", expected exactly 1 raw ore");
        return;
    }
    helper.succeed();
}
```

**Silk Touch**: must yield the block itself, not the drop.

```java
List<ItemStack> got = drops(helper, "scheelite_ore", enchantedTool(helper, "silk_touch", 1));
Block block = /* the ore block */;
if (got.size() != 1 || got.get(0).getItem() != block.asItem()) {
    helper.fail("Silk Touch dropped " + describe(got) + ", expected the block itself");
    return;
}
```

**Fortune**: sample, and include a vanilla control. A single roll proves nothing.

```java
private static final int FORTUNE_SAMPLES = 200;

@GameTest(templateNamespace = TheRoarOfSteel.NAMESPACE, template = "empty", timeoutTicks = 400)
public static void oresScaleWithFortune(GameTestHelper helper) {
    int plain = 0;
    for (int i = 0; i < FORTUNE_SAMPLES; i++) {
        plain = Math.max(plain, totalCount(drops(helper, "scheelite_ore",
                new ItemStack(Items.NETHERITE_PICKAXE))));
    }
    if (plain != 1) {
        helper.fail("without Fortune the maximum was " + plain + ", must be exactly 1");
        return;
    }

    ItemStack fortune3 = enchantedTool(helper, "fortune", 3);
    int boosted = 0;
    for (int i = 0; i < FORTUNE_SAMPLES; i++) {
        boosted = Math.max(boosted, totalCount(drops(helper, "scheelite_ore", fortune3)));
    }
    // ore_drops at level 3 can reach 4; never exceeding the plain result over 200
    // rolls means Fortune is not being applied at all.
    if (boosted <= plain) {
        helper.fail("never exceeded " + plain + " across " + FORTUNE_SAMPLES
                + " Fortune III rolls - Fortune is not applied");
        return;
    }
    helper.succeed();
}
```

**The vanilla control** is what separates "our table is broken" from "our test
tool is broken". Sample vanilla iron ore with the *same* tool and require the
same maximum:

```java
int vanillaMax = 0;
for (int i = 0; i < FORTUNE_SAMPLES; i++) {
    vanillaMax = Math.max(vanillaMax, totalCount(Block.getDrops(
            Blocks.IRON_ORE.defaultBlockState(), helper.getLevel(),
            helper.absolutePos(BlockPos.ZERO), null,
            helper.makeMockPlayer(GameType.SURVIVAL), fortune3)));
}
if (vanillaMax < 4) {
    helper.fail("vanilla iron_ore peaked at " + vanillaMax + " - the test tool is not honoured");
    return;
}
if (ourMax != vanillaMax) {
    helper.fail("our ore peaked at " + ourMax + " vs vanilla " + vanillaMax);
    return;
}
```

Always log the stacks on failure with a small `describe(List<ItemStack>)` helper
(`"[" + count + "x" + id + ", ...]"`); "dropped 1" is far less useful than
"dropped [1xcbc_rs:scheelite_ore]".

### 8.4 Checklist for a new recipe or loot table

1. Write the JSON under `src/main/resources/data/...` (or produce it from datagen
   - see the shadowing pitfall in section 4c).
2. Add the exact id to the `allRecipesLoadWithoutErrors` id list, and bump the
   expected count.
3. For a block drop, add or extend a loot test; if Fortune is involved, sample it
   and keep a vanilla control.
4. Extend `_tools/verify_scaffold.py` with a content assertion (numbers inside
   Create machine recipes cannot be checked from a game test).
5. Run `gradlew runData` if datagen owns any of the files, then
   `gradlew build runGameTestServer`.
6. Record the new totals in section 4c.
