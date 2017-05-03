#!/usr/bin/env bash

# Starts the master on the machine this script is executed on.

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin/Yosemite-config.sh"

if [ -f "${YOSEMITE_CONF_DIR}/Yosemite-env.sh" ]; then
  . "${YOSEMITE_CONF_DIR}/Yosemite-env.sh"
fi

if [ "$YOSEMITE_MASTER_PORT" = "" ]; then
  YOSEMITE_MASTER_PORT=1606
fi

if [ "$YOSEMITE_MASTER_IP" = "" ]; then
  YOSEMITE_MASTER_IP=`hostname`
fi

if [ "$YOSEMITE_MASTER_WEBUI_PORT" = "" ]; then
  YOSEMITE_MASTER_WEBUI_PORT=16016
fi

# Set YOSEMITE_PUBLIC_DNS so the master report the correct webUI address to the slaves
if [ "$YOSEMITE_PUBLIC_DNS" = "" ]; then
    # If we appear to be running on EC2, use the public address by default:
    # NOTE: ec2-metadata is installed on Amazon Linux AMI. Check based on that and hostname
    if command -v ec2-metadata > /dev/null || [[ `hostname` == *ec2.internal ]]; then
        export YOSEMITE_PUBLIC_DNS=`wget -q -O - http://instance-data.ec2.internal/latest/meta-data/public-hostname`
    fi
fi

"$bin"/Yosemite-daemon.sh start Yosemite.framework.master.Master --ip $YOSEMITE_MASTER_IP --port $YOSEMITE_MASTER_PORT --webui-port $YOSEMITE_MASTER_WEBUI_PORT -n

# Start a slave agent at the Master
sleep 1
"$bin"/Yosemite-daemon.sh start Yosemite.framework.slave.Slave Yosemite://$YOSEMITE_MASTER_IP:$YOSEMITE_MASTER_PORT -n
