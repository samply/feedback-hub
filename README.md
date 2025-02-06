# Feedback hub
This is the backend for the feedback hub part of the metadata feedback system. It
provides a backend that allows researchers to associate metadata (e.g. the DOIs of relevant publications) with
samples held at biobanks that have installed the feedback agent.

This component collects the data entered via the feedback-hub-ui and creates a Beam task from
it. The feedback agents (at biobank sites) that are permitted to collect tasks need
to be specified via an environment variable. Once all feedback agents have collected the
task, it will be removed.

## Prerequisites

In the docker-compose.yml file, there are a number of things that need to be generated first, before running it.

One task is to enable the Beam connection. For this, you will need to register with Beam, and that in turn requires a certificate signing request (CSR). Right now, this is done semi-automatically. You need to run the enroll.sh script in ../beam-feedback-central.broker-test.bbmri-test.samply.de. Provide the script with the ID of the Beam proxy, e.g. "feedback-central.broker-test.bbmri-test.samply.de". After registration, copy the pki directory over to this directory.

You need to create the directory "trusted-ca-certs" if not already present.

You need to copy over eric.root.crt.pem from a Bridgehead (it is publicly available in the bridgehead GitHub repo).

Build the feedback hub UI:
``` code
cd ..
git clone https://github.com/samply/feedback-hub-ui.git
cd feedback-hub-ui
docker build -t samply/feedback-hub-ui .
``` 

## Building for production
``` code
mvn clean install
docker build -t samply/feedback-agent .
```

## Running
The ```docker-compose.yml``` file can be used to run the feedback hub. This file will run both the backend and the UI.
``` code
docker compose up -d
```

