# BarelyStorage

A server room for your items, for Minecraft on Forge and NeoForge (26.1.2, 26.2, 26.3). Server Racks take Forge Energy and hold drive cartridges and CPU or RAM blades. Bus cables link them to the Storage, Crafting, Pattern and Queue Terminals, the Interface, Import and Export Nodes, Storage Buses and Assemblers. Crafting runs from patterns and is limited by the threads and RAM your racks offer.

Docs: https://itmecasey.com/wiki/barelystorage/. Listing text: [`curseforge/DESCRIPTION.md`](curseforge/DESCRIPTION.md).

## What it adds

| Block or item | What it does |
|---|---|
| Server Rack | Takes Forge Energy, holds up to 8 drive cartridges and 4 blades, pays an upkeep and goes offline without power |
| Drive cartridges | Iron, Steel, Gold, Obsidian and Quantum; they keep their items inside the item |
| Rack Bus Cable | Straight tube that joins racks, terminals, nodes and assemblers |
| Storage Terminal | Scrolling, searchable list with sort, view and theme buttons; click takes one item, shift-click a stack; items you can craft but have none of show a C |
| Crafting Terminal | 3x3 grid fed by the network and then your inventory; missing ingredients turn red |
| Pattern Terminal | Encodes crafting and processing patterns, with a Tags toggle |
| Crafting Queue Terminal | Shows running jobs and halts, and cancels them |
| Assembler | Crafts from crafting patterns, takes up to four upgrade cards |
| Interface, Import and Export Nodes, Storage Bus | Feed machines, pull items in, push items out, map a chest or tank into the network; each has a filter and upgrade screen |
| Upgrade cards | Speed, Capacity, Efficiency (takes 10 percent off every rack's upkeep per card, up to 50 percent) and Filter |
| CPU and RAM blades | Basic Core, Multi-Threaded RAM Blade and Quantum Core set threads, RAM and chain depth; a job halts with Thread Exhaustion or Out of Memory until you add hardware |

Scripting: with ModJS installed, server scripts get a `BarelyStorage` global; see [docs/scripting.md](docs/scripting.md).

Optional integrations: JEI, REI and BEI plus buttons and search sync, a BEI storage source, Show Me What This Is and Jade (NeoForge) info lines. See the wiki's Compatibility page for what was checked where.

## Build

One module per loader and Minecraft version (`neoforge-26.2`, `forge-26.2`, and so on), sharing `common/` (Minecraft-free logic and unit tests) and the master code in `version-26.2`. Never edit the copies in `version-26.1.2` and `version-26.3`; run `python scripts/sync_versions.py` after changing `version-26.2`.

```
cd neoforge-26.2
./gradlew build                    # jar in build/libs
./gradlew test                     # unit tests
python scripts/server_check.py neoforge-26.2   # headless self test (needs -PdevTools, the script adds it)
```

`python scripts/combine_jars.py out.jar a.jar b.jar` joins the Forge and NeoForge jars of one Minecraft version into the release jar (CI does this). Development tools (self test, screenshot tour) build only with `-PdevTools`.

Optional dependencies (SMWTI, JEI, REI, BEI) come from `curse.maven`: through our private mirror when `IMC_MAVEN_USER` and `IMC_MAVEN_PASSWORD` are set, from cursemaven otherwise. The BEI API jars are in each module's `libs/`.

## Version

`YYYY.M.n`, the same in every module's `gradle.properties`. Releases are tags `v<version>`.

License: MIT. The GUI fonts Silkscreen and Liberation Sans are under the SIL Open Font License (texts in `licenses/` and next to the font files).
