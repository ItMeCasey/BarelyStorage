#!/usr/bin/env python3
"""Tests for plan_matrix.py. Run from the repo root: python -I -m unittest discover -s .github/scripts
or: python -I .github/scripts/test_plan_matrix.py

Synthetic file lists always run; the cases built from real commits are skipped when the commit is not in the clone.
"""
import json
import os
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import plan_matrix as pm  # noqa: E402

ALL = set(pm.NAMES)
MC262 = {"forge-26.2", "neoforge-26.2"}
MC263 = {"forge-26.3", "neoforge-26.3"}
NEOFORGE = {"neoforge-26.1.2", "neoforge-26.2", "neoforge-26.3"}


def git_files(*args):
    """Paths of a real commit range, or None when git or the commit is not available."""
    try:
        out = subprocess.run(["git", "diff", "--name-only", "--no-renames", *args], capture_output=True, text=True, cwd=pm.ROOT)
    except OSError:
        return None
    return out.stdout.split() if out.returncode == 0 else None


class SyntheticBuild(unittest.TestCase):
    def pick(self, *files):
        return set(pm.pick_build(list(files)))

    def test_docs_and_status_pick_nothing(self):
        self.assertEqual(self.pick("PROJECT_STATUS.md", "README.md", "CHANGELOG.md", "PLAN.md", "art/server_rack.json",
                                   "branding/banners/banner.png", "curseforge/DESCRIPTION.md", "licenses/OFL-silkscreen.txt",
                                   "LICENSE", ".gitignore", "dist/x.jar", "run/options.txt"), set())

    def test_workflow_and_ci_script_changes_pick_nothing(self):
        self.assertEqual(self.pick(".github/workflows/build.yml", ".github/workflows/release.yml",
                                   ".github/scripts/plan_matrix.py", ".github/scripts/publish_curseforge.py",
                                   ".github/ISSUE_TEMPLATE/config.yml"), set())

    def test_common_picks_everything(self):
        self.assertEqual(self.pick("common/src/main/java/com/itmecasey/barelystorage/A.java"), ALL)
        self.assertEqual(self.pick("common/src/main/resources/assets/barelystorage/lang/en_us.json"), ALL)
        self.assertEqual(self.pick("common/src/test/java/com/itmecasey/barelystorage/ATest.java"), ALL)

    def test_version_folder_picks_both_loaders_of_that_version_only(self):
        self.assertEqual(self.pick("version-26.2/src/main/java/x/A.java"), MC262)
        self.assertEqual(self.pick("version-26.3/src/main/resources/a.json"), MC263)

    def test_dev_tools_are_not_built_by_ci(self):
        self.assertEqual(self.pick("version-26.2/src/dev/java/x/ShotTour.java", "neoforge-26.3/src/dev/java/x/ReiProbe.java"), set())

    def test_single_module_change_picks_exactly_that_module(self):
        self.assertEqual(self.pick("neoforge-26.3/src/main/java/x/StorageJeiPlugin.java"), {"neoforge-26.3"})
        self.assertEqual(self.pick("forge-26.1.2/gradle.properties"), {"forge-26.1.2"})
        self.assertEqual(self.pick("forge-26.2/libs/bei-forge-26.2-2026.11.7-api.jar", "forge-26.2/build.gradle"), {"forge-26.2"})

    def test_module_build_and_run_folders_pick_nothing(self):
        self.assertEqual(self.pick("forge-26.2/build/libs/x.jar", "neoforge-26.2/run/options.txt", "neoforge-26.2/runs/a"), set())

    def test_combine_script_picks_everything_and_dev_scripts_nothing(self):
        self.assertEqual(self.pick("scripts/combine_jars.py"), ALL)
        self.assertEqual(self.pick("scripts/client_tour.py", "scripts/make_assets.py", "scripts/server_check.py"), set())
        self.assertEqual(self.pick("scripts/brand_new_tool.py"), ALL)

    def test_unknown_path_picks_everything(self):
        self.assertEqual(self.pick("something/new.txt"), ALL)
        self.assertEqual(self.pick("build.gradle"), ALL)
        self.assertEqual(self.pick(".gitattributes"), ALL)
        self.assertEqual(self.pick("forge-9.9/build.gradle"), ALL)
        self.assertEqual(self.pick("version-9.9/src/main/java/A.java"), ALL)

    def test_mixed_change_is_the_union(self):
        self.assertEqual(self.pick("PROJECT_STATUS.md", "forge-26.2/gradle.properties", "neoforge-26.3/gradle.properties"), {"forge-26.2", "neoforge-26.3"})

    def test_backslash_and_dot_slash_paths(self):
        self.assertEqual(self.pick("forge-26.2\\src\\main\\java\\A.java"), {"forge-26.2"})
        self.assertEqual(self.pick("./.github/workflows/build.yml"), set())

    def test_diff_failure_picks_everything(self):
        self.assertEqual(set(pm.pick_build(None)), ALL)
        self.assertIsNone(pm.changed_files("0123456789abcdef0123456789abcdef01234567", "fedcba9876543210fedcba9876543210fedcba98"))

    def test_outputs(self):
        out = pm.outputs(["neoforge-26.2", "forge-26.2", "forge-26.3"])
        self.assertEqual((out["any"], out["count"], out["test"], out["combine"]), ("true", "3", "true", "26.2"))
        self.assertEqual(json.loads(out["matrix"]), {"include": [{"layer": "neoforge-26.2"}, {"layer": "forge-26.2"}, {"layer": "forge-26.3"}]})
        nothing = pm.outputs([])
        self.assertEqual((nothing["any"], nothing["test"], nothing["count"], nothing["combine"], nothing["matrix"]),
                         ("false", "false", "0", "", '{"include": []}'))
        self.assertEqual(pm.outputs(pm.NAMES)["combine"], "26.1.2 26.2 26.3")
        self.assertEqual(pm.outputs(["forge-26.3"])["test"], "false")


