#!/usr/bin/env bash

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin/Yosemite-config.sh"

if [ -f "${YOSEMITE_CONF_DIR}/Yosemite-env.sh" ]; then
  . "${YOSEMITE_CONF_DIR}/Yosemite-env.sh"
fi

# Find the port number for the master
if [ "$YOSEMITE_MASTER_PORT" = "" ]; then
  YOSEMITE_MASTER_PORT=1606
fi

if [ "$YOSEMITE_MASTER_IP" = "" ]; then
  YOSEMITE_MASTER_IP=`hostname`
fi

echo "Master IP: $YOSEMITE_MASTER_IP"

# Launch the slaves
exec "$bin/slaves.sh" cd "$YOSEMITE_HOME" \; "$bin/start-slave.sh" Yosemite://$YOSEMITE_MASTER_IP:$YOSEMITE_MASTER_PORT
