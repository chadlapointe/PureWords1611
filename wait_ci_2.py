import urllib.request
import json
import time

def get_latest_run():
    url = "https://api.github.com/repos/chadlapointe/PureWords1611/actions/runs?branch=main&per_page=1"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        return data["workflow_runs"][0]

while True:
    run = get_latest_run()
    if run['id'] != 37844191164:
        status = run["status"]
        if status == "completed":
            print(f"Run {run['id']} completed with conclusion: {run['conclusion']}")
            break
        print(f"Run {run['id']} status: {status}... waiting 10s")
    else:
        print(f"Waiting for new run to start...")
    time.sleep(10)
