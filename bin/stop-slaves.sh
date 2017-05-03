#!/usr/bin/env bash

# Starts the master on the machine this script is executed on.

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin/Yosemite-config.sh"

"$bin"/Yosemite-daemons.sh stop Yosemite.framework.slave.Slave