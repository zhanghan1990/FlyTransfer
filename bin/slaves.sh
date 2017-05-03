#!/usr/bin/env bash

# This YOSEMITE framework script is a modified version of the Apache Hadoop framework
# script, available under the Apache 2 license:
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Run a shell command on all slave hosts.
#
# Environment Variables
#
#   YOSEMITE_SLAVES    File naming remote hosts.
#     Default is ${YOSEMITE_CONF_DIR}/slaves.
#   YOSEMITE_CONF_DIR  Alternate conf dir. Default is ${YOSEMITE_HOME}/conf.
#   YOSEMITE_SLAVE_SLEEP Seconds to sleep between spawning remote commands.
#   YOSEMITE_SSH_OPTS Options passed to ssh when running remote commands.
##

usage="Usage: slaves.sh [--config confdir] command..."

# if no args specified, show usage
if [ $# -le 0 ]; then
  echo $usage
  exit 1
fi

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin/Yosemite-config.sh"

# If the slaves file is specified in the command line,
# then it takes precedence over the definition in 
# Yosemite-env.sh. Save it here.
HOSTLIST=$YOSEMITE_SLAVES

if [ -f "${YOSEMITE_CONF_DIR}/Yosemite-env.sh" ]; then
  . "${YOSEMITE_CONF_DIR}/Yosemite-env.sh"
fi

if [ "$HOSTLIST" = "" ]; then
  if [ "$YOSEMITE_SLAVES" = "" ]; then
    export HOSTLIST="${YOSEMITE_CONF_DIR}/slaves"
  else
    export HOSTLIST="${YOSEMITE_SLAVES}"
  fi
fi

echo $"${@// /\\ }"

# By default disable strict host key checking
if [ "$YOSEMITE_SSH_OPTS" = "" ]; then
  YOSEMITE_SSH_OPTS="-o StrictHostKeyChecking=no"
fi

for slave in `cat "$HOSTLIST"|sed  "s/#.*$//;/^$/d"`; do
 ssh $YOSEMITE_SSH_OPTS $slave $"${@// /\\ }" \
   2>&1 | sed "s/^/$slave: /" &
 if [ "$YOSEMITE_SLAVE_SLEEP" != "" ]; then
   sleep $YOSEMITE_SLAVE_SLEEP
 fi
done

wait
