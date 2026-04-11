#!/bin/bash

# Configuration variables
SERVER_ALIAS="server-aws"
SERVER_USER="ubuntu"
REMOTE_DIR="/home/${SERVER_USER}/job_scrapper"
JAR_FILE="JobHunterBotUA-1.0-shaded.jar"
REMOTE_JAR="app.jar"
SYSTEMD_SERVICE="job_scrapper"
SLEEP_TIME=10

# Deployment options (set to 0 to disable)
INCLUDE_CONFIG_FILE=0
INCLUDE_PROCESSING_FILES=0
CREATE_SERVICE=0

# Files to deploy
PROPERTIES_FILE="application.properties"
EXCLUSION_FILE="exclusion.txt"
PROCESSED_FILE="processed.txt"

# Build the project
echo "Building project..."
mvn clean install

# Deploy JAR file
echo "Deploying JAR file..."
scp ./target/${JAR_FILE} ${SERVER_ALIAS}:${REMOTE_DIR}/${REMOTE_JAR}

# Deploy configuration files
if [ "${INCLUDE_CONFIG_FILE}" -ne 0 ]; then
    echo "Deploying configuration files..."
    scp ./${PROPERTIES_FILE} ${SERVER_ALIAS}:${REMOTE_DIR}/${PROPERTIES_FILE}
fi

# Deploy processing files if enabled
if [ "${INCLUDE_PROCESSING_FILES}" -ne 0 ]; then
    echo "Deploying processing files..."
    scp ./${EXCLUSION_FILE} ${SERVER_ALIAS}:${REMOTE_DIR}/${EXCLUSION_FILE}
    scp ./${PROCESSED_FILE} ${SERVER_ALIAS}:${REMOTE_DIR}/${PROCESSED_FILE}
fi

# Create systemd service if enabled
if [ "${CREATE_SERVICE}" -ne 0 ]; then
    echo "Creating remote directory..."
    ssh -t ${SERVER_ALIAS} "mkdir -p ${REMOTE_DIR}"
    echo "Creating systemd service..."
    ssh -t ${SERVER_ALIAS} "sudo tee /etc/systemd/system/${SYSTEMD_SERVICE}.service > /dev/null << EOF
[Unit]
Description=Job Scraper Service
After=network.target

[Service]
Type=simple
User=${SERVER_USER}
WorkingDirectory=${REMOTE_DIR}
ExecStart=/usr/bin/java -jar ${REMOTE_JAR}
Restart=always
RestartSec=10
SyslogIdentifier=${SYSTEMD_SERVICE}

[Install]
WantedBy=multi-user.target
EOF
    "
    ssh -t ${SERVER_ALIAS} "sudo systemctl daemon-reload"
fi

# Restart service and check status
echo "Restarting service..."
ssh -t ${SERVER_ALIAS} "sudo systemctl restart ${SYSTEMD_SERVICE} && sleep ${SLEEP_TIME} && sudo systemctl status ${SYSTEMD_SERVICE}"