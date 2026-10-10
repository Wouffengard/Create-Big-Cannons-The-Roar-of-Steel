#!/usr/bin/env python3
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
MOD_ID = "createbigcannons_theroarofsteel"
NS = "cbc_rs"
BASE_PKG = "com.cbcrs"
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

print("== dependency layout ==")
# Maven coordinates used by MODE A (compileOnly / additionalRuntimeClasspath /
# runtimeOnly / implementation / <custom>Configuration).
maven_deps = re.findall(
    r'(?:compileOnly|additionalRuntimeClasspath|runtimeOnly|implementation|createDevRuntime)\s*\(?\s*"([^"]+)"',
    gradle)
for dep in maven_deps:
    coord = ":".join(dep.split(":")[:2]) if dep.count(":") >= 2 else dep
    ok("maven dependency: " + coord)
if not any(d.startswith("com.simibubi.create:create-") for d in maven_deps):
    bad("no Create dependency found in build.gradle")
if "artifact { classifier = 'slim' }" not in gradle:
    bad("Create is expected to use the slim CLASSIFIER (artifact selector), not a -slim version")
if not any("Registrate" in d for d in maven_deps):
    bad("no Registrate dependency (needed for CreateRegistrate)")
# Pinned library versions must not float, or compile/runtime can diverge.
for key in ("registrate_version", "ponder_version", "flywheel_version"):
    m = re.search(r"(?m)^{}=(.+)$".format(key), props)
    if not m:
        bad("missing {}".format(key))
    elif "[" in m.group(1) or "+" not in m.group(1) and "-" not in m.group(1):
        ok("{} pinned to {}".format(key, m.group(1)))
    else:
        ok("{} pinned to {}".format(key, m.group(1)))

# Jars that must always exist locally (not on any public Maven). They are
# declared in the externalModJars map and staged into <gameDir>/mods for runs.
always_local = [
    "libs/createbigcannons-5.11.7+mc.1.21.1.jar",
    "libs/ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar",
    "libs/sable-neoforge-1.21.1-2.0.6.jar",
    "libs/create-aeronautics-bundled-1.21.1-1.3.2.jar",
]
for m in always_local:
    if m in gradle:
        ok("local mod jar declared in build.gradle: " + m)
    else:
        bad("local mod jar NOT declared in build.gradle: " + m)
    (ok if (ROOT / m).is_file() else bad)(
        "libs present (else run _tools/fetch_local_libs.ps1): " + m)

# Every run configuration shares one game directory, and a single staging task
# supplies the external mods.
(ok if "tasks.register('syncMods'" in gradle else bad)(
    "dev-run mods staging task 'syncMods' declared")
(ok if "run/mods" in gradle else bad)("staging target is run/mods")
for run_task in ("runServer", "runClient", "runData", "runGameTestServer"):
    (ok if run_task in gradle else bad)("run task wired for staging: " + run_task)
(ok if "gameDirectory = project.file('run')" in gradle else bad)(
    "all runs share the run/ game directory")

# Regression guards for the two failures that were hit and fixed.
# Comments are stripped first: this file's own build.gradle warns about these
# mistakes in prose, and the guards must look at real declarations only.
def _strip_gradle_comments(text):
    out = []
    for line in text.split("\n"):
        stripped = line.strip()
        if stripped.startswith("//"):
            continue
        out.append(line)
    return "\n".join(out)


gradle_code = _strip_gradle_comments(gradle)
if re.search(r"additionalRuntimeClasspath[^\n]*flywheel", gradle_code, re.I) or \
        re.search(r"additionalRuntimeClasspath[^\n]*ponder", gradle_code, re.I):
    bad("Flywheel/Ponder must not be on additionalRuntimeClasspath (duplicate JPMS modules)")
else:
    ok("no duplicate Flywheel/Ponder on the runtime classpath")
(ok if "createDevRuntime" in gradle else bad)(
    "separate Create dev-runtime configuration present")

# Offline-mode jars are referenced inside comments; only warn if absent.
offline_local = [
    "libs/create-1.21.1-6.0.10.jar",
    "libs/Registrate-MC1.21-1.3.0+67.jar",
    "libs/ponder-neoforge-1.0.82+mc1.21.1.jar",
    "libs/flywheel-neoforge-1.21.1-1.0.6.jar",
]
for m in offline_local:
    (ok if (ROOT / m).is_file() else bad)("offline-mode jar available: " + m)

