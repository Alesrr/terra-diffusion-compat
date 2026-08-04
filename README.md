# Terrain Diffusion MC — Terralith compatibility fork

A fork of [xandergos/terrain-diffusion-mc](https://github.com/xandergos/terrain-diffusion-mc)
that makes [Terralith](https://modrinth.com/mod/terralith)'s biomes actually generate, adds
temperature-driven snow depth, carves rivers, and places water lakes.

Everything else — the diffusion pipeline, the models, the explorer UI, the build system — is
upstream's work, unchanged. If you want the base mod without any of this, use
[upstream](https://modrinth.com/mod/terrain-diffusion) instead.

<img width="1914" height="1029" alt="Screenshot 2026-07-31 120407" src="https://github.com/user-attachments/assets/1019878e-d109-4e94-ad13-d6daf8ba3eee" />
<img width="1914" height="1030" alt="Screenshot 2026-07-31 130154" src="https://github.com/user-attachments/assets/0e8e82ad-c2e5-4873-bf57-95ae1bd0d425" />

Targets **Minecraft 1.21.1**, Fabric and NeoForge.

> The research behind the base mod was accepted to SIGGRAPH 2026. That's xandergos's work,
> not this fork's.

## What this fork changes

**Terralith biomes generate.** Upstream replaces the biome source and ships its own noise
settings, so Terralith's two placement hooks are never consulted and its ~90 biomes register
but are unreachable. This fork maps the model's elevation and climate output onto 76 Terralith
surface biomes, and pulls Terralith's surface rules in at world load. Details in
[Terralith compatibility](#terralith-compatibility) below.

**Snow gets deeper as it gets colder.** Vanilla lays one snow layer everywhere it snows. Here
the depth comes from the terrain model's own temperature: 1–7 layers between −5 °C and
−7.5 °C, then a full snow block plus another 1–7 layers down to −10 °C. Snow on a tree canopy
also snows the ground underneath. If [Snow Real Magic](https://modrinth.com/mod/snow-real-magic)
is installed, columns where it is keeping a plant alive get a configurable lower ceiling.

**Rivers.** Channels are carved from the zero-crossings of two Perlin fields, up to 42 m deep,
faded out above 150 m elevation and on slopes over 0.20. River and frozen river biomes are
placed along them.

**Water lakes**, via a [Lithostitched](https://modrinth.com/mod/lithostitched) worldgen
modifier. See the note under [Requirements](#requirements) — without Lithostitched they
silently do not generate.

**Two terrain fixes.** Ocean depth now falls off hyperbolically to a 96-block floor instead of
following a square-root curve off the bottom of the world. The density function declares
`[-4096, 4096]` instead of `[-64, 1024]`, matching what tall worlds actually produce.

The NeoForge mod id is `terra_diffusion_compat`, so this installs alongside upstream's
NeoForge jar rather than colliding with it. On Fabric the id is still `terrain-diffusion-mc`
and the two jars **will** collide — pick one.

## Requirements

Same as upstream:

- Windows with a GPU, or Linux with an NVIDIA GPU. CPU inference works but is very slow.
- 1.5 GB VRAM, 2.5 GB RAM (you may need to raise Minecraft's memory allocation).
- Minecraft 1.21.1 with either [Fabric](https://fabricmc.net/) plus
  [Fabric API](https://modrinth.com/mod/fabric-api), or [NeoForge](https://neoforged.net/).

Optional, and only relevant to this fork:

- **[Terralith](https://modrinth.com/mod/terralith)** — without it the vanilla biome palette is
  used and nothing here misbehaves.
- **[Lithostitched](https://modrinth.com/mod/lithostitched)** — required for the water lakes.
  It is not declared as a dependency, so if it is missing the lake feature is skipped with no
  crash and no log line. Terralith already depends on it, so if you have Terralith you have this.
- **[Snow Real Magic](https://modrinth.com/mod/snow-real-magic)** — only affects the snow
  ceiling over vegetation.

## Installing

Check releases for the latest .jar file to copy in your /mods folder.

Once you have a jar:

1. Drop it in your `mods/` folder. The Minecraft version has to match.
2. Launch once while online so the models download (~2.5 GB).
3. Create a world, pick the **Terrain Diffusion** world type, and click **Customize** to set
   `World Scale` (see [Per-world settings](#per-world-settings)).
4. Spawn search finds land near the origin on its own. If (0, 0) is all ocean it takes a
   moment. Use `/td-explore` to scout further.

**Using the CUDA build?** Read [CUDA_INSTALL.md](CUDA_INSTALL.md) first.

## Exploring the world

Upstream's terrain explorer is unchanged. Run `/td-explore` in game and it prints a clickable
link (`http://localhost:19801` by default) that opens an interactive map. Click the map on the
left for a detailed view; click that for coordinates in the bottom left. You can filter by
climate.

## Configuration

`config/terrain-diffusion-mc.properties`, created on first launch:

```properties
# Terrain Diffusion MC configuration

# Inference device: "cpu", "gpu", or "auto" (try GPU first then fall back to CPU).
# "gpu" uses DirectML on the -windows build, or CUDA on the -cuda build.
# GPU builds default to "gpu" so startup fails loudly if no GPU is detected.
# CPU build defaults to "auto": uses CoreML on macOS, otherwise CPU.
inference.device=gpu

# Offload inactive models from VRAM between pipeline stages.
# Keeps peak VRAM to ~1.5-2 GB. Set to false if you have ~2.5+ GB free for slightly
# faster generation.
inference.offload_models=true

# Validate SHA-256 for pre-existing files in .minecraft/terrain-diffusion-models.
# Set to false if you want to provide custom models/config files without hash checks.
validate_model=true

# Port for the local terrain explorer web UI (/td-explore).
explorer.port=19801

# Spawn search: coarse-pixel region sizes for finding a land spawn near (0, 0).
# Starts at initial_size x initial_size and expands by 8 each step up to max_size x max_size.
# Each coarse pixel covers a large area (hundreds of blocks), so 16–128 is typically sufficient.
spawn_search.initial_size=16
spawn_search.max_size=128

# Snow lies deeper the colder the ground is, instead of a uniform single layer everywhere.
snow.depth_scaling=true

# Layer cap where snow is covering vegetation instead of bare ground. Keep this equal to
# Snow Real Magic's accumulation.maxLayers. Ignored when Snow Real Magic is not installed.
snow.max_layers_over_vegetation=6

# Place Terralith's biomes when Terralith is installed.
terralith.enabled=true

# Give Terralith's biomes their own surface blocks. See caveates in compatibility!
terralith.inject_surface_rules=true
```

The last four keys are specific to this fork.

### Terralith compatibility

Terralith places biomes by extending the vanilla multi-noise biome parameter list through
Lithostitched, and defines their surfaces by overriding
`minecraft:worldgen/noise_settings/overworld`. Terrain Diffusion replaces the biome source
outright and uses its own noise settings, so neither hook is ever read. That is why Terralith
and upstream coexist without errors and without any Terralith biome ever appearing.

Two pieces close the gap:

- `TerralithClassifier` maps the model's elevation and four climate variables onto 76 Terralith
  surface biomes. Climate picks a class of biome, then dedicated low-frequency noise picks the
  variant inside that class, which keeps showpiece biomes (lavender, sakura, moonlight,
  blooming) rare instead of carpeting every region whose climate happens to match.
- `TerralithSurfaceRules` reads the surface rule off the `minecraft:overworld` noise settings at
  world load and prepends it to this mod's, so calcite cliffs, tuff, basalt and painted
  terracotta generate. It reads whatever Terralith version you have installed rather than a
  pinned copy, so Terralith updates carry over.

Both are automatic. Terralith is detected at world load and the vanilla palette is kept when it
is absent, so one jar covers both cases.

Two caveats, which is why `terralith.inject_surface_rules` exists:

- Terralith's surface rules also cover vanilla biomes, so vanilla surfaces become Terralith's
  versions of them rather than this mod's.
- Those rules were written for a 384-block world. Terrain Diffusion worlds are much taller, so
  rules keyed to absolute Y can look wrong at extreme altitude.

Turning it off keeps this mod's surfaces everywhere, at the cost of Terralith biomes looking
generic.

Not placed: the 11 `cave/*` and 4 `skylands_*` biomes, which need terrain shapes this mod does
not generate. Also skipped are `warm_river`, `alpha_islands`, `alpha_islands_winter` and
`mirage_isles`, which depend on river and island detection the diffusion pipeline does not
expose.

### Per-world settings

For Terrain Diffusion worlds, click **Customize** during world creation and set:

- `World Scale` (integer `1..6`)

Saved with the world. It controls:

- how many real-world meters each block represents (`scale=1` → `30 m/block`, `scale=2` →
  `15 m/block`, and so on)
- max world height for newly created worlds (assumes the tallest point is 10000 m)

2 is a good balance. Use 1 for smaller, more compressed worlds. Lower values lean on the GPU
(the model runs more often); higher values lean on the CPU (taller worlds). Most modern GPUs
end up CPU-bottlenecked around scale 2 or 3.

## Common issues

**A dynamic link library (DLL) initialization routine failed**

Some older Java versions. Update to the latest Java 21 or higher; the
[latest Microsoft OpenJDK 21](https://learn.microsoft.com/en-us/java/openjdk/download) is known
to work.

**LoadLibrary failed with error 126** *(CUDA build only)*

Usually a bad CUDA or cuDNN install. See [CUDA_INSTALL.md](CUDA_INSTALL.md).

**java.lang.IllegalStateException: Failed to load terrain-diffusion models**

Almost always out of memory — the logs should confirm it. The models need about 2.5 GB of RAM,
so allocate enough.

**Terralith is installed but I see no Terralith biomes**

Check the log at world load. The biome source logs how many Terralith biomes it resolved, and
it deliberately falls back to the vanilla palette if any expected biome is missing rather than
generating a half-broken world. A version mismatch between Terralith and this fork's biome
table is the usual cause.

For anything in the base mod, use
[upstream's issue tracker](https://github.com/xandergos/terrain-diffusion-mc/issues). For the
Terralith, snow, river or lake behaviour, open an issue here.

## Building from source

Needs **JDK 21**. Newer JDKs fail in `buildSrc` with
`Unsupported class file major version` because of the Gradle version in use.

An internet connection is required during the build to fetch the pinned model manifest metadata
from Hugging Face.

The `-windows` build needs `libs/onnxruntime-dml.jar`, which ships in the repo. See
[Building onnxruntime with DirectML](#building-onnxruntime-with-directml) to build it yourself.

### Build tasks

Use `Windows` when you want the DirectML build. The old `Dml` names still work as aliases.

| What you want | Command |
|---------------|---------|
| Fabric + NeoForge, Windows/DirectML | `./gradlew buildWindows` |
| Fabric + NeoForge, CUDA | `./gradlew buildCuda` |
| Fabric + NeoForge, CPU/CoreML | `./gradlew buildCpu` |
| Every loader and every variant | `./gradlew buildRelease` |
| Every loader/variant, copied into `build/release/` | `./gradlew collectReleaseJars` |
| Fabric only, Windows/DirectML | `./gradlew buildFabricWindows` |
| Fabric only, CUDA | `./gradlew buildFabricCuda` |
| Fabric only, CPU/CoreML | `./gradlew buildFabricCpu` |
| Every Fabric variant | `./gradlew buildFabricAll` |
| NeoForge only, Windows/DirectML | `./gradlew buildNeoForgeWindows` |
| NeoForge only, CUDA | `./gradlew buildNeoForgeCuda` |
| NeoForge only, CPU/CoreML | `./gradlew buildNeoForgeCpu` |
| Every NeoForge variant | `./gradlew buildNeoForgeAll` |

The direct property form still works:

```
./gradlew build -PuseDml=true
./gradlew build -PuseCuda=true
./gradlew build -PuseCpu=true
./gradlew :fabric:build -PuseDml=true
./gradlew :neoforge:build -PuseDml=true
```

Aliases kept for existing scripts:

```
./gradlew buildDml
./gradlew buildFabricDml
./gradlew buildNeoForgeDml
./gradlew buildAll
```

Jars land under each loader module:

```
fabric/build/libs/
neoforge/build/libs/
```

`./gradlew collectReleaseJars` copies the distributable ones into:

```
build/release/fabric/
build/release/neoforge/
```

### Building onnxruntime with DirectML

**Requirements**

- [Windows 10 SDK (10.0.17134.0)](https://developer.microsoft.com/en-us/windows/downloads/sdk-archive/index-legacy) — Windows 10 1803 or newer
- Visual Studio 2017 toolchain — *Desktop development with C++* from the VS Installer
- Visual Studio 2022 toolchain — same
- Python 3.10+
- CMake 3.28 or higher

Keep both VS toolchains current. Full details in the
[ONNX Runtime build docs](https://onnxruntime.ai/docs/build/inferencing.html) and the
[DirectML EP requirements](https://onnxruntime.ai/docs/execution-providers/DirectML-ExecutionProvider.html#build).

**Steps**

Run everything from the **Developer Command Prompt for VS 2022**.

```
git clone --recursive https://github.com/Microsoft/onnxruntime.git
cd onnxruntime
.\build.bat --config RelWithDebInfo --build_shared_lib --parallel --compile_no_warning_as_error --skip_submodule_sync --use_dml --build_java --build
```

The jar appears in `java/build/`. Rename it to `onnxruntime-dml.jar` and put it in `libs/`.

## For mod developers

Upstream's point stands and is worth repeating: modifying the AI terrain is hard, but the biome
integration is not. The model outputs elevation plus four climate variables, and hand-written
rules turn that into Minecraft biomes. It is the most direct way to improve how the terrain
reads, and it mostly takes patience rather than cleverness.

The two classifiers in this fork are
[`BiomeClassifier`](common/src/main/java/com/github/xandergos/terraindiffusionmc/pipeline/BiomeClassifier.java)
(vanilla palette) and
[`TerralithClassifier`](common/src/main/java/com/github/xandergos/terraindiffusionmc/pipeline/TerralithClassifier.java)
(Terralith palette). Both read the same
[`TerrainSample`](common/src/main/java/com/github/xandergos/terraindiffusionmc/pipeline/TerrainSample.java)
struct, so adding a third palette means writing one more class and a biome id table.

Terrain diversity still outpaces biome diversity by a wide margin. There is plenty left to do.

## Credits and licence

Terrain Diffusion and this mod's entire pipeline are by
[xandergos](https://github.com/xandergos). Original repositories:
[terrain-diffusion](https://github.com/xandergos/terrain-diffusion) and
[terrain-diffusion-mc](https://github.com/xandergos/terrain-diffusion-mc).

Terralith is by [Stardust Labs](https://modrinth.com/mod/terralith).

MIT, same as upstream. Copyright (c) 2025 Alexander Goslin — see [LICENSE.txt](LICENSE.txt).
Fork changes are offered under the same licence.
