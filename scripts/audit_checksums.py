import json
import glob
import re
import sys

hexre = re.compile(r'^[0-9a-f]{64}$')
total = 0
bad = 0

for p in sorted(glob.glob('app/src/main/assets/study/**/*.json', recursive=True)):
    try:
        with open(p, 'r', encoding='utf-8-sig') as f:
            d = json.load(f)
    except Exception as e:
        print(f'SKIP {p}: {e}')
        continue
    
    if isinstance(d, dict) and len(d.keys()) == 1 and isinstance(list(d.values())[0], list):
        es = list(d.values())[0]
    else:
        es = d if isinstance(d, list) else [d]
        
    for e in es:
        if isinstance(e, dict) and 'checksum_sha256' in e:
            total += 1
            if not hexre.match(str(e['checksum_sha256'])):
                bad += 1
                print(f'BAD {p}: {e["checksum_sha256"]!r}')

print(f'TOTAL_CHECKSUMS={total} BAD={bad}')
if bad > 0:
    sys.exit(1)
