import re
import os
import time

scheduled_file_path = './logs/scheduled-job-ids.txt'
worker_log_file_path = './logs/worker-logs.txt'

headline = """


     ██╗ ██████╗ ██████╗ ██████╗ ██╗   ██╗███╗   ██╗██████╗     ████████╗███████╗███████╗████████╗██████╗ ███████╗██████╗ 
     ██║██╔═══██╗██╔══██╗██╔══██╗██║   ██║████╗  ██║██╔══██╗    ╚══██╔══╝██╔════╝██╔════╝╚══██╔══╝██╔══██╗██╔════╝██╔══██╗
     ██║██║   ██║██████╔╝██████╔╝██║   ██║██╔██╗ ██║██████╔╝       ██║   █████╗  ███████╗   ██║   ██████╔╝█████╗  ██║  ██║
██   ██║██║   ██║██╔══██╗██╔══██╗██║   ██║██║╚██╗██║██╔══██╗       ██║   ██╔══╝  ╚════██║   ██║   ██╔══██╗██╔══╝  ██║  ██║
╚█████╔╝╚██████╔╝██████╔╝██║  ██║╚██████╔╝██║ ╚████║██║  ██║       ██║   ███████╗███████║   ██║   ██████╔╝███████╗██████╔╝
 ╚════╝  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═╝       ╚═╝   ╚══════╝╚══════╝   ╚═╝   ╚═════╝ ╚══════╝╚═════╝ 
                                                                                                                                                                                                                          
"""

# Compile regex patterns
SCHEDULED_PATTERN = re.compile(r'\[(jobrunr-testbed-[a-z0-9-]+)\]: Recurring job .* resulted in 1 scheduled job')
SUCCEEDED_PATTERN = re.compile(
    r"Job\(id=([a-z0-9-]+), recurringJobId=Optional\[(jobrunr-testbed-[a-z0-9-]+)\], jobName='[^']*'\) processing succeeded"
)
SUCCEEDED_ONETIME_PATTERN = re.compile(
    r"Job\(id=([a-z0-9-]+), recurringJobId=Optional\.empty, jobName='[^']*'\) processing succeeded"
)


# Load scheduled job IDs and times from file
def load_scheduled_job_ids():
    if not os.path.exists(scheduled_file_path):
        return {}

    jobs = {}
    with open(scheduled_file_path, 'r') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            # Try to extract scheduled time
            if 'scheduled for' in line:
                parts = line.split(' scheduled for ')
                job_id = parts[0].strip()
                run_time = parts[1].strip()
                jobs[job_id] = run_time
            else:
                jobs[line] = "N/A"
    return jobs

# Tail worker logs
def tail_worker_logs():
    seen_scheduled = set()
    seen_recurring_succeeded = set()
    seen_onetime_succeeded = set()

    with open(worker_log_file_path, 'r') as log:
        log.seek(0, os.SEEK_END)
        while True:
            line = log.readline()
            if not line:
                time.sleep(0.2)
                continue

            # Recurring job scheduled
            match_scheduled = SCHEDULED_PATTERN.search(line)
            if match_scheduled:
                recurring_id = match_scheduled.group(1)
                seen_scheduled.add(recurring_id)

            # Recurring job succeeded
            match_succeeded = SUCCEEDED_PATTERN.search(line)
            if match_succeeded:
                recurring_id = match_succeeded.group(2)
                seen_recurring_succeeded.add(recurring_id)

            # One-time job succeeded
            match_onetime = SUCCEEDED_ONETIME_PATTERN.search(line)
            if match_onetime:
                job_id = match_onetime.group(1)
                seen_onetime_succeeded.add(job_id)

            show_status(seen_scheduled, seen_recurring_succeeded, seen_onetime_succeeded)

            # Exit once all jobs succeeded
            scheduled_jobs = load_scheduled_job_ids()
            all_done = all(
                (job.startswith("jobrunr-testbed-") and job in seen_recurring_succeeded) or
                (not job.startswith("jobrunr-testbed-") and job in seen_onetime_succeeded)
                for job in scheduled_jobs
            )
            if all_done:
                print("🎉 All jobs have succeeded. Exiting.")
                break

# Display current job status
def show_status(seen_scheduled, recurring_succeeded, onetime_succeeded):
    scheduled_jobs = load_scheduled_job_ids()
    os.system('clear')
    print(headline)
    print("📋 Job Status Dashboard")
    print("========================\n")

    print("Recurring Jobs:")
    for job in sorted(j for j in scheduled_jobs if j.startswith("jobrunr-testbed-")):
        sched = "SCHEDULED (🟢)" if job in seen_scheduled else "SCHEDULED (⚪)"
        succ = "PROCESSING SUCCEEDED (✅)" if job in recurring_succeeded else "PROCESSING SUCCEEDED (❌)"
        print(f"{sched} {succ} {job} → scheduled for {scheduled_jobs[job]}")

    print("\nOne-time Jobs:")
    for job in sorted(j for j in scheduled_jobs if not j.startswith("jobrunr-testbed-")):
        succ = "PROCESSING SUCCEEDED (✅)" if job in onetime_succeeded else "PROCESSING SUCCEEDED (❌)"
        print(f"     {succ} {job} → scheduled for {scheduled_jobs[job]}")

    print(f"\n⏱ Last updated: {time.strftime('%H:%M:%S')}\n")

if __name__ == "__main__":
    tail_worker_logs()