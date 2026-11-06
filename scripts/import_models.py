"""Turns the Blockbench exports in art/*.json into the block and item models of the mod.

The exports come from the Blockbench MCP (java_block format). This script namespaces the texture references, adds the particle
texture, and makes the powered/active variants (texture swap and light emission). Run it after re-exporting a model, then
`python scripts/make_assets.py --no-models`.
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MOD = "barelystorage"
ART = os.path.join(ROOT, "art")
OUT = os.path.join(ROOT, "common", "src", "main", "resources", "assets", MOD, "models")


def load(name):
    with open(os.path.join(ART, name + ".json"), encoding="utf-8") as f:
        return json.load(f)


def save(kind, name, model, particle):
    textures = {k: f"{MOD}:block/{v}" for k, v in model["textures"].items()}
    textures["particle"] = f"{MOD}:block/{particle}"
    out = {"textures": textures, "elements": model["elements"]}
    os.makedirs(os.path.join(OUT, kind), exist_ok=True)
    with open(os.path.join(OUT, kind, name + ".json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=2)
        f.write("\n")


def emit(model, prefix, level):
    for el in model["elements"]:
        if el["name"].startswith(prefix):
            el["light_emission"] = level
    return model


def main():
    rack = load("server_rack")
    save("block", "server_rack_off", rack, "server_rack_side")
    on = json.loads(json.dumps(rack).replace("server_rack_front_off", "server_rack_front_on"))
    save("block", "server_rack_on", emit(on, "tray", 9), "server_rack_side")
    for kind in ("storage", "pattern"):
        save("block", f"{kind}_terminal", emit(load(f"{kind}_terminal"), "screen", 10), "terminal_side")
    storage = load("storage_terminal")
    for kind in ("crafting", "queue"):
        save("block", f"{kind}_terminal", emit(json.loads(json.dumps(storage).replace("storage_terminal_front", f"{kind}_terminal_front")), "screen", 10), "terminal_side")
    save("block", "assembler_idle", load("assembler_idle"), "assembler_side")
    save("block", "assembler_active", emit(load("assembler_active"), "window", 10), "assembler_side")
    save("block", "rack_bus_cable_core", load("rack_bus_cable_core"), "rack_bus_cable")
    save("block", "rack_bus_cable_arm", load("rack_bus_cable_arm"), "rack_bus_cable")
    save("item", "rack_bus_cable", load("rack_bus_cable_item"), "rack_bus_cable")
    for n in ("storage_bus", "interface_node", "import_node"):
        model = load(n)
        for el in model["elements"]:
            if el["name"] == "tab":
                el["from"] = [6.5, 6.5, 5]
                el["to"] = [9.5, 9.5, 9.5]
        save("block", n, model, n)
        if n == "import_node":
            save("block", "export_node", json.loads(json.dumps(model).replace("import_node", "export_node")), "export_node")
    print("imported models")


if __name__ == "__main__":
    main()
