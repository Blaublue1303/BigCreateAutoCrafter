#!/usr/bin/env bash
# Analysis helper only (the build uses Maven): downloads the reference JARs (Create 6.0.10, JEI) from
# Modrinth into libs/ for decompiling and verifies checksums.
# libs/ is git-ignored (third-party binaries are never committed).
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p libs
python3 -I - <<'PY'
import json, hashlib, urllib.request, os
def versions(slug):
    u = f'https://api.modrinth.com/v2/project/{slug}/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D'
    return json.load(urllib.request.urlopen(u, timeout=60))
def fetch(slug, pick, sha1=None):
    v = pick(versions(slug)); f = v['files'][0]; path = os.path.join('libs', f['filename'])
    if not os.path.exists(path):
        urllib.request.urlretrieve(f['url'], path)
    got = hashlib.sha1(open(path, 'rb').read()).hexdigest()
    assert got == f['hashes']['sha1'], f'sha1 mismatch for {path}'
    if sha1: assert got == sha1, f'unexpected sha1 for {path}: {got}'
    print('ok', path)
# Create 6.0.10 == CurseForge file 7963363 (size 19123767 bytes, verified)
fetch('create', lambda vs: next(x for x in vs if x['version_number'] == '6.0.10+mc1.21.1'),
      '0e97e49837bed766e6f28a4c95b04885d6acc353')
# JEI: pack's exact build (CurseForge fileID 8857687 in manifest.json, not yet resolved, see docs/01) is still UNVERIFIED; newest build used for dev only.
fetch('jei', lambda vs: vs[0])
PY
