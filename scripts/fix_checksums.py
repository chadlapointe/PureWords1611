import json
import glob
import hashlib
import os

def fix_checksums():
    study_dir = os.path.join("app", "src", "main", "assets", "study")
    json_files = sorted(glob.glob(os.path.join(study_dir, "*.json")))

    for path in json_files:
        try:
            with open(path, "r", encoding="utf-8-sig") as f:
                data = json.load(f)
        except Exception as e:
            print(f"{path}: Failed to load JSON ({e})")
            continue

        modified_count = 0
        total_checksum_entries = 0

        if isinstance(data, list):
            for entry in data:
                if isinstance(entry, dict) and "checksum_sha256" in entry:
                    total_checksum_entries += 1
                    old_hash = entry["checksum_sha256"]
                    content = {k: v for k, v in entry.items() if k != "checksum_sha256"}
                    canonical_json = json.dumps(content, sort_keys=True, ensure_ascii=False)
                    new_hash = hashlib.sha256(canonical_json.encode("utf-8")).hexdigest().lower()
                    if old_hash != new_hash:
                        entry["checksum_sha256"] = new_hash
                        modified_count += 1
        elif isinstance(data, dict):
            if "checksum_sha256" in data:
                total_checksum_entries += 1
                old_hash = data["checksum_sha256"]
                content = {k: v for k, v in data.items() if k != "checksum_sha256"}
                canonical_json = json.dumps(content, sort_keys=True, ensure_ascii=False)
                new_hash = hashlib.sha256(canonical_json.encode("utf-8")).hexdigest().lower()
                if old_hash != new_hash:
                    data["checksum_sha256"] = new_hash
                    modified_count += 1

        print(f"{path}: {modified_count} out of {total_checksum_entries} entries updated")

        if modified_count > 0:
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
                f.write("\n")

if __name__ == "__main__":
    fix_checksums()
