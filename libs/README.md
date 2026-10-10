# libs/ - local dependency jars

## What is here and why

Create Big Cannons, Ritchie's Projectile Library, Sable and Aeronautics are
**not published to any public Maven repository** (verified against Maven Central
and modmaven), so they are always resolved from this folder.

`create-1.21.1-6.0.10.jar` and the three library jars are only needed in
**MODE B** of `build.gradle` (fully offline). In the default MODE A they are
resolved from Maven, but keeping the copies here costs nothing and lets you
switch to offline mode instantly.

| File | Role | Needed in |
| --- | --- | --- |
| `createbigcannons-5.11.7+mc.1.21.1.jar` | Create Big Cannons 5.11.7 | always |
| `ritchiesprojectilelib-2.1.2+mc.1.21.1-neoforge.jar` | Ritchie's Projectile Library 2.1.2 | always |
| `sable-neoforge-1.21.1-2.0.6.jar` | Sable 2.0.6 (optional dependency) | always |
| `create-aeronautics-bundled-1.21.1-1.3.2.jar` | Create Aeronautics (bundled: aeronautics + simulated + offroad) | always |
| `create-1.21.1-6.0.10.jar` | Create 6.0.10 | MODE B only |
| `Registrate-MC1.21-1.3.0+67.jar` | Registrate | MODE B only |
| `ponder-neoforge-1.0.82+mc1.21.1.jar` | Ponder | MODE B only |
| `flywheel-neoforge-1.21.1-1.0.6.jar` | Flywheel | MODE B only |

The last three are the exact libraries Create 6.0.10-281 bundles in its
`META-INF/jarjar/` folder, extracted verbatim so the compile-time and
runtime versions match Create's own.

`imguimc` is intentionally **not** listed: it is not required by any of the six
mods in this workspace and is not a runtime requirement for this addon (see the
feasibility report, chapter 3).

Populate this folder with:

```powershell
powershell -ExecutionPolicy Bypass -File ./_tools/fetch_local_libs.ps1
```

The script copies the four mod jars from the workspace and extracts the three
bundled library jars out of `create-1.21.1-6.0.10.jar`.
