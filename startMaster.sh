#!/bin/bash

SCALA_VERSION=2.10

# Figure out where it is installed
FWDIR="$(cd `dirname $0`; pwd)"

# Export this as VARYS_HOME
export VARYS_HOME="$FWDIR"

VARYS_MASTER_IP=172.17.0.10

# Set JAVA_OPTS to be able to load native libraries and to set heap size
JAVA_OPTS="$VARYS_JAVA_OPTS"
JAVA_OPTS+=" -Djava.library.path=$VARYS_LIBRARY_PATH"
JAVA_OPTS+=" -Xms$VARYS_MEM -Xmx$VARYS_MEM"
# Load extra JAVA_OPTS from conf/java-opts, if it exists
if [ -e $FWDIR/conf/java-opts ] ; then
  JAVA_OPTS+=" `cat $FWDIR/conf/java-opts`"
fi
export JAVA_OPTS

CORE_DIR="$FWDIR/core"
EXAMPLES_DIR="$FWDIR/examples"

# Build up classpath
CLASSPATH="$VARYS_CLASSPATH"
CLASSPATH+=":$FWDIR/conf"
CLASSPATH+=":$CORE_DIR/target/scala-$SCALA_VERSION/classes"
CLASSPATH+=":$CORE_DIR/src/main/resources"
CLASSPATH+=":$EXAMPLES_DIR/target/scala-$SCALA_VERSION/classes"
CLASSPATH+=":$FWDIR/lib_managed/jars/*"
CLASSPATH+=":$FWDIR/lib_managed/bundles/*"

if [ -e "$FWDIR/lib_managed" ]; then
  CLASSPATH+=":$FWDIR/lib_managed/jars/*"
  CLASSPATH+=":$FWDIR/lib_managed/bundles/*"
fi
export CLASSPATH


CLASSPATH+=":$SCALA_LIBRARY_PATH/scala-library.jar"
CLASSPATH+=":$SCALA_LIBRARY_PATH/scala-compiler.jar"
CLASSPATH+=":$SCALA_LIBRARY_PATH/jline.jar"
  # The JVM doesn't read JAVA_OPTS by default so we need to pass it in
EXTRA_ARGS="$JAVA_OPTS"

java -cp "$CLASSPATH" varys.framework.master.Master -n
