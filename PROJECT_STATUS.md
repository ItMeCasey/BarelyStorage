# Project Status
<!-- status-format: 1 -->
Updated: 2026-11-14
Summary: Server-room storage network (racks, drives, bus cables, nodes with upgrades and filters, terminals, patterns, CPU/RAM-limited crafting) on Forge and NeoForge 26.1.2, 26.2 and 26.3; code, headless self test and unit tests pass on all six layers, client frames checked on NeoForge and Forge 26.2; ModJS scripting added in 2026.11.4, release still to do.
Spend: none

## Foundation
- [x] Layout: shared common/ plus master version-26.2 code synced to 26.1.2 and 26.3, and six loader layers (w:3)
- [x] Gradle builds compile on Forge and NeoForge for 26.1.2, 26.2 and 26.3 (w:4)
- [x] 49 unit tests for storage, priority routing, graph, planner (including tag substitutes), scheduler, persistence and power (w:4)
- [x] Headless server self test, 39 checks, SELFTEST DONE failures=0, with and without the ModJS jar (w:4)
- [x] Combined jar per Minecraft version with a dev-tools-out-of-release check (CI dry run and release built three jars, none with dev files) (w:3)
- [x] GitHub repository ItMeCasey/BarelyStorage with CI, tags v2026.11.1 and v2026.11.2 and pre-releases (w:1)

## Core
- [x] Server Rack with FE port, drive bays, blade slots, upkeep and offline state (w:5)
- [x] Storage cartridges in five tiers keeping contents in the item (w:4)
- [x] Rack Bus Cable (straight tube) linking racks, nodes, terminals and assemblers; nodes draw an arm toward the cable (w:3)
- [x] Storage Bus with High/Normal/Low priority, filter and upgrades (headless check) (w:4)
- [x] Storage Terminal: scrolling, search, sort, view, three heights, theme, glyph buttons, craft request window (seen on NeoForge 26.2) (w:5)

## Machines and crafting
- [x] Interface Node (patterns for its machine, buffer, upgrades) and Import and Export Nodes with filters (furnace and chest checked headless) (w:5)
- [x] Assembler with patterns and upgrade slots (headless) (w:4)
- [x] Upgrade cards: Speed, Capacity, Efficiency (10% off every rack's upkeep per card in the network, max 50%, unit tests and self test), Filter (w:3)
- [x] Crafting Terminal: grid from network and inventory, red missing ingredients, result click sent by the client crafts bread (seen on NeoForge 26.2) (w:4)
- [x] Pattern Terminal: grid, processing outputs, Tags toggle, encode button sent by the client makes a crafting pattern (seen on NeoForge 26.2) (w:4)
- [x] Crafting Queue Terminal with progress, halt error and cancel (seen on NeoForge 26.2) (w:3)
- [x] CPU and RAM blades, Thread Exhaustion and Out of Memory halts, resume on new hardware (w:5)

## Art and models
- [x] Blockbench models for rack, terminals, assembler, cable and nodes inside 16x16x16, 3D item views, straight cable with matching hit boxes (seen in the room and stack tours on all six layers) (w:5)
- [x] GUI fonts: Silkscreen (OFL) for titles and labels, Liberation Sans (OFL) for numbers and names, flat dark and light themes, per-GUI-scale icon sprites (w:2)
- [x] GUI frames seen on a client: all 22 tour frames on NeoForge 26.2, and tours passed on the other five layers (frames read: room overview and terminals) (w:4)

## Compatibility
- [x] SMWTI plugin: Server Rack lines (energy, power, drives, CPU) seen on all six layers; Assembler, Interface Node, Storage Bus and Import Node lines seen on NeoForge 26.2 (rack line is Upkeep, no duplicate power bar); Jade plugin for rack and nodes on the NeoForge layers (rack and interface node seen on NeoForge 26.2) (w:3)
- [x] JEI and REI plus buttons for the Crafting and Pattern terminals (NeoForge layers; the handler call for the Bread recipe was run in a real client on NeoForge 26.2 and fills the grid for both viewers; JEI needed a fix: one handler for all terminal menus; the other NeoForge versions compile only) (w:4)
- [x] BEI plus button (seen on NeoForge 26.2: enabled in the Pattern Terminal, fills the grid, hidden in the inventory; other layers compile only); BEI storage source for the Master Blueprint (compiles on all six; counts the open terminal list only); search sync with BEI (seen on NeoForge 26.2 both ways), JEI and REI hooked (compile only); EMI (no build for 26.x) and Jade plugin still open (w:3)
- [x] Item viewers: JEI plus run in a client on NeoForge 26.1.2, 26.2, 26.3; REI plus on NeoForge 26.1.2, 26.2, 26.3; BEI plus on NeoForge 26.1.2, 26.2 and Forge 26.2; BEI search sync (needed a fix, see 2026.11.3) and BEI storage source on NeoForge 26.2, search sync also Forge 26.2. JEI and REI have no Forge 26.x plugin in this mod (their Forge builds do not exist for these versions); BEI on NeoForge 26.3 and Forge 26.1.2/26.3 share the code of the checked builds (w:3)

## Scripting and cleanup
- [x] ModJS scripting support: BarelyStorage global (events, network read, insert filter, settings), unit tests, headless self test, docs/scripting.md and wiki page (w:3)
- [x] Hints removed from tooltips, labels and messages (w:2)

## Config
- [x] Loader config files (barelystorage-common.toml, barelystorage-client.toml) from one key list, NeoForge config screen, old .properties moved to .back with unit tests on real fixtures from every release (w:3)

## Docs, branding, release
- [x] Branding: icon, banners, LISTING.md, curseforge/DESCRIPTION.md (w:3)
- [x] Docs, README, wiki pages in the site repo (21 pages, deployed and checked live 2026-11-06) (w:3)
- [x] CI build.yml, release.yml, publish script, issue templates (green builds, dry run and release run) (w:3)
- [~] CI builds and releases only the modules a change affects (.github/scripts/plan_matrix.py with unit tests; committed locally, not yet run on GitHub) (w:2)
- [x] First release v2026.11.1: GitHub prerelease and three CurseForge betas (project 1786861, file ids 9472566-9472568) (w:2)
- [x] CurseForge approval: project and the three 2026.11.1 files are public (checked 2026-11-06) (w:1)
- [ ] Release 2026.11.5 (config files and migration; PM tags it) (w:1)
- [x] Release v2026.11.3: search sync priority fix, tagged and released (w:1)
- [x] Release v2026.11.2: GitHub pre-release (see-through block fix, Efficiency cards, JEI fix, BEI/REI/Jade, new icons and fonts, BEI storage source) (w:2)
