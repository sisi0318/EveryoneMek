"""Resolve an allowed mod and stage only its release JAR, checksum and notes."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil
import tomllib
from zipfile import ZipFile

# Add new projects here and to build.yml/release.yml's module choices.
MODULES = {
    "NaturesAura": {
        "directory": "NaturesAura",
        "name": "Nature's Mekanism",
        "archive": "NaturesMekanism",
        "mod_id": "naturesmekanism",
        "java": "21",
    },
    "Ars-Nouveau": {
        "directory": "Ars-Nouveau",
        "name": "Ars Mekanism",
        "archive": "ArsMekanism",
        "mod_id": "arsmekanism",
        "java": "21",
    },
    "Forbidden-Arcanus": {
        "directory": "Forbidden-Arcanus",
        "name": "Forbidden Mekanism",
        "archive": "ForbiddenMekanism",
        "mod_id": "forbiddenmekanism",
        "java": "21",
    },
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", type=Path)
    args = parser.parse_args()
    selected = os.environ["RELEASE_MODULE"]
    if selected not in MODULES:
        raise SystemExit(f"Unsupported module: {selected}")
    config = MODULES[selected]
    directory = Path(config["directory"])
    properties = {}
    for line in (directory / "gradle.properties").read_text(encoding="utf-8").splitlines():
        key, separator, value = line.partition("=")
        if separator and not key.lstrip().startswith("#"):
            properties[key.strip()] = value.strip()
    version = properties.get("mod_version", "")
    if not re.fullmatch(r"[0-9][0-9A-Za-z._+-]{0,79}", version) or ".." in version or version.endswith("."):
        raise SystemExit("mod_version must be a non-empty, safe version string")
    filename = f"{config['archive']}-{version}.jar"
    metadata = {
        "directory": config["directory"],
        "java": config["java"],
        "version": version,
        "tag": f"{selected}-v{version}",
        "title": f"[{selected}] {config['name']} {version}",
        "filename": filename,
        "artifact": f"release-{selected}-{os.environ['GITHUB_RUN_ID']}-{os.environ['GITHUB_RUN_ATTEMPT']}",
    }
    if args.stage is None:
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            for key, value in metadata.items():
                output.write(f"{key}={value}\n")
        print(f"Selected {selected} {version}: {filename}")
        return

    source = directory / "build" / "libs" / filename
    if not source.is_file():
        raise SystemExit(f"Expected release JAR missing: {source}")
    with ZipFile(source) as archive:
        mod_data = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode("utf-8"))
    if not any(mod.get("modId") == config["mod_id"] and str(mod.get("version")) == version for mod in mod_data.get("mods", [])):
        raise SystemExit("Built JAR does not match the selected mod and version")
    args.stage.mkdir(parents=True, exist_ok=True)
    if any(args.stage.iterdir()):
        raise SystemExit("Release staging directory must be empty")
    shutil.copy2(source, args.stage / filename)
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    (args.stage / "SHA256SUMS").write_text(f"{digest}  {filename}\n", encoding="utf-8")
    sha = os.environ["GITHUB_SHA"]
    repository = os.environ["GITHUB_REPOSITORY"]
    notes = [
        f"# {config['name']} {version}", "",
        f"模组：`{selected}`", "",
        f"源码提交：[{sha[:7]}](https://github.com/{repository}/commit/{sha})", "",
        f"[使用说明](https://github.com/{repository}/blob/{sha}/{config['directory']}/README.md)", "",
    ]
    changelog = directory / "CHANGELOG.md"
    if changelog.is_file():
        lines = changelog.read_text(encoding="utf-8").splitlines()
        heading = f"## {version}"
        if heading in lines:
            notes += ["## 更新内容", ""]
            for line in lines[lines.index(heading) + 1:]:
                if line.startswith("## "):
                    break
                notes.append(line)
    (args.stage / "RELEASE_NOTES.md").write_text("\n".join(notes).rstrip() + "\n", encoding="utf-8")
    print(f"Staged {filename} and SHA256SUMS")


if __name__ == "__main__":
    main()
