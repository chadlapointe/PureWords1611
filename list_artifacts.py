import urllib.request
import json
import zipfile
import io
import sys

def get_latest_run():
    url = "https://api.github.com/repos/chadlapointe/PureWords1611/actions/runs?branch=main&per_page=1"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        return data["workflow_runs"][0]

run_id = get_latest_run()['id']
url = f"https://api.github.com/repos/chadlapointe/PureWords1611/actions/runs/{run_id}/artifacts"
req = urllib.request.Request(url)
with urllib.request.urlopen(req) as response:
    data = json.loads(response.read().decode())
    for artifact in data.get('artifacts', []):
        print(f"Artifact: {artifact['name']}")