class RealCommits(unittest.TestCase):
    def check(self, a, b, expected):
        files = git_files(a, b)
        if files is None:
            self.skipTest(f"commit {a} or {b} is not in this clone")
        self.assertEqual(set(pm.pick_build(files)), expected)

    def test_status_only_commit(self):
        self.check("6ead22a^", "6ead22a", set())

    def test_docs_only_commit(self):
        self.check("6a8a105^", "6a8a105", set())

    def test_status_and_dev_tour_commit(self):
        self.check("18ac5a4^", "18ac5a4", set())

    def test_dev_probes_and_docs_commit(self):
        self.check("6552996^", "6552996", set())

    def test_neoforge_only_commit(self):
        self.check("3f948d0^", "3f948d0", NEOFORGE)

    def test_commit_touching_every_module(self):
        self.check("242e6e0^", "242e6e0", ALL)


class Release(unittest.TestCase):
    def make_root(self, versions):
        root = tempfile.mkdtemp()
        for module in pm.NAMES:
            os.makedirs(os.path.join(root, module))
            with open(os.path.join(root, module, "gradle.properties"), "w", encoding="utf-8") as fh:
                fh.write(f"mod_id=barelystorage\nmod_version={versions.get(module, '2026.11.3')}\n")
        return root

    def test_only_modules_at_the_tag_version(self):
        root = self.make_root({m: "2026.11.4" for m in MC263})
        picked, version = pm.pick_release("v2026.11.4", root)
        self.assertEqual(version, "2026.11.4")
        self.assertEqual(set(picked), MC263)
        self.assertEqual(set(pm.pick_release("v2026.11.3", root)[0]), ALL - MC263)

    def test_all_modules_at_the_tag_version(self):
        root = self.make_root({m: "2026.11.4" for m in pm.NAMES})
        self.assertEqual(set(pm.pick_release("v2026.11.4", root)[0]), ALL)

    def test_tag_nobody_carries_picks_nothing(self):
        self.assertEqual(pm.pick_release("v2026.11.9", self.make_root({}))[0], [])

    def test_manual_run_uses_the_newest_version(self):
        root = self.make_root({m: "2026.11.10" for m in MC262})
        picked, version = pm.pick_release("main", root)
        self.assertEqual((version, set(picked)), ("2026.11.10", MC262))

    def test_real_repo_versions(self):
        version = pm.newest_version()
        if not version:
            self.skipTest("no mod_version found")
        picked, _ = pm.pick_release("v" + version)
        self.assertTrue(picked)
        for module in picked:
            self.assertEqual(pm.module_version(module), version)


if __name__ == "__main__":
    unittest.main()
