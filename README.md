# BarelyStorage

A server room for your items, for Minecraft on Forge and NeoForge (26.1.2, 26.2, 26.3). Server Racks take Forge Energy and hold drive cartridges and CPU or RAM blades. Bus cables link them to the Storage, Crafting, Pattern and Queue Terminals, the Interface, Import and Export Nodes, Storage Buses and Assemblers. Crafting runs from patterns and is limited by the threads and RAM your racks offer.

Docs: https://itmecasey.com/wiki/barelystorage/ (once published). Listing text: [`curseforge/DESCRIPTION.md`](curseforge/DESCRIPTION.md).

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

License: MIT.
