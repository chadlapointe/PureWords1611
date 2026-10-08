import urllib.request
import json
import zipfile
import io

# We need the log URL for job 37837914763
try:
    # Get jobs for the run
    url = "https://api.github.com/repos/chadlapointe/PureWords1611/actions/runs/37837914763/jobs"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req) as response:
        jobs_data = json.loads(response.read().decode())
        
    for job in jobs_data.get('jobs', []):
        if job['name'] == 'Smoke Tests':
            job_id = job['id']
            # Fetch log directly
            log_url = f"https://api.github.com/repos/chadlapointe/PureWords1611/actions/jobs/{job_id}/logs"
            # Without token, this might 403 or redirect
            log_req = urllib.request.Request(log_url)
            try:
                with urllib.request.urlopen(log_req) as log_response:
                    content = log_response.read().decode()
                    print(content[-2000:])
            except urllib.error.HTTPError as e:
                print(f"HTTP Error fetching log for job {job_id}: {e.code} {e.reason}")
except Exception as e:
    print(f"Error: {e}")
