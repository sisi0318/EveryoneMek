"""Select addon builds from the complete push/PR diff, or an explicit manual choice."""
import json
import os
from pathlib import Path
import re
import subprocess

from prepare_release import MODULES


def modules_for_paths(paths):
    selected = set()
    for path in paths:
        if path.lower().endswith(".md") or path.startswith("docs/"):
            continue
        directory = path.partition("/")[0]
        if directory in MODULES:
            selected.add(directory)
        else:
            # Shared workflows, scripts and root configuration can affect either addon.
            return list(MODULES)
    return [module for module in MODULES if module in selected]


def changed_paths(event, event_name, repository=None):
    if event_name == "pull_request":
        pull = event["pull_request"]
        base, head = pull["base"]["sha"], pull["head"]["sha"]
        separator = "..."
    elif event_name == "push":
        base, head = event.get("before", ""), event.get("after", "")
        separator = ".."
    else:
        return None
    if not all(re.fullmatch(r"[0-9a-f]{40}(?:[0-9a-f]{24})?", sha) and set(sha) != {"0"} for sha in (base, head)):
        return None
    # Disabling rename detection includes both old and new directories for moves.
    # NUL delimiters preserve spaces/newlines in paths; Git's full diff has no API file-list limit.
    result = subprocess.run(
        ["git", "diff", "--name-only", "--no-renames", "-z", f"{base}{separator}{head}", "--"],
        cwd=repository, capture_output=True, check=False,
    )
    if result.returncode:
        return None
    return [path.decode("utf-8", errors="surrogateescape") for path in result.stdout.split(b"\0") if path]


def select_modules(event, event_name, repository=None):
    if event_name == "workflow_dispatch":
        selected = event.get("inputs", {}).get("module", "all")
        if selected == "all":
            return list(MODULES)
        if selected not in MODULES:
            raise ValueError(f"Unsupported module: {selected}")
        return [selected]
    paths = changed_paths(event, event_name, repository)
    if paths is None:
        # New branches and unavailable old revisions must not silently skip a required build.
        print("::notice::No reliable comparison base; selecting all modules.")
        return list(MODULES)
    return modules_for_paths(paths)


def main():
    event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text(encoding="utf-8"))
    selected = select_modules(event, os.environ["GITHUB_EVENT_NAME"])
    matrix = {"mod": [{"directory": MODULES[name]["directory"], "archive": MODULES[name]["archive"]} for name in selected]}
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"matrix={json.dumps(matrix, separators=(',', ':'))}\n")
        output.write(f"has_changes={str(bool(selected)).lower()}\n")
    summary = "Build: " + ", ".join(selected) if selected else "No build needed: documentation-only changes."
    print(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as output:
            output.write(summary + "\n")


if __name__ == "__main__":
    main()
