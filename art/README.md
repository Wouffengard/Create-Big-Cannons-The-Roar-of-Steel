# Source art

Author-supplied source textures for this mod, kept so the artwork is versioned
alongside the code. These are **source copies**; the game loads the copies under
`src/main/resources/assets/cbc_rs/textures/`.

## Layout

```
art/textures/
├─ item/     item icons          -> src/main/resources/assets/cbc_rs/textures/item/
└─ block/    block textures      -> src/main/resources/assets/cbc_rs/textures/block/
```

## Naming

Item source files already carry their registry id, so they install unchanged.
The two block textures were authored under shorter names and are **renamed on
install**:

| Source file | Installed as | Registry id |
| --- | --- | --- |
| `block/scheelite.png` | `block/scheelite_ore.png` | `cbc_rs:scheelite_ore` |
| `block/wolframite.png` | `block/wolframite_ore.png` | `cbc_rs:wolframite_ore` |

## Conventions

Both are enforced by the client-side asset assertion (see `TESTING.md` section 3)
and by the offline verifier:

| Kind | Folder | Alpha |
| --- | --- | --- |
| Item icon | `item/` | must have transparent pixels - a cut-out sprite drawn on `item/generated` |
| Block texture | `block/` | fully opaque, used by a `cube_all` model |

All textures are 16x16 RGBA.

## Installing an updated texture

1. Replace the file here (and the workspace copy, if you keep one).
2. Copy it to the matching path under `src/main/resources/assets/cbc_rs/textures/`.
3. `gradlew build`, then `gradlew runClient` and confirm the log shows
   `ASSET SUMMARY textures: 8 present, 0 missing` and `models: 8 ok, 0 failed`.

The item icons install with a straight copy; only the two block textures need the
rename above.
