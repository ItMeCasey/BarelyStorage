# Changelog

## 2026.11.5

- Changed: the settings now use the mod loader's standard config files, `config/barelystorage-common.toml` (every number, with comments and ranges) and `config/barelystorage-client.toml` (the terminal's theme, height, sort, order and view). NeoForge also has the in-game config screen. Values reach the game when the file loads or is saved.
- Migrating: on the first start where a `.toml` does not exist, the values from `config/barelystorage.properties` and `config/barelystorage-client.properties` are copied into it and checked, then each old file is renamed to the same name with `.back` added (with a first line saying where and why it migrated, then the original content; an existing `.back` is never overwritten, the next name is `.back2`). A corrupt or only partly readable old file stays where it is, with no `.back`. Invalid values use the default and out-of-range values are moved into the new range; both are logged.

## 2026.11.4

- New: optional ModJS scripting. Server scripts get a global `BarelyStorage` with events (rack online and offline, craft queued, failed and halted, pattern encoded, items taken), read access to a network (power, hardware, drives, items), an item insert filter and settings. See docs/scripting.md.
- Removed the hint texts: tooltip lines on upgrade cards, blades and blank or empty patterns, the explanations in the filter and tag labels, the sync button and craftable item tooltips, and the explanation in the offline Import Node line.

## 2026.11.3

- Fixed: with BEI 2026.11.8 or newer, which also runs JEI plugins, the terminal's search sync could follow BEI's JEI bridge instead of BEI's own search box. BEI now wins over REI and JEI when several are installed.
- Checked in a real client since 2026.11.2: take, craft and encode clicks (NeoForge 26.2), the plus buttons of JEI (NeoForge 26.1.2, 26.2, 26.3), REI (26.1.2, 26.2, 26.3) and BEI (NeoForge 26.1.2, 26.2, Forge 26.2), BEI search sync (NeoForge 26.2, Forge 26.2) and the BEI storage source (NeoForge 26.2).

## 2026.11.2

- Fixed: the top face of the block under a Server Rack, a terminal or an Assembler was invisible (see-through). These blocks now use no full-cube occlusion, like the cable and the nodes.
- The Efficiency upgrade card now lowers rack upkeep (10 percent per card in the network, up to 50 percent).
- JEI plus button fixed for the Crafting and Pattern Terminals; REI and BEI plus buttons and search sync added.
- Smaller icons and buttons, Silkscreen font for titles and labels, per-GUI-scale crisp icon sprites.
- Jade plugin for the rack and nodes (NeoForge).
- BEI storage source: BEI's Master Blueprint counts the items of the open terminal list (the rows the client has on screen).

## 2026.11.1

- First build: Server Rack, drives, bus cable, nodes, terminals, Assembler, upgrades, CPU and RAM blades.
