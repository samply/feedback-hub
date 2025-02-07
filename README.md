# Feedback hub
This is the backend for the feedback hub part of the metadata feedback system. It
provides a backend that allows researchers to associate metadata (e.g. the DOIs of relevant publications) with
samples held at biobanks that have installed the feedback agent.

This component collects the data entered via the feedback-hub-ui and creates a Beam task from
it. The feedback agents (at biobank sites) that are permitted to collect tasks need
to be specified via an environment variable. Once all feedback agents have collected the
task, it will be removed.

## Building for production
This repository contains a ```docker-compose.yml``` file that can be used to run a fully-operational central feedback hub. It starts containers for the docker hub backend and its associated database, plus the docker hub UI. However, before starting it, a number of prerequisites need to be met.

### Prerequisites

In the docker-compose.yml file, there are a number of things that need to be generated first, before running it.

One task is to enable the Beam connection. For this, you will need to register with Beam, and that in turn requires a certificate signing request (CSR). Right now, this is done semi-automatically. You need to run the enroll.sh script in ../beam-feedback-central.broker-test.bbmri-test.samply.de. Provide the script with the ID of the Beam proxy, e.g. "feedback-central.broker-test.bbmri-test.samply.de". After registration, copy the pki directory over to this directory.

You need to create the directory "trusted-ca-certs" if not already present.

You need to copy over eric.root.crt.pem from a Bridgehead (it is publicly available in the bridgehead GitHub repo).

Build the feedback hub UI (more details [here](https://github.com/samply/feedback-hub-ui)).

### Building
Build the feedback hub backend and start the central feedback hub containers:
``` code
git clone https://github.com/samply/feedback-hub.git
cd feedback-hub
mvn clean install
docker build -t samply/feedback-agent .
docker-compose up -d
```

## Running for production
The ```docker-compose.yml``` file can be used to run the feedback hub. This file will run both the backend and the UI.
``` code
docker compose up -d
```

You will find the UI at http://localhost:8095/ on the production machine.

