#!/usr/bin/env bash

# Start all Yosemite daemons.
# Starts the master on this node.
# Starts a slave on each node specified in conf/slaves

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

# Load the Varys configuration
. "$bin/Yosemite-config.sh"

# Start Master
"$bin"/start-master.sh

# Start Slaves
"$bin"/start-slaves.sh