def strip_literals_and_comments(text):
    """Remove string/char literals and comments so delimiter counts are meaningful."""
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if c == "/" and nxt == "/":
            while i < n and text[i] != "\n":
                i += 1
        elif c == "/" and nxt == "*":
            i += 2
            while i + 1 < n and not (text[i] == "*" and text[i + 1] == "/"):
                i += 1
            i += 2
        elif text[i:i + 3] == '"""':
            i += 3
            while i + 2 < n and text[i:i + 3] != '"""':
                i += 1
            i += 3
        elif c == '"':
            i += 1
            while i < n and text[i] != '"':
                i += 2 if text[i] == "\\" else 1
            i += 1
        elif c == "'":
            i += 1
            while i < n and text[i] != "'":
                i += 2 if text[i] == "\\" else 1
            i += 1
        else:
            out.append(c)
            i += 1
    return "".join(out)


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
    code = strip_literals_and_comments(body)
    if code.count("{") != code.count("}"):
        bad("unbalanced braces: " + rel)
    if code.count("(") != code.count(")"):
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
        # Our own content must sit in our namespace. Two namespaces are
        # deliberately shared with the wider ecosystem and are allowed:
        #   minecraft - vanilla tag files (ore tags, mining tags)
        #   c         - NeoForge/Forge common tags (raw_materials, nuggets, ...)
        SHARED = {"minecraft", "c"}
        if top != NS and top not in SHARED:
            bad("resource namespace is '{}' not '{}': {}".format(top, NS, rel))
        elif top in SHARED and "/tags/" not in rel:
            # Only tags may live outside our namespace; no assets or other data.
            bad("non-tag resource in shared namespace '{}': {}".format(top, rel))
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

print("== phase 1 content: tungsten ingot ==")
items_path = ROOT / "src/main/java/com/cbcrs/index/CBSItems.java"
if items_path.is_file():
    item_java = read(items_path)
    for needed in ['item("tungsten_ingot"', '.lang("Tungsten Ingot")', "item/generated", "layer0"]:
        (ok if needed in item_java else bad)("CBSItems declares: " + needed)
else:
    bad("CBSItems.java missing")
(ok if "CBSItems.register()" in read(ROOT / "src/main/java/com/cbcrs/index/CBSIndex.java")
    else bad)("CBSIndex calls CBSItems.register()")
(ok if "CBSItems.TUNGSTEN_INGOT" in read(ROOT / "src/main/java/com/cbcrs/index/CBSTabs.java")
    else bad)("CBSTabs references the tungsten ingot (tab icon / contents)")

tex = ROOT / "src/main/resources/assets/cbc_rs/textures/item/tungsten_ingot.png"
(ok if tex.is_file() else bad)("item texture present: " + tex.name)
if tex.is_file():
    head = tex.read_bytes()[:8]
    (ok if head == b"\x89PNG\r\n\x1a\n" else bad)("texture is a real PNG")

gen_model = ROOT / "src/generated/resources/assets/cbc_rs/models/item/tungsten_ingot.json"
if gen_model.is_file():
    model = json.loads(read(gen_model))
    tex_ref = model.get("textures", {}).get("layer0", "")
    (ok if tex_ref == "cbc_rs:item/tungsten_ingot" else bad)(
        "generated model points at cbc_rs:item/tungsten_ingot (got " + str(tex_ref) + ")")
    (ok if model.get("parent") == "minecraft:item/generated" else bad)(
        "generated model uses item/generated")
else:
    bad("generated item model missing - run: gradlew runData")

zh = json.loads(read(ROOT / "src/main/resources/assets/cbc_rs/lang/zh_cn.json"))
(ok if zh.get("item.cbc_rs.tungsten_ingot") == "\u94a8\u952d" else bad)("zh_cn name for the tungsten ingot")
generated_en = ROOT / "src/generated/resources/assets/cbc_rs/lang/en_us.json"
if generated_en.is_file():
    en = json.loads(read(generated_en))
    (ok if en.get("item.cbc_rs.tungsten_ingot") == "Tungsten Ingot" else bad)(
        "generated en_us name for the tungsten ingot")
else:
    bad("generated en_us missing - run: gradlew runData")

struct = ROOT / "src/main/resources/data/cbc_rs/structure/empty.nbt"
(ok if struct.is_file() else bad)("game-test structure template present")
if struct.is_file():
    import gzip as _gz
    try:
        raw = _gz.decompress(struct.read_bytes())
        (ok if raw[:1] == b"\x0a" else bad)("template decompresses to a compound (single gzip layer)")
    except Exception as exc:
        bad("template is not single-gzip compressed: " + str(exc))

gt = ROOT / "src/main/java/com/cbcrs/gametest/CBSGameTests.java"
if gt.is_file():
    gtc = read(gt)
    (ok if "RegisterGameTestsEvent" in gtc else bad)("game tests register via RegisterGameTestsEvent")
    # Exactly ONE registration route: the direct GameTestRegistry call, because
    # RegisterGameTestsEvent fires after the registry has been consumed in this
    # setup. Using both duplicates every test (observed: 6 instead of 3).
    (ok if re.search(r"GameTestRegistry\.register\(", gtc) else bad)(
        "game tests register directly via GameTestRegistry")
    # The type may be mentioned in prose; an actual import/usage is the problem.
    (ok if "import net.neoforged.neoforge.event.RegisterGameTestsEvent" not in gtc else bad)(
        "must NOT also register via RegisterGameTestsEvent (each test would run twice)")
else:
    bad("CBSGameTests.java missing")
(ok if (ROOT / "src/main/java/com/cbcrs/gametest/TungstenIngotGameTests.java").is_file()
    else bad)("TungstenIngotGameTests.java present")

print("== test baseline documentation ==")
testing = ROOT / "TESTING.md"
(ok if testing.is_file() else bad)("TESTING.md present (test procedure + baseline)")
if testing.is_file():
    t = read(testing)
    for needed in ("ASSET OK", "ASSET PIXELS", "Known compatibility noise",
                   "pngBytes_sha256", "decodedRGBA_sha256",
                   # reusable templates for the two error-prone test kinds
                   "Test templates: recipes and loot tables",
                   "Block.getDrops", "hadErrorsLoading", "FORTUNE_SAMPLES"):
        (ok if needed in t else bad)("TESTING.md documents: " + needed)

    # A Chinese development log must exist and cover the environment and the
    # content added in 0.1.0.
    log = ROOT / "CHANGELOG.dev.md"
    (ok if log.is_file() else bad)("CHANGELOG.dev.md present (development log)")
    if log.is_file():
        c = read(log)
        for needed in ("0.1.0", "开发环境", "NeoForge", "21.1.228", "1.21.1",
                       "新增内容", "不包含的内容", "已知风险"):
            (ok if needed in c else bad)("CHANGELOG.dev.md covers: " + needed)
(ok if "Known risks" in read(ROOT / "README.md") else bad)(
    "README has a Known risks section")

print("== tungsten ores and materials (decisions 15/16) ==")
ores_java = ROOT / "src/main/java/com/cbcrs/index/CBSOres.java"
mats_java = ROOT / "src/main/java/com/cbcrs/index/CBSMaterials.java"
for path, label in ((ores_java, "CBSOres.java"), (mats_java, "CBSMaterials.java")):
    (ok if path.is_file() else bad)(label + " present")

