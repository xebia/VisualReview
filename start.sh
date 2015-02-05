#!/bin/bash

export VISUALREVIEW_PORT=7000
export VISUALREVIEW_DB_URI="file:./.visualreview/vr-h2.db"
export VISUALREVIEW_DB_USER=""
export VISUALREVIEW_DB_PASSWORD=""
export VISUALREVIEW_SCREENSHOTS_DIR="./.visualreview/screenshots"

java -jar target/visualreview-0.0.1-SNAPSHOT-standalone.jar

