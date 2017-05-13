# Yosemite
Yosemite tries to minimize weight coflow completion time.
some codes of Yosemite borrow from Varys

### Building Yosemite

```
git clone https://github.com/zhanghan1990/Yosemite.git
cd Yosemite
sbt assembly
```

After this, you will get 2 files:
- /core/target/scala-2.10/Yosemite-core-assembly-0.2.0-SNAPSHOT.jar
- /examples/target/scala-2.10/Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar

The two files are the package of Yosemite project

### How to deploy (multi-nodes)

##### Start master at the master node
```
java  -cp /youpath/Yosemite-core-assembly-0.2.0-SNAPSHOT.jar Yosemite.framework.master.Master -n
```

Open http://$master-ip:16016 with your browser, you will see the
web address of the master, then copy the address as master-address

#### Start slave at each node (including the master node)

```
java  -cp /yourpath/Yosemite-core-assembly-0.2.0-SNAPSHOT.jar Yosemite.framework.slave.Slave $master-address -n
```
note file log4j.properties should at the same directory with Yosemite-core-assembly-0.2.0-SNAPSHOT.jar and Yosemite-core-assembly-0.2.0-SNAPSHOT.jar

#### Start application at every node
###### Use the broadcast as the example
- Start the sender

```
java  -cp  Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastSender  $master-address 1 "dsdsds" 100000 0  2
```
You will get the broadcast sender address


- Start the client

```
java  -cp  Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastReceiver $master-address  $Broadcastaddress:1608 1
```


### Deploy on docker
Yosemite can run at docker.
##### Generate images with Dockerfile
```
./start-all.sh
```
#### start Data broadcast application
```
python deploy.py
```
#### get avarage coflow completion time and average coflow weight completion time 
```
python analysis.py
```