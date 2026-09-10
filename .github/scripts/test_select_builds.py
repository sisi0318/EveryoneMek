from pathlib import Path
import subprocess
import tempfile
import unittest

from prepare_release import MODULES
from select_builds import changed_paths, modules_for_paths, select_modules


class BuildSelectionTest(unittest.TestCase):
    def test_module_paths_shared_changes_and_docs(self):
        cases = [
            (["Ars-Nouveau/src/main/java/Machine.java", "NaturesAura/README.md"], ["Ars-Nouveau"]),
            (["NaturesAura/src/main/resources/data/deleted.json"], ["NaturesAura"]),
            (["Ars-Nouveau/old.java", "NaturesAura/new.java"], ["NaturesAura", "Ars-Nouveau"]),
            (["Forbidden-Arcanus/src/main/java/Controller.java"], ["Forbidden-Arcanus"]),
            (["Ars-Nouveau/old.java", "Forbidden-Arcanus/new.java"], ["Ars-Nouveau", "Forbidden-Arcanus"]),
            (["README.md", "Ars-Nouveau/AGENTS.md", "docs/diagram.svg"], []),
            ([".github/workflows/build.yml"], list(MODULES)),
            ([".gitattributes"], list(MODULES)),
        ]
        for paths, expected in cases:
            with self.subTest(paths=paths):
                self.assertEqual(modules_for_paths(paths), expected)

    def test_manual_selection_and_unknown_comparison(self):
        for module in MODULES:
            self.assertEqual(select_modules({"inputs": {"module": module}}, "workflow_dispatch"), [module])
        self.assertEqual(select_modules({}, "workflow_dispatch"), list(MODULES))
        with self.assertRaises(ValueError):
            select_modules({"inputs": {"module": "unknown"}}, "workflow_dispatch")
        self.assertEqual(select_modules({"before": "0" * 40, "after": "a" * 40}, "push"), list(MODULES))


class GitDiffSelectionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="everyonemek-ci-")
        self.repo = Path(self.temp.name).resolve()
        # Cleanup is restricted to the explicitly created test directory.
        self.assertEqual(self.repo.parent, Path(tempfile.gettempdir()).resolve())
        self.addCleanup(self.temp.cleanup)
        self.git("init", "-q")
        self.write("README.md", "initial\n")
        self.initial = self.commit()

    def git(self, *args):
        result = subprocess.run(
            ["git", "-c", "user.name=CI test", "-c", "user.email=ci@example.invalid",
             "-c", "commit.gpgsign=false", "-c", f"core.hooksPath={self.repo / 'empty-hooks'}", *args],
            cwd=self.repo, capture_output=True, text=True,
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        return result.stdout.strip()

    def write(self, path, text):
        destination = self.repo / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(text, encoding="utf-8")

    def commit(self):
        self.git("add", "--all")
        self.git("commit", "-qm", "test change")
        return self.git("rev-parse", "HEAD")

    def test_push_includes_all_commits_and_cross_module_rename(self):
        self.write("NaturesAura/space name.java", "machine\n")
        first = self.commit()
        self.write("Ars-Nouveau/other.java", "another machine\n")
        second = self.commit()
        event = {"before": self.initial, "after": second}
        self.assertEqual(select_modules(event, "push", self.repo), ["NaturesAura", "Ars-Nouveau"])
        (self.repo / "NaturesAura/space name.java").rename(self.repo / "Ars-Nouveau/moved.java")
        moved = self.commit()
        paths = changed_paths({"before": second, "after": moved}, "push", self.repo)
        self.assertIn("NaturesAura/space name.java", paths)
        self.assertIn("Ars-Nouveau/moved.java", paths)
        self.assertEqual(modules_for_paths(paths), ["NaturesAura", "Ars-Nouveau"])
        self.assertEqual(select_modules({"before": first, "after": second}, "push", self.repo), ["Ars-Nouveau"])

    def test_pull_request_excludes_changes_only_on_base_branch(self):
        self.git("checkout", "-qb", "topic")
        self.write("Ars-Nouveau/feature.java", "feature\n")
        head = self.commit()
        self.git("checkout", "-qb", "base", self.initial)
        self.write("NaturesAura/base-only.java", "unrelated main change\n")
        base = self.commit()
        event = {"pull_request": {"base": {"sha": base}, "head": {"sha": head}}}
        self.assertEqual(select_modules(event, "pull_request", self.repo), ["Ars-Nouveau"])

    def test_unavailable_old_revision_falls_back_to_all(self):
        event = {"before": "f" * 40, "after": self.initial}
        self.assertEqual(select_modules(event, "push", self.repo), list(MODULES))


if __name__ == "__main__":
    unittest.main()
