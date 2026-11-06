"""Copies the master Minecraft code (version-26.2/src/{main,dev}/java) to version-26.1.2 and version-26.3, and the loader sources
(forge-26.1.2, neoforge-26.1.2) to the other loader layers.

Edit the code in version-26.2; never edit the copies. The few API differences are applied here:
  - 26.3 removed the block codec() method and moved dropping an item at a player to Containers.
  - 26.1.2 has the older Minecraft accessors the dev screenshot tour uses (screen, overlay, toasts, render target).

Usage: python scripts/sync_versions.py
"""
import os
import re
import shutil
import time

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MASTER = "26.2"


def copy_tree(src, dst, transform=None):
    written = set()
    for base, _, files in os.walk(src):
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(base, name)
            rel = os.path.relpath(path, src)
            out = os.path.join(dst, rel)
            written.add(os.path.abspath(out))
            os.makedirs(os.path.dirname(out), exist_ok=True)
            text = open(path, encoding="utf-8").read()
            if transform:
                text = transform(text)
            with open(out, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)


def v263(text):
    text = re.sub(r"\n    private static final MapCodec<[^\n]*\n", "\n", text)
    text = re.sub(r"\n    @Override\n    protected MapCodec<[^\n]*\{\n        return CODEC;\n    \}\n", "\n", text)
    text = text.replace("import com.mojang.serialization.MapCodec;\n", "")
    if "player.drop(out, false);" in text:
        text = text.replace("player.drop(out, false);", "Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), out);")
        text = text.replace("import net.minecraft.world.Container;" + chr(10), "import net.minecraft.world.Container;" + chr(10) + "import net.minecraft.world.Containers;" + chr(10), 1)
    return text


def v2612(text):
    text = text.replace("minecraft.gui.overlay()", "minecraft.getOverlay()").replace("minecraft.gui.screen()", "minecraft.screen")
    text = text.replace("registries -> registries.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions()",
                        "WorldPresets::createFlatWorldDimensions")
    text = text.replace("minecraft.gui.hud.getChat()", "minecraft.gui.getChat()").replace("minecraft.gui.toastManager()", "minecraft.getToastManager()")
    text = text.replace("minecraft.gameRenderer.mainRenderTarget()", "minecraft.getMainRenderTarget()")
    text = text.replace("Minecraft.getInstance().gui.screen()", "Minecraft.getInstance().screen").replace("mc.gui.screen()", "mc.screen")
    text = text.replace("Minecraft.getInstance().gui.setScreen(", "Minecraft.getInstance().setScreen(")
    return text


def main():
    for v, transform in (("26.1.2", v2612), ("26.3", v263)):
        for kind in ("main", "dev"):
            src = os.path.join(ROOT, f"version-{MASTER}", "src", kind, "java")
            dst = os.path.join(ROOT, f"version-{v}", "src", kind, "java")
            if not os.path.isdir(src):
                continue
            if v == "26.1.2":
                copy_tree(src, dst, transform if kind == "dev" else None)
            else:
                copy_tree(src, dst, transform)
    for loader in ("neoforge", "forge"):
        src = os.path.join(ROOT, f"{loader}-26.1.2", "src")
        for v in ("26.2", "26.3"):
            for kind in ("main", "dev"):
                s = os.path.join(src, kind, "java")
                if os.path.isdir(s):
                    d = os.path.join(ROOT, f"{loader}-{v}", "src", kind, "java")
                    copy_tree(s, d)
    print("synced")


if __name__ == "__main__":
    main()