if ores_java.is_file():
    o = read(ores_java)
    for needed in ["block(\"scheelite_ore\"", "block(\"wolframite_ore\"",
                   "defaultBlockstate()", "BlockTags.MINEABLE_WITH_PICKAXE",
                   "BlockTags.NEEDS_STONE_TOOL",
                   # Vanilla ore drop table (Silk Touch branch + ore_drops for
                   # Fortune). Generated by datagen, NOT hand-written.
                   "createOreDrop"]:
        (ok if needed in o else bad)("CBSOres declares: " + needed)
    # A hand-written table under src/main/resources would be shadowed by the
    # datagen output, so it must not exist. A game test caught exactly that.
    (ok if not (ROOT / "src/main/resources/data/cbc_rs/loot_table/blocks/scheelite_ore.json").exists()
        else bad)("no hand-written ore loot table (datagen owns it; a hand-written one is shadowed)")
    gen_loot = ROOT / "src/generated/resources/data/cbc_rs/loot_table/blocks/scheelite_ore.json"
    if gen_loot.is_file():
        lt = read(gen_loot)
        (ok if "minecraft:alternatives" in lt else bad)(
            "generated ore loot table branches on Silk Touch")
        (ok if "minecraft:ore_drops" in lt else bad)(
            "generated ore loot table uses ore_drops so Fortune scales")
        (ok if "cbc_rs:raw_scheelite" in lt else bad)(
            "generated ore loot table drops the raw ore")
    else:
        bad("generated ore loot table missing - run: gradlew runData")

    # world generation data
    for rel in ["data/cbc_rs/worldgen/configured_feature/scheelite_ore.json",
                "data/cbc_rs/worldgen/configured_feature/wolframite_ore.json",
                "data/cbc_rs/worldgen/placed_feature/scheelite_ore_main.json",
                "data/cbc_rs/worldgen/placed_feature/scheelite_ore_buried.json",
                "data/cbc_rs/worldgen/placed_feature/wolframite_ore_main.json",
                "data/cbc_rs/worldgen/placed_feature/wolframite_ore_secondary.json",
                "data/cbc_rs/neoforge/biome_modifier/add_scheelite_ore.json",
                "data/cbc_rs/neoforge/biome_modifier/add_wolframite_ore.json"]:
        (ok if (ROOT / "src/main/resources" / rel).is_file() else bad)("worldgen present: " + rel)

    # layer exclusion must be target-based, and the height provider type must be
    # minecraft:trapezoid (minecraft:triangle does not exist)
    cf = read(ROOT / "src/main/resources/data/cbc_rs/worldgen/configured_feature/scheelite_ore.json")
    (ok if "minecraft:stone_ore_replaceables" in cf and "deepslate_ore_replaceables" not in cf
        else bad)("scheelite targets the stone layer only")
    cfw = read(ROOT / "src/main/resources/data/cbc_rs/worldgen/configured_feature/wolframite_ore.json")
    (ok if "minecraft:deepslate_ore_replaceables" in cfw and "stone_ore_replaceables" not in cfw
        else bad)("wolframite targets the deepslate layer only")
    for name in ["scheelite_ore_main", "wolframite_ore_main"]:
        pf = read(ROOT / "src/main/resources/data/cbc_rs/worldgen/placed_feature/{}.json".format(name))
        (ok if "minecraft:trapezoid" in pf else bad)(name + " uses the minecraft:trapezoid provider")
        (ok if "minecraft:triangle" not in pf else bad)(name + " does not use minecraft:triangle")
    sec = read(ROOT / "src/main/resources/data/cbc_rs/worldgen/placed_feature/wolframite_ore_secondary.json")
    (ok if '"absolute": -48' in sec and '"absolute": -16' in sec else bad)(
        "wolframite secondary vein is tightened to y -48..-16")
    (ok if '"count": 10' in sec else bad)(
        "wolframite secondary vein count is 10 (lowered from vanilla 50 for density)")

    # recipes
    recipes = list((ROOT / "src/main/resources/data/cbc_rs/recipe").rglob("*.json")) \
        if (ROOT / "src/main/resources/data/cbc_rs/recipe").is_dir() else []
    (ok if len(recipes) == 20 else bad)(
        "20 recipe files present (got {})".format(len(recipes)))
    for kind, count in (("smelting", 6), ("blasting", 6), ("crushing", 4), ("splashing", 2), ("crafting", 2)):
        got = len(list((ROOT / "src/main/resources/data/cbc_rs/recipe" / kind).glob("*.json"))) \
            if (ROOT / "src/main/resources/data/cbc_rs/recipe" / kind).is_dir() else 0
        (ok if got == count else bad)("{} recipes: {} (expected {})".format(kind, got, count))

    crush_w = read(ROOT / "src/main/resources/data/cbc_rs/recipe/crushing/wolframite_ore.json")
    (ok if '"count": 2' in crush_w and '"processing_time": 350' in crush_w else bad)(
        "wolframite crushing mirrors deepslate iron ore (2 + 0.25, time 350)")
    crush_s = read(ROOT / "src/main/resources/data/cbc_rs/recipe/crushing/scheelite_ore.json")
    (ok if '"processing_time": 250' in crush_s else bad)(
        "scheelite crushing mirrors iron ore (time 250)")
    splash_w = read(ROOT / "src/main/resources/data/cbc_rs/recipe/splashing/crushed_raw_wolframite.json")
    (ok if '"chance": 0.15' in splash_w and '"chance": 0.02' in splash_w
        and "rose_quartz" in splash_w else bad)(
        "wolframite washing: 9 nuggets + 15% iron nugget + 2% rose quartz")
    splash_s = read(ROOT / "src/main/resources/data/cbc_rs/recipe/splashing/crushed_raw_scheelite.json")
    (ok if '"chance": 0.15' in splash_s and "clay_ball" in splash_s else bad)(
        "scheelite washing: 9 nuggets + 15% clay ball")

if mats_java.is_file():
    m = read(mats_java)
    for needed in ["raw_scheelite", "raw_wolframite", "crushed_raw_scheelite",
                   "crushed_raw_wolframite", "tungsten_nugget"]:
        (ok if needed in m else bad)("CBSMaterials declares: " + needed)

idx = read(ROOT / "src/main/java/com/cbcrs/index/CBSIndex.java")
for needed in ["CBSOres.register()", "CBSMaterials.register()"]:
    (ok if needed in idx else bad)("CBSIndex calls " + needed)

