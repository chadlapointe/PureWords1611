import urllib.request
import json

url = "https://api.github.com/repos/chadlapointe/PureWords1611/actions/runs/37844627754/jobs"
req = urllib.request.Request(url)
with urllib.request.urlopen(req) as response:
    data = json.loads(response.read().decode())
    for job in data.get('jobs', []):
        if job['conclusion'] == 'failure':
            check_run_url = job['check_run_url'] + "/annotations"
            try:
                annot_req = urllib.request.Request(check_run_url)
                with urllib.request.urlopen(annot_req) as a_resp:
                    annots = json.loads(a_resp.read().decode())
                    if annots:
                        print(f"Annotations for {job['name']}:")
                        for a in annots:
                            print(f"  {a.get('path', '')}:{a.get('start_line', '')} - {a.get('message', '')}")
            except Exception as e:
                pass
