import re
import json

path = "app/src/main/assets/study/kjv_strongs.json"
with open(path, "r", encoding="utf-8") as f:
    # Read in chunks if necessary, but 9MB fits in memory
    content = f.read()

# Tags like {H7225} or {(H8804)}
tags = re.findall(r'\{(\(?[HG]\d+\)?)\}', content)
unique_tags = sorted(list(set(tags)))

print(f"Total unique tags: {len(unique_tags)}")
print("Sample tags:")
print(unique_tags[:50])

# Group by H and G
hebrew = [t for t in unique_tags if 'H' in t]
greek = [t for t in unique_tags if 'G' in t]

print(f"Hebrew tags: {len(hebrew)}")
print(f"Greek tags: {len(greek)}")

# Check for morphology (the ones in parentheses)
morph = [t for t in unique_tags if '(' in t]
print(f"Morphology tags: {len(morph)}")