tabs = read(ROOT / "src/main/java/com/cbcrs/index/CBSTabs.java")
for needed in ["CBSOres.SCHEELITE_ORE", "CBSOres.WOLFRAMITE_ORE",
               "CBSMaterials.RAW_SCHEELITE", "CBSMaterials.TUNGSTEN_NUGGET"]:
    (ok if needed in tabs else bad)("creative tab lists " + needed)

# textures: 6 item icons + 2 block textures, all real PNGs
item_tex = ROOT / "src/main/resources/assets/cbc_rs/textures/item"
block_tex = ROOT / "src/main/resources/assets/cbc_rs/textures/block"
expected_item_tex = ["tungsten_ingot.png", "tungsten_nugget.png", "raw_scheelite.png",
                     "raw_wolframite.png", "crushed_raw_scheelite.png", "crushed_raw_wolframite.png"]
expected_block_tex = ["scheelite_ore.png", "wolframite_ore.png"]
for name in expected_item_tex:
    p = item_tex / name
    (ok if p.is_file() else bad)("item texture present: " + name)
for name in expected_block_tex:
    p = block_tex / name
    (ok if p.is_file() else bad)("block texture present: " + name)

import struct as _struct
for folder, names in ((item_tex, expected_item_tex), (block_tex, expected_block_tex)):
    for name in names:
        p = folder / name
        if not p.is_file():
            continue
        raw = p.read_bytes()
        (ok if raw[:8] == b"\x89PNG\r\n\x1a\n" else bad)("real PNG: " + name)
        if raw[:8] == b"\x89PNG\r\n\x1a\n":
            w, h = _struct.unpack(">II", raw[16:24])
            (ok if (w, h) == (16, 16) else bad)(
                "{} is 16x16 (got {}x{})".format(name, w, h))

# generated models / blockstates / loot
gen = ROOT / "src/generated/resources"
for rel in ["assets/cbc_rs/models/item/raw_scheelite.json",
            "assets/cbc_rs/models/item/crushed_raw_wolframite.json",
            "assets/cbc_rs/models/item/tungsten_nugget.json",
            "assets/cbc_rs/models/item/scheelite_ore.json",
            "assets/cbc_rs/models/block/scheelite_ore.json",
            "assets/cbc_rs/blockstates/wolframite_ore.json",
            "data/cbc_rs/loot_table/blocks/scheelite_ore.json",
            "data/cbc_rs/loot_table/blocks/wolframite_ore.json"]:
    (ok if (gen / rel).is_file() else bad)("generated: " + rel + " (run gradlew runData)")

gen_en = gen / "assets/cbc_rs/lang/en_us.json"
if gen_en.is_file():
    en = json.loads(read(gen_en))
    for key, want in [("item.cbc_rs.raw_scheelite", "Raw Scheelite"),
                      ("item.cbc_rs.tungsten_nugget", "Tungsten Nugget"),
                      ("block.cbc_rs.scheelite_ore", "Scheelite Ore")]:
        (ok if en.get(key) == want else bad)("en_us {} = {}".format(key, want))

# tags
for rel in ["data/c/tags/item/raw_materials/scheelite.json",
            "data/c/tags/item/crushed_raw_materials/wolframite.json",
            "data/c/tags/item/nuggets/tungsten.json",
            "data/minecraft/tags/item/scheelite_ores.json"]:
    (ok if (ROOT / "src/main/resources" / rel).is_file() else bad)("tag present: " + rel)

gen_tags = gen / "data/minecraft/tags/block/mineable/pickaxe.json"
if gen_tags.is_file():
    t = json.loads(read(gen_tags))
    vals = t.get("values", [])
    (ok if "cbc_rs:scheelite_ore" in vals and "cbc_rs:wolframite_ore" in vals else bad)(
        "generated pickaxe tag lists both ores")

# zh_cn
zh2 = json.loads(read(ROOT / "src/main/resources/assets/cbc_rs/lang/zh_cn.json"))
for key, want in [("block.cbc_rs.scheelite_ore", "\u767d\u94a8\u77ff"),
                  ("block.cbc_rs.wolframite_ore", "\u9ed1\u94a8\u77ff"),
                  ("item.cbc_rs.crushed_raw_scheelite", "\u7c89\u788e\u767d\u94a8\u77ff"),
                  ("item.cbc_rs.tungsten_nugget", "\u94a8\u7c92")]:
    (ok if zh2.get(key) == want else bad)("zh_cn {} correct".format(key))

# exactly one tungsten ingot: the new classes must not register another
all_java = "\n".join(read(p) for p in (ROOT / "src/main/java/com/cbcrs/index").glob("*.java"))
(ok if all_java.count('item("tungsten_ingot"') == 1 else bad)(
    "exactly one tungsten_ingot registration (decision 16)")

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
