# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-06
Summary: Server-room storage network (racks, drives, bus cables, nodes with upgrades and filters, terminals, patterns, CPU/RAM-limited crafting) on Forge and NeoForge 26.1.2, 26.2 and 26.3; code, headless self test and unit tests pass on all six layers, client frames checked on NeoForge 26.2 only; branding, docs, wiki, CI and release still to do.
Spend: none

## Foundation
- [x] Layout: shared common/ plus master version-26.2 code synced to 26.1.2 and 26.3, and six loader layers (w:3)
- [x] Gradle builds compile on Forge and NeoForge for 26.1.2, 26.2 and 26.3 (w:4)
- [x] 32 unit tests for storage, priority routing, graph, planner (including tag substitutes), scheduler, persistence and power (w:4)
- [x] Headless server self test, 32 checks, SELFTEST DONE failures=0 on all six layers (w:4)
- [ ] Combined jar per Minecraft version with a dev-tools-out-of-release check (w:3)
- [~] GitHub repository (public repo exists, first commit pushed; later commits local until the PM pushes) (w:1)

## Core
- [x] Server Rack with FE port, drive bays, blade slots, upkeep and offline state (w:5)
- [x] Storage cartridges in five tiers keeping contents in the item (w:4)
- [x] Rack Bus Cable (straight tube) linking racks, nodes, terminals and assemblers; nodes draw an arm toward the cable (w:3)
- [x] Storage Bus with High/Normal/Low priority, filter and upgrades (headless check) (w:4)
- [x] Storage Terminal: scrolling, search, sort, view, three heights, theme, glyph buttons, craft request window (seen on NeoForge 26.2) (w:5)

## Machines and crafting
- [x] Interface Node (patterns for its machine, buffer, upgrades) and Import and Export Nodes with filters (furnace and chest checked headless) (w:5)
- [x] Assembler with patterns and upgrade slots (headless) (w:4)
- [x] Upgrade cards: Speed, Capacity, Efficiency (rack upkeep), Filter (Efficiency effect not built yet, see docs) (w:3)
- [~] Crafting Terminal: grid from network and inventory, red missing ingredients (seen on NeoForge 26.2; craft click not exercised in a client) (w:4)
- [~] Pattern Terminal: grid, processing outputs, Tags toggle, encode (seen on NeoForge 26.2; encode click not exercised in a client) (w:4)
- [x] Crafting Queue Terminal with progress, halt error and cancel (seen on NeoForge 26.2) (w:3)
- [x] CPU and RAM blades, Thread Exhaustion and Out of Memory halts, resume on new hardware (w:5)

## Art and models
- [x] Blockbench models for rack, terminals, assembler, cable and nodes inside 16x16x16, 3D item views, straight cable with matching hit boxes (headless and unit checks only; not yet seen in a client) (w:5)
- [x] GUI font Liberation Sans (OFL) as a TrueType font, flat dark and light themes (w:2)
- [~] GUI frames seen on a client: NeoForge 26.2 all screens; other five layers not yet (w:4)

## Compatibility
- [~] SMWTI plugin for rack, nodes and assembler (loaded from the Maven mirror; lines not yet viewed) (w:3)
- [~] JEI and REI plus buttons for the Crafting and Pattern terminals (NeoForge layers; plugins load, plus click not exercised) (w:4)
- [x] BEI plus button (seen on NeoForge 26.2: enabled in the Pattern Terminal, fills the grid, hidden in the inventory; other layers compile only); search sync with BEI (seen on NeoForge 26.2 both ways), JEI and REI hooked (compile only); EMI (no build for 26.x) and Jade plugin still open (w:3)
- [ ] Item viewers show items and recipes per version (checked loading only) (w:3)

## Docs, branding, release
- [ ] Branding: icon, banners, LISTING.md, curseforge/DESCRIPTION.md (w:3)
- [ ] Docs, README, wiki pages in the site repo (w:3)
- [ ] CI build.yml, release.yml, publish script, issue templates (w:3)
