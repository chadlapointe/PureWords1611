import urllib.request
import json

url = "https://api.github.com/repos/chadlapointe/PureWords1611/actions/runs/37841835980/jobs"
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
