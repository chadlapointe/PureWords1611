import urllib.request
import json

def get_latest_run():
    url = "https://api.github.com/repos/chadlapointe/PureWords1611/actions/runs?branch=main&per_page=1"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        return data["workflow_runs"][0]

run_id = get_latest_run()['id']
url = f"https://api.github.com/repos/chadlapointe/PureWords1611/actions/runs/{run_id}/jobs"
req = urllib.request.Request(url)
with urllib.request.urlopen(req) as response:
    data = json.loads(response.read().decode())
    for job in data.get('jobs', []):
        print(f"Job {job['name']}: {job['conclusion']}")
        if job['conclusion'] == 'failure':
            steps = job.get('steps', [])
            for step in steps:
                if step['conclusion'] == 'failure':
                    print(f"  Failed Step: {step['name']}")
