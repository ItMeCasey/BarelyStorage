#!/usr/bin/env python3
"""Pick the builds a workflow run needs, so unchanged Minecraft versions and loaders are never built.

  plan_matrix.py build   [--before SHA] [--sha SHA] [--files PATH...]   modules whose code changed in the push
  plan_matrix.py release --tag vYYYY.M.N                                modules whose own mod_version equals the tag

Writes these lines to $GITHUB_OUTPUT (and prints them):
  matrix=<json>   {"include": [{"layer": ...}]}
  any=true|false  at least one module is picked
  count=<n>       number of picked modules
  test=true|false the unit-test job needs to run (neoforge-26.2 is picked)
  combine=<list>  Minecraft versions whose two loader layers are both picked, so a combined jar can be made (space separated)

How the modules share code (read from every build.gradle):
  common/src/main, common/src/test   compiled into every module (the build task also runs the common unit tests)
  version-<mc>/src/main              compiled into both loaders of that Minecraft version
  <layer>/src/main, libs/            the module itself (libs/ holds the BEI API jar it compiles against)
  version-<mc>/src/dev, <layer>/src/dev   only with -PdevTools (screenshot tour, probes); the CI never passes it
  scripts/combine_jars.py            makes the combined jars the release uploads
Anything not understood selects every module.
"""
import argparse
import json
import os
import re
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Same modules and order as the matrix in build.yml and release.yml (all build with Java 25).
ALL = [
    "neoforge-26.1.2",
    "forge-26.1.2",
    "neoforge-26.2",
    "forge-26.2",
    "neoforge-26.3",
    "forge-26.3",
]
NAMES = list(ALL)
MCS = ["26.1.2", "26.2", "26.3"]
TEST_MODULE = "neoforge-26.2"  # runs `gradlew test` in the test job
ZERO = "0" * 40

MODULE_DIR = re.compile(r"^(forge|neoforge)-(\d[\w.]*)/(.*)$")
VERSION_DIR = re.compile(r"^version-(\d[\w.]*)/(.*)$")
MODULE_NOISE = re.compile(r"^(build|run|runs|run-data|\.gradle|logs|crash-reports)(/|$)")
NONE_TOP = re.compile(
    r"^(art|branding|curseforge|dist|docs|logs|licenses|release|crash-reports|run[^/]*|runs|[^/]*\.log)(/|$)"
)
NONE_FILES = {"PROJECT_STATUS.md", "CHANGELOG.md", "README.md", "PLAN.md", "LICENSE", ".gitignore"}
NONE_GITHUB = re.compile(r"^\.github/(workflows|scripts|ISSUE_TEMPLATE)/")
# Generators and dev checks run by hand; no gradle file and no workflow step uses them (checked).
NONE_SCRIPTS = {
    "scripts/art.py", "scripts/client_tour.py", "scripts/icons.py", "scripts/import_models.py",
    "scripts/make_assets.py", "scripts/play.py", "scripts/server_check.py", "scripts/sync_versions.py",
}
ALL_FILES = {"scripts/combine_jars.py"}


def modules_of_mc(mc):
    return {m for m in NAMES if m.split("-", 1)[1] == mc}


def modules_for(path):
    """The set of module names a changed path can change the jar or the test result of."""
    path = path.replace("\\", "/")
    while path.startswith("./"):
        path = path[2:]
    m = MODULE_DIR.match(path)
    if m:
        module = f"{m.group(1)}-{m.group(2)}"
        rest = m.group(3)
        if module not in NAMES:
            return set(NAMES)  # a module folder this script does not know: fail safe
        if MODULE_NOISE.match(rest) or rest.startswith("src/dev/"):
            return set()
        return {module}
    m = VERSION_DIR.match(path)
    if m:
        mc, rest = m.group(1), m.group(2)
        if mc not in MCS:
            return set(NAMES)
        if rest.startswith("src/dev/"):
            return set()
        return modules_of_mc(mc)
    if path.startswith("common/"):
        return set(NAMES)
    if path in ALL_FILES:
        return set(NAMES)
    if path in NONE_FILES or path in NONE_SCRIPTS or NONE_TOP.match(path) or NONE_GITHUB.match(path):
        return set()
    return set(NAMES)  # unknown path


def changed_files(before, sha, cwd=None):
    """Paths changed between two commits, or None when git cannot tell (the caller then builds everything)."""
    if not before or before == ZERO:
        before = sha + "^"  # first push of a branch or a manual run: compare with the parent commit
    try:
        out = subprocess.run(["git", "diff", "--name-only", "--no-renames", before, sha], capture_output=True, text=True, cwd=cwd or ROOT)
    except OSError:
        return None
    if out.returncode != 0:
        return None
    return [line.strip() for line in out.stdout.splitlines() if line.strip()]


def module_version(module, root=None):
    try:
        with open(os.path.join(root or ROOT, module, "gradle.properties"), encoding="utf-8") as fh:
            text = fh.read()
    except OSError:
        return None
    m = re.search(r"^mod_version=(\S+)", text, re.M)
    return m.group(1) if m else None


def newest_version(root=None):
    found = {module_version(m, root) for m in NAMES} - {None}
    keyed = [v for v in found if re.match(r"^\d+(\.\d+)*$", v)]
    return sorted(keyed, key=lambda v: [int(x) for x in v.split(".")])[-1] if keyed else ""


def pick_build(files):
    if files is None:
        return list(NAMES)  # cannot tell: build everything
    picked = set()
    for f in files:
        picked |= modules_for(f)
    return [m for m in NAMES if m in picked]


def pick_release(tag, root=None):
    version = (tag or "").lstrip("v")
    if not re.match(r"^\d+\.\d+\.\d+$", version):
        version = newest_version(root)  # a manual run on a branch: release the newest version found
    return [m for m in NAMES if module_version(m, root) == version], version


def outputs(picked):
    matrix = {"include": [{"layer": m} for m in NAMES if m in picked]}
    combine = [mc for mc in MCS if modules_of_mc(mc) <= set(picked)]
    return {
        "matrix": json.dumps(matrix),
        "any": "true" if picked else "false",
        "count": str(len(picked)),
        "test": "true" if TEST_MODULE in picked else "false",
        "combine": " ".join(combine),
    }


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["build", "release"])
    ap.add_argument("--before")
    ap.add_argument("--sha", default="HEAD")
    ap.add_argument("--tag")
    ap.add_argument("--files", nargs="*", default=None, help="changed paths, instead of asking git (for tests)")
    args = ap.parse_args(argv)

    if args.mode == "release":
        picked, version = pick_release(args.tag)
        print("release version:", version or "none")
    else:
        files = args.files if args.files is not None else changed_files(args.before, args.sha)
        picked = pick_build(files)
        if files is None:
            print("could not compute the changed files: building everything")

    out = outputs(picked)
    lines = [f"{k}={v}" for k, v in out.items()]
    print("\n".join(lines))
    print("builds:", ", ".join(picked) or "none")
    target = os.environ.get("GITHUB_OUTPUT")
    if target:
        with open(target, "a", encoding="utf-8") as fh:
            fh.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
