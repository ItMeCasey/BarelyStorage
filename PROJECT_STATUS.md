# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-05
Summary: Server-room storage network (racks, drives, bus cables, nodes, terminals, patterns, CPU/RAM-limited crafting) on Forge and NeoForge 26.1.2, 26.2 and 26.3; logic, headless self test and unit tests done on all six layers, client frames, art pass, compat, CI and docs still to do.
Spend: none

## Foundation
- [x] Layout: shared common/ plus version-<mc>/ and six loader layers (w:3)
- [x] Gradle builds compile on Forge and NeoForge for 26.1.2, 26.2 and 26.3 (w:4)
- [x] 30 unit tests for storage, priority routing, graph, planner, scheduler, persistence and power (w:4)
- [x] Headless server self test, 22 checks, SELFTEST DONE failures=0 on all six layers (w:4)
- [ ] Combined jar per Minecraft version with a dev-tools-out-of-release check (w:3)
- [~] GitHub repository (public repo exists, first commit pushed; later commits local until the PM pushes) (w:1)

## Core
- [x] Server Rack with FE port, drive bays, blade slots, upkeep and offline state (w:5)
- [x] Storage cartridges in five tiers keeping contents in the item (w:4)
- [x] Rack Bus Cable linking racks, nodes, terminals and assemblers (w:3)
- [x] Storage Bus mapping outside inventories with High/Normal/Low priority (w:4)
- [~] Storage Terminal GUI: list, search, sort, insert, extract (built; not yet seen on a client) (w:5)

## Machines and crafting
- [x] Interface Node feeding a machine and Import Node returning products (furnace checked headless) (w:4)
- [~] Pattern Terminal, Circuit Patterns, Assembler (built; GUI not yet seen on a client) (w:5)
- [x] CPU and RAM blades, Thread Exhaustion and Out of Memory halts, resume on new hardware (w:5)

## Art and models
- [~] Original textures drawn by script; simple block models (w:3)
- [ ] Blockbench models and detailed textures for rack, drives, cables, nodes, terminals, assembler (w:5)
- [ ] GUI frames seen on a client on all six layers (w:4)

## Compatibility
- [ ] SMWTI plugin and Jade plugin (NeoForge) (w:3)
- [ ] Item viewers BEI, JEI, REI, EMI shown per version (w:3)

## Docs, branding, release
- [ ] Branding: icon, banners, LISTING.md, curseforge/DESCRIPTION.md (w:3)
- [ ] Docs, README, wiki pages in the site repo (w:3)
- [ ] CI build.yml, release.yml, publish script, issue templates (w:3)
