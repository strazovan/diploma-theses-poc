FROM agent-sidecar
RUN apt-get update && apt-get install -y software-properties-common && apt-get install -y python3-pip && apt-get install iputils-ping
RUN mkdir -p /scripts
RUN mkdir -p /jobs
RUN mkdir -p /jobs/descriptions
RUN mkdir -p /jobs/results
COPY ping.py /scripts