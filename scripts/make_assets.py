"""Writes the resource files of BarelyStorage: textures, models, blockstates, item definitions, language, recipes, loot tables, tags.

Usage: python scripts/make_assets.py [--no-models]   (--no-models keeps the Blockbench-exported block models already on disk)
Shared files go to common/src/main/resources; the per-version data (item definitions, recipes, loot) goes to every version-<mc> folder.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import art  # noqa: E402
import icons  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MOD = "barelystorage"
COMMON = os.path.join(ROOT, "common", "src", "main", "resources")
ASSETS = os.path.join(COMMON, "assets", MOD)
VERSIONS = {"26.1.2": "old", "26.2": "old", "26.3": "new"}
KEEP_MODELS = "--no-models" in sys.argv


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        if isinstance(data, str):
            f.write(data)
        else:
            json.dump(data, f, indent=2)
            f.write("\n")


BLOCKS = ["server_rack", "rack_bus_cable", "storage_bus", "interface_node", "import_node", "export_node", "storage_terminal", "crafting_terminal", "queue_terminal", "pattern_terminal", "assembler"]
ITEMS = ["upgrade_speed", "upgrade_capacity", "upgrade_efficiency", "upgrade_filter", "cartridge_iron", "cartridge_steel", "cartridge_gold", "cartridge_obsidian", "cartridge_quantum",
         "basic_core", "ram_blade", "quantum_core", "blank_pattern", "crafting_pattern", "processing_pattern"]
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def model_ref(name):
    return f"{MOD}:block/{name}"


def blockstates():
    bs = os.path.join(ASSETS, "blockstates")
    # Horizontal-facing machines: (block, {state-suffix: model})
    for block, models in {
        "server_rack": ("online", {"false": "server_rack_off", "true": "server_rack_on"}),
        "assembler": ("active", {"false": "assembler_idle", "true": "assembler_active"}),
    }.items():
        prop, m = models
        variants = {}
        for facing, y in FACING_Y.items():
            for val, model in m.items():
                v = {"model": model_ref(model)}
                if y:
                    v["y"] = y
                variants[f"facing={facing},{prop}={val}" if block == "server_rack" else f"{prop}={val}"] = v
        write(os.path.join(bs, block + ".json"), {"variants": variants})
    # The assembler has no facing: rewrite plainly.
    write(os.path.join(bs, "assembler.json"), {"variants": {
        "active=false": {"model": model_ref("assembler_idle")}, "active=true": {"model": model_ref("assembler_active")}}})
    for block in ("storage_terminal", "crafting_terminal", "queue_terminal", "pattern_terminal"):
        variants = {}
        for facing, y in FACING_Y.items():
            v = {"model": model_ref(block)}
            if y:
                v["y"] = y
            variants[f"facing={facing}"] = v
        write(os.path.join(bs, block + ".json"), {"variants": variants})
    for block in ("storage_bus", "interface_node", "import_node", "export_node"):
        rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
        parts = []
        for facing, r in rot.items():
            parts.append({"when": {"facing": facing}, "apply": dict({"model": model_ref(block)}, **r)})
        for d, r in rot.items():
            parts.append({"when": {d: "true"}, "apply": dict({"model": model_ref("rack_bus_cable_arm")}, **r)})
        write(os.path.join(bs, block + ".json"), {"multipart": parts})
    parts = [{"apply": {"model": model_ref("rack_bus_cable_core")}}]
    for d, r in {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}.items():
        parts.append({"when": {d: "true"}, "apply": dict({"model": model_ref("rack_bus_cable_arm")}, **r)})
    write(os.path.join(bs, "rack_bus_cable.json"), {"multipart": parts})


def box(frm, to, tex, faces=None):
    f = {}
    for side in ("north", "south", "east", "west", "up", "down"):
        f[side] = {"uv": [frm[0], frm[1], to[0], to[1]], "texture": tex}
    return {"from": frm, "to": to, "faces": f}


def models():
    mb = os.path.join(ASSETS, "models", "block")
    mi = os.path.join(ASSETS, "models", "item")
    for on in ("off", "on"):
        write(os.path.join(mb, f"server_rack_{on}.json"), {"parent": "minecraft:block/orientable", "textures": {
            "front": f"{MOD}:block/server_rack_front_{on}", "side": f"{MOD}:block/server_rack_side", "top": f"{MOD}:block/server_rack_top"}})
    for kind in ("storage", "pattern"):
        write(os.path.join(mb, f"{kind}_terminal.json"), {"parent": "minecraft:block/orientable", "textures": {
            "front": f"{MOD}:block/{kind}_terminal_front", "side": f"{MOD}:block/terminal_side", "top": f"{MOD}:block/terminal_side"}})
    for st in ("idle", "active"):
        write(os.path.join(mb, f"assembler_{st}.json"), {"parent": "minecraft:block/cube", "textures": {
            "north": f"{MOD}:block/assembler_front_{st}", "south": f"{MOD}:block/assembler_side", "east": f"{MOD}:block/assembler_side",
            "west": f"{MOD}:block/assembler_side", "up": f"{MOD}:block/assembler_top", "down": f"{MOD}:block/assembler_top",
            "particle": f"{MOD}:block/assembler_side"}})
    tex = f"{MOD}:block/rack_bus_cable"
    write(os.path.join(mb, "rack_bus_cable_core.json"), {"textures": {"0": tex, "particle": tex}, "elements": [
        {"from": [5, 5, 5], "to": [11, 11, 11], "faces": {s: {"uv": [5, 5, 11, 11], "texture": "#0"} for s in ("north", "south", "east", "west", "up", "down")}}]})
    write(os.path.join(mb, "rack_bus_cable_arm.json"), {"textures": {"0": tex, "particle": tex}, "elements": [
        {"from": [6, 6, 0], "to": [10, 10, 5], "faces": {s: {"uv": [6, 0, 10, 5] if s in ("east", "west", "up", "down") else [6, 6, 10, 10], "texture": "#0"}
                                                    for s in ("north", "south", "east", "west", "up", "down")}},
        {"from": [5, 5, 3], "to": [11, 11, 4], "faces": {s: {"uv": [5, 5, 11, 11], "texture": "#0"} for s in ("north", "south", "east", "west", "up", "down")}}]})
    write(os.path.join(mi, "rack_bus_cable.json"), {"textures": {"0": tex, "particle": tex}, "elements": [
        {"from": [0, 6, 5], "to": [16, 10, 11], "faces": {s: {"uv": [0, 5, 16, 11], "texture": "#0"} for s in ("north", "south", "east", "west", "up", "down")}}]})
    for node in ("storage_bus", "interface_node", "import_node"):
        t = f"{MOD}:block/{node}"
        write(os.path.join(mb, node + ".json"), {"textures": {"0": t, "particle": t}, "elements": [
            {"from": [2, 2, 0], "to": [14, 14, 3], "faces": {s: {"uv": [2, 2, 14, 14] if s in ("north", "south") else [2, 0, 14, 3], "texture": "#0"} for s in ("north", "south", "east", "west", "up", "down")}},
            {"from": [5, 5, 3], "to": [11, 11, 5], "faces": {s: {"uv": [5, 5, 11, 11], "texture": "#0"} for s in ("north", "south", "east", "west", "up", "down")}}]})


def item_defs(version):
    base = os.path.join(ROOT, f"version-{version}", "src", "main", "resources", "assets", MOD, "items")
    for b in BLOCKS:
        model = f"{MOD}:block/{b}"
        if b == "server_rack":
            model = f"{MOD}:block/server_rack_off"
        elif b == "assembler":
            model = f"{MOD}:block/assembler_idle"
        elif b == "rack_bus_cable":
            model = f"{MOD}:item/rack_bus_cable"
        write(os.path.join(base, b + ".json"), {"model": {"type": "minecraft:model", "model": model}})
    for i in ITEMS:
        write(os.path.join(base, i + ".json"), {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{i}"}})


def item_models():
    mi = os.path.join(ASSETS, "models", "item")
    for i in ITEMS:
        write(os.path.join(mi, i + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{i}"}})


def shaped(pattern, key, result, count=1, category="misc"):
    return {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
            "result": {"id": f"{MOD}:{result}", "count": count}}


def shapeless(ingredients, result, count=1, category="misc"):
    return {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
            "result": {"id": f"{MOD}:{result}", "count": count}}


def m(x):
    return x if ":" in x else f"minecraft:{x}"


def recipes():
    cable = f"{MOD}:rack_bus_cable"
    return {
        "server_rack": shaped(["IHI", "IRI", "IHI"], {"I": m("iron_ingot"), "H": m("hopper"), "R": m("redstone_block")}, "server_rack"),
        "rack_bus_cable": shaped(["RIR"], {"R": m("redstone"), "I": m("iron_ingot")}, "rack_bus_cable", 4),
        "storage_bus": shaped([" C ", "IHI", " R "], {"C": cable, "I": m("iron_ingot"), "H": m("hopper"), "R": m("redstone")}, "storage_bus"),
        "interface_node": shaped([" C ", "IDI", " R "], {"C": cable, "I": m("iron_ingot"), "D": m("dropper"), "R": m("redstone")}, "interface_node"),
        "import_node": shaped([" C ", "IHI", " Q "], {"C": cable, "I": m("iron_ingot"), "H": m("hopper"), "Q": m("quartz")}, "import_node"),
        "export_node": shaped([" C ", "IHI", " G "], {"C": cable, "I": m("iron_ingot"), "H": m("hopper"), "G": m("gold_ingot")}, "export_node"),
        "upgrade_speed": shaped(["GRG", "RPR", "GRG"], {"G": m("gold_ingot"), "R": m("redstone"), "P": f"{MOD}:blank_pattern"}, "upgrade_speed"),
        "upgrade_capacity": shaped(["LRL", "RPR", "LRL"], {"L": m("lapis_lazuli"), "R": m("redstone"), "P": f"{MOD}:blank_pattern"}, "upgrade_capacity"),
        "upgrade_efficiency": shaped(["QRQ", "RPR", "QRQ"], {"Q": m("quartz"), "R": m("redstone"), "P": f"{MOD}:blank_pattern"}, "upgrade_efficiency"),
        "upgrade_filter": shaped(["NRN", "RPR", "NRN"], {"N": m("iron_nugget"), "R": m("redstone"), "P": f"{MOD}:blank_pattern"}, "upgrade_filter"),
        "storage_terminal": shaped(["IGI", "RBR", "III"], {"I": m("iron_ingot"), "G": m("glass_pane"), "R": m("redstone"), "B": cable}, "storage_terminal"),
        "crafting_terminal": shapeless([f"{MOD}:storage_terminal", m("crafting_table")], "crafting_terminal"),
        "queue_terminal": shapeless([f"{MOD}:storage_terminal", m("clock"), m("redstone")], "queue_terminal"),
        "pattern_terminal": shapeless([f"{MOD}:storage_terminal", m("crafting_table"), f"{MOD}:blank_pattern"], "pattern_terminal"),
        "assembler": shaped(["IBI", "ITI", "IRI"], {"I": m("iron_ingot"), "B": cable, "T": m("crafting_table"), "R": m("redstone_block")}, "assembler"),
        "cartridge_iron": shaped(["IRI", "ICI", "III"], {"I": m("iron_ingot"), "R": m("redstone"), "C": m("chest")}, "cartridge_iron"),
        "cartridge_steel": shaped(["IXI", "XCX", "IXI"], {"I": m("iron_block"), "X": m("redstone"), "C": f"{MOD}:cartridge_iron"}, "cartridge_steel"),
        "cartridge_gold": shaped(["GXG", "XCX", "GXG"], {"G": m("gold_ingot"), "X": m("redstone_block"), "C": f"{MOD}:cartridge_steel"}, "cartridge_gold"),
        "cartridge_obsidian": shaped(["OXO", "XCX", "OXO"], {"O": m("obsidian"), "X": m("gold_block"), "C": f"{MOD}:cartridge_gold"}, "cartridge_obsidian"),
        "cartridge_quantum": shaped(["DED", "ECE", "DED"], {"D": m("diamond_block"), "E": m("ender_eye"), "C": f"{MOD}:cartridge_obsidian"}, "cartridge_quantum"),
        "blank_pattern": shapeless([m("paper"), m("redstone"), m("iron_nugget")], "blank_pattern", 2),
        "basic_core": shaped(["IGI", "GRG", "IGI"], {"I": m("iron_ingot"), "G": m("gold_ingot"), "R": m("redstone_block")}, "basic_core"),
        "ram_blade": shaped(["LQL", "QCQ", "LQL"], {"L": m("lapis_lazuli"), "Q": m("quartz"), "C": f"{MOD}:basic_core"}, "ram_blade"),
        "quantum_core": shaped(["DPD", "PCP", "DPD"], {"D": m("diamond"), "P": m("ender_pearl"), "C": f"{MOD}:ram_blade"}, "quantum_core"),
    }


def loot(block, style):
    if style == "old":
        pool = {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]}
    else:
        pool = {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                "entries": [{"type": "minecraft:item", "name": f"{MOD}:{block}"}]}
    return {"type": "minecraft:block", "pools": [pool], "random_sequence": f"{MOD}:blocks/{block}"}


def data(version, style):
    base = os.path.join(ROOT, f"version-{version}", "src", "main", "resources", "data", MOD)
    for name, r in recipes().items():
        write(os.path.join(base, "recipe", name + ".json"), r)
    for b in BLOCKS:
        write(os.path.join(base, "loot_table", "blocks", b + ".json"), loot(b, style))


def tags():
    write(os.path.join(COMMON, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json"),
          {"replace": False, "values": [f"{MOD}:{b}" for b in BLOCKS]})


LANG = {
    "itemGroup.barelystorage": "BarelyStorage",
    "block.barelystorage.server_rack": "Server Rack",
    "block.barelystorage.rack_bus_cable": "Rack Bus Cable",
    "block.barelystorage.storage_bus": "Storage Bus",
    "block.barelystorage.interface_node": "Interface Node",
    "block.barelystorage.import_node": "Import Node",
    "block.barelystorage.export_node": "Export Node",
    "block.barelystorage.storage_terminal": "Storage Terminal",
    "block.barelystorage.pattern_terminal": "Pattern Terminal",
    "block.barelystorage.crafting_terminal": "Crafting Terminal",
    "block.barelystorage.queue_terminal": "Crafting Queue Terminal",
    "block.barelystorage.assembler": "Assembler",
    "item.barelystorage.upgrade_speed": "Speed Upgrade",
    "item.barelystorage.upgrade_capacity": "Capacity Upgrade",
    "item.barelystorage.upgrade_efficiency": "Efficiency Upgrade",
    "item.barelystorage.upgrade_filter": "Filter Upgrade",
    "gui.barelystorage.buffer": "Buffer",
    "gui.barelystorage.crafting_patterns": "Crafting patterns",
    "gui.barelystorage.processing_patterns": "Processing patterns",
    "gui.barelystorage.import_filter": "Filter",
    "gui.barelystorage.export_filter": "Filter",
    "gui.barelystorage.bus_filter": "Filter",
    "gui.barelystorage.priority": "Priority: %s",
    "item.barelystorage.cartridge_iron": "Iron Drive",
    "item.barelystorage.cartridge_steel": "Steel Drive",
    "item.barelystorage.cartridge_gold": "Gold Drive",
    "item.barelystorage.cartridge_obsidian": "Obsidian Drive",
    "item.barelystorage.cartridge_quantum": "Quantum Drive",
    "item.barelystorage.basic_core": "Basic Core",
    "item.barelystorage.ram_blade": "Multi-Threaded RAM Blade",
    "item.barelystorage.quantum_core": "Quantum Core",
    "item.barelystorage.blank_pattern": "Blank Circuit Pattern",
    "item.barelystorage.crafting_pattern": "Crafting Pattern",
    "item.barelystorage.processing_pattern": "Processing Pattern",
    "tooltip.barelystorage.cartridge_items": "%s / %s items",
    "tooltip.barelystorage.cartridge_types": "%s / %s item types",
    "tooltip.barelystorage.blade_stats": "+%s threads, +%s RAM",
    "tooltip.barelystorage.pattern_in": "In: %s x %s",
    "tooltip.barelystorage.pattern_out": "Out: %s x %s",
    "message.barelystorage.priority": "Storage Bus priority: %s",
    "message.barelystorage.node_online": "Import Node: network online",
    "message.barelystorage.node_offline": "Import Node: network offline",
    "message.barelystorage.craft_started": "Crafting %s x %s queued (%s steps)",
    "message.barelystorage.craft_failed": "Cannot craft: %s",
    "gui.barelystorage.power": "Power %s / %s FE   -%s/t",
    "gui.barelystorage.storage": "Storage %s / %s items",
    "gui.barelystorage.cpu": "Threads %s, RAM %s",
    "gui.barelystorage.depth": "Max chain depth %s",
    "gui.barelystorage.drive_bays": "Drive bays",
    "gui.barelystorage.blades": "Blades",
    "gui.barelystorage.no_jobs": "No crafting jobs",
    "gui.barelystorage.jobs": "Jobs %s   steps %s / %s",
    "gui.barelystorage.error_no_core": "No Core installed: crafting halted",
    "gui.barelystorage.error_threads": "Thread Exhaustion: needs %s threads, has %s",
    "gui.barelystorage.error_depth": "Thread Exhaustion: chain depth %s, max %s",
    "gui.barelystorage.error_ram": "Out of Memory: needs %s RAM, has %s",
    "gui.barelystorage.error_other": "Crafting halted",
    "gui.barelystorage.search": "Search",
    "gui.barelystorage.tip_height": "Height: %s",
    "gui.barelystorage.tip_theme": "Theme: %s",
    "gui.barelystorage.tip_sort": "Sort by: %s",
    "gui.barelystorage.tip_order": "Order: %s",
    "gui.barelystorage.tip_view": "View: %s",
    "gui.barelystorage.tip_sync": "Sync search",
    "gui.barelystorage.size_0": "Small",
    "gui.barelystorage.size_1": "Medium",
    "gui.barelystorage.size_2": "Large",
    "gui.barelystorage.sort_full_0": "Quantity",
    "gui.barelystorage.sort_full_1": "Mod name",
    "gui.barelystorage.sort_full_2": "Item name",
    "gui.barelystorage.height": "Height",
    "gui.barelystorage.theme": "Theme",
    "gui.barelystorage.theme_dark": "Dark",
    "gui.barelystorage.theme_light": "Light",
    "gui.barelystorage.sort_by": "Sort by",
    "gui.barelystorage.sort_quantity": "Qty",
    "gui.barelystorage.sort_mod": "Mod",
    "gui.barelystorage.sort_name": "Name",
    "gui.barelystorage.order": "Order",
    "gui.barelystorage.order_asc": "Ascending",
    "gui.barelystorage.order_desc": "Descending",
    "gui.barelystorage.view_both": "Crafting + Available",
    "gui.barelystorage.view_available": "Available only",
    "gui.barelystorage.view_craftable": "Craftable only",
    "gui.barelystorage.start": "Start crafting",
    "gui.barelystorage.cancel": "Cancel",
    "gui.barelystorage.tip_add": "Add %s",
    "gui.barelystorage.tip_remove": "Remove %s",
    "gui.barelystorage.craft_title": "Craft",
    "gui.barelystorage.craft_amount": "Amount: %s",
    "gui.barelystorage.offline": "Network offline",
    "gui.barelystorage.craftable": "Craftable",
    "gui.barelystorage.stored": "Stored: %s",
    "gui.barelystorage.encode": "Encode",
    "gui.barelystorage.tags": "Tags",
    "gui.barelystorage.tags_tip": "Tags",
    "gui.barelystorage.queue_empty": "No crafting jobs",
    "gui.barelystorage.queue_running": "Running",
    "gui.barelystorage.queue_waiting": "Waiting",
    "gui.barelystorage.queue_halted": "Halted",
    "gui.barelystorage.queue_cancel": "Cancel job",
    "gui.barelystorage.error_depth_short": "Thread Exhaustion: chain depth %s, max %s",
    "gui.barelystorage.missing": "Missing ingredient",
    "gui.barelystorage.clear": "Clear",
    "gui.barelystorage.mode_crafting": "Crafting",
    "gui.barelystorage.mode_processing": "Processing",
    "gui.barelystorage.inputs": "Inputs",
    "gui.barelystorage.outputs": "Outputs",
    "gui.barelystorage.result": "Result",
    "gui.barelystorage.blank": "Blank",
    "gui.barelystorage.encoded": "Encoded",
    "smwti.barelystorage.rack_upkeep": "Upkeep: %s FE/t",
    "smwti.barelystorage.rack_drives": "Drives: %s used of %s, %s cartridges",
    "smwti.barelystorage.rack_cpu": "CPU: %s threads, %s RAM, depth %s",
    "smwti.barelystorage.rack_offline": "Offline: no power",
    "smwti.barelystorage.rack_error": "Error: %s",
    "smwti.barelystorage.priority": "Priority: %s",
    "smwti.barelystorage.buffer": "Buffer: %s items",
    "smwti.barelystorage.assembler_busy": "Crafting (%s ticks left)",
    "smwti.barelystorage.assembler_idle": "Idle, %s patterns",
}


def main():
    icons.write_all(os.path.join(ASSETS, "textures", "gui", "icon"))
    blocks, items = art.write_all(os.path.join(ASSETS, "textures", "block"), os.path.join(ASSETS, "textures", "item"))
    if not KEEP_MODELS:
        models()
    blockstates()
    item_models()
    write(os.path.join(ASSETS, "lang", "en_us.json"), LANG)
    tags()
    for v, style in VERSIONS.items():
        item_defs(v)
        data(v, style)
    print(f"wrote {len(blocks)} block textures, {len(items)} item textures, {len(LANG)} lang keys")


if __name__ == "__main__":
    main()
