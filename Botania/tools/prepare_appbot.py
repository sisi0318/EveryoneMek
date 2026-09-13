"""Reproducible API-name relocation for the official Appbot alpha.3 JAR.

Relocate renamed API classes, sound fields and one recipe ingredient. Instructions and textures remain original.
The upstream licenses remain in the archive. See appbot-compat.json for provenance.
"""
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
LOCK = json.loads((ROOT / 'appbot-compat.json').read_text(encoding='utf-8'))
DEST = ROOT / 'build/dependencies'


def relocate(data):
    out = bytearray(data[:10]); pos = 10; index = 1
    count = struct.unpack_from('>H', data, 8)[0]
    while index < count:
        tag = data[pos]; pos += 1; out.append(tag)
        if tag == 1:
            size = struct.unpack_from('>H', data, pos)[0]; pos += 2
            value = data[pos:pos + size]; pos += size
            for old, new in LOCK['remappings'].items(): value = value.replace(old.encode(), new.encode())
            for old, new in LOCK['member_remappings'].items():
                if value == old.encode(): value = new.encode()
            out.extend(struct.pack('>H', len(value))); out.extend(value)
        else:
            size = {3:4, 4:4, 5:8, 6:8, 7:2, 8:2, 9:4, 10:4, 11:4, 12:4, 15:3, 16:2, 17:4, 18:4, 19:2, 20:2}[tag]
            out.extend(data[pos:pos + size]); pos += size
            if tag in (5, 6): index += 1
        index += 1
    out.extend(data[pos:]); return bytes(out)


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    original = DEST / LOCK['filename']; output = DEST / LOCK['output']
    assert original.parent.resolve() == DEST.resolve() and output.parent.resolve() == DEST.resolve()
    if not original.exists():
        subprocess.run(['gh', 'release', 'download', LOCK['version'], '--repo', LOCK['repository'], '--pattern', LOCK['filename'], '--dir', str(DEST)], check=True)
    if hashlib.sha256(original.read_bytes()).hexdigest() != LOCK['sha256']:
        raise SystemExit('Applied Botanics upstream JAR checksum mismatch')
    with zipfile.ZipFile(original) as source, zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as target:
        for entry in source.infolist():
            data = source.read(entry.filename)
            if entry.filename.endswith('.class'): data = relocate(data)
            elif entry.filename.endswith('.json'):
                for old, new in LOCK['resource_remappings'].items(): data = data.replace(old.encode(), new.encode())
            elif entry.filename == 'META-INF/neoforge.mods.toml':
                data = data.replace(b'version = "1.6.0-alpha.3"', b'version = "1.6.0-alpha.3-botania456"')
            target.writestr(entry, data)
        provenance = zipfile.ZipInfo('META-INF/botanicalmekanism-api-relocations.json', (1980, 1, 1, 0, 0, 0))
        provenance.compress_type = zipfile.ZIP_DEFLATED
        target.writestr(provenance, json.dumps(LOCK, indent=2))
        for name in ('LICENSE', 'NOTICE.md'):
            entry = zipfile.ZipInfo('META-INF/appbot-' + name, (1980, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            target.writestr(entry, (ROOT / 'third-party/appbot' / name).read_bytes())
    print('Prepared', output.name, hashlib.sha256(output.read_bytes()).hexdigest())


if __name__ == '__main__': main()
