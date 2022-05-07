#!/usr/bin/python
import sys
import os
import json
import platform
import subprocess

# https://stackoverflow.com/a/49073644/8081973
# this version fixes works correctly on windows where the ping command returns 0 even for Destination host unreachable.


def ping(ipAddr, timeout=100):
    if platform.system().lower() == 'windows':
        numFlag = '-n'
    else:
        numFlag = '-c'
    completedPing = subprocess.run(['ping', numFlag, '1', '-w', str(timeout), ipAddr],
                                   stdout=subprocess.PIPE,    # Capture standard out
                                   stderr=subprocess.STDOUT)  # Capture standard error
    #print(completedPing.stdout)
    return (completedPing.returncode == 0) and (b'TTL=' in completedPing.stdout or b'ttl=' in completedPing.stdout)


# every agent script has 2 parameters - input file and output folder
job_description = sys.argv[1]
output_folder = sys.argv[2]


description = None
with open(job_description, 'r') as job_description:
    description = json.load(job_description)

address_to_ping = description["parameters"]["address"]
ping_result = ping(address_to_ping)

with open(os.path.join(output_folder, 'result.json'), 'w') as output_file:
    json.dump({address_to_ping: ping_result}, output_file)
