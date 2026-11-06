"""Copies the master Minecraft code (version-26.1.2, shared by 26.1.2 and 26.2) to version-26.2 and version-26.3, and the
NeoForge/Forge loader sources to every layer. 26.3 removed the block codec() method, so those lines are stripped there.

Usage: python scripts/sync_versions.py
"""
import os
import re
import shutil

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MC = ["26.2", "26.3"]


def copy_tree(src, dst, transform=None):
    for base, _, files in os.walk(src):
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(base, name)
            rel = os.path.relpath(path, src)
            out = os.path.join(dst, rel)
            os.makedirs(os.path.dirname(out), exist_ok=True)
            text = open(path, encoding="utf-8").read()
            if transform:
                text = transform(text)
            with open(out, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)


def strip_codec(text):
    text = re.sub(r"\n    private static final MapCodec<[^\n]*\n", "\n", text)
    text = re.sub(r"\n    @Override\n    protected MapCodec<[^\n]*\{\n        return CODEC;\n    \}\n", "\n", text)
    text = text.replace("import com.mojang.serialization.MapCodec;\n", "")
    return text


def main():
    master = os.path.join(ROOT, "version-26.1.2", "src", "main", "java")
    for v in MC:
        dst = os.path.join(ROOT, f"version-{v}", "src", "main", "java")
        shutil.rmtree(dst, ignore_errors=True)
        copy_tree(master, dst, strip_codec if v == "26.3" else None)
    for loader in ("neoforge", "forge"):
        src = os.path.join(ROOT, f"{loader}-26.1.2", "src")
        if not os.path.isdir(src):
            continue
        for v in MC:
            for kind in ("main", "dev"):
                s = os.path.join(src, kind, "java")
                if os.path.isdir(s):
                    d = os.path.join(ROOT, f"{loader}-{v}", "src", kind, "java")
                    shutil.rmtree(d, ignore_errors=True)
                    copy_tree(s, d)
    print("synced")


if __name__ == "__main__":
    main()
