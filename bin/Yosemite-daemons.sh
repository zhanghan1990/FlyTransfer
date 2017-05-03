#!/usr/bin/env bash

# Run a Varys command on all slave hosts.

usage="Usage: Yosemite-daemons.sh [--config confdir] [--hosts hostlistfile] [start|stop] command args..."

# if no args specified, show usage
if [ $# -le 1 ]; then
  echo $usage
  exit 1
fi

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin/Yosemite-config.sh"

exec "$bin/slaves.sh" cd "$YOSEMITE_HOME" \; "$bin/Yosemite-daemon.sh" "$@"
