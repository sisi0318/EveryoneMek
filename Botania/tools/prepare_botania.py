"""Obtain the exact upstream CI artifact; never accept a floating snapshot silently."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
LOCK = json.loads((ROOT / 'upstream-lock.json').read_text(encoding='utf-8-sig'))
CACHE = ROOT / 'build' / 'dependencies'
DESTINATION = CACHE / LOCK['filename']


def matches(path):
    if not path.is_file():
        return False
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest() == LOCK['sha256']


def main():
    CACHE.mkdir(parents=True, exist_ok=True)
    if matches(DESTINATION):
        print(f"Verified Botania {LOCK['commit'][:7]}")
        return
    supplied = os.environ.get('BOTANIA_JAR')
    if supplied:
        source = Path(supplied).resolve()
        if not matches(source):
            raise SystemExit('BOTANIA_JAR does not match upstream-lock.json')
        shutil.copy2(source, DESTINATION)
    else:
        if not shutil.which('gh'):
            raise SystemExit('Install GitHub CLI and authenticate, or set BOTANIA_JAR to the locked upstream JAR.')
        with tempfile.TemporaryDirectory(prefix='upstream-', dir=CACHE) as directory:
            temporary = Path(directory).resolve()
            if not temporary.is_relative_to(CACHE.resolve()):
                raise SystemExit('Temporary dependency directory escaped the project cache')
            subprocess.run(['gh', 'run', 'download', str(LOCK['run_id']), '--repo', LOCK['repository'],
                            '--name', LOCK['artifact_name'], '--dir', str(temporary)],
                           check=True, timeout=240)
            source = temporary / LOCK['filename']
            if not matches(source):
                raise SystemExit('Downloaded Botania checksum differs from upstream-lock.json')
            shutil.copy2(source, DESTINATION)
    print(f"Prepared and verified {DESTINATION.name}")


if __name__ == '__main__':
    main()
