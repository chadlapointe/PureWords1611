import urllib.request
import json
import zipfile
import io

try:
    req = urllib.request.Request("https://api.github.com/repos/chadlapointe/PureWords1611/actions/runs/37820902414/logs")
    with urllib.request.urlopen(req) as response:
        with zipfile.ZipFile(io.BytesIO(response.read())) as z:
            for name in z.namelist():
                if 'Smoke Tests' in name and 'Set up Android SDK' in name:
                    content = z.read(name).decode()
                    lines = content.split('\n')
                    for line in lines[-20:]:
                        print(line.strip())
except urllib.error.HTTPError as e:
    print(f"HTTPError: {e.code} {e.reason}")
