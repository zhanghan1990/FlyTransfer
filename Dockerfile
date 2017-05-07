from openjdk
COPY Yosemite/log4j.properties /root
COPY Yosemite/core/target/scala-2.10/Yosemite-core-assembly-0.2.0-SNAPSHOT.jar /root
COPY Yosemite/examples/target/scala-2.10/Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar  /root
WORKDIR /root
