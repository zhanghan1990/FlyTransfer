# Yosemite
Yosemite tries to minimize weight coflow completion time,
codes of Yosemite is based on Varys

## Building Yosemite

```
./sbt/sbt package
```

### SBT
```
libraryDependencies += "net.varys" %% "varys-core" % "0.2.0-SNAPSHOT"
```
###How to use


- Start the master
```
./run varys.framework.master.Master -n
```

- Start the slave
```
 ./run varys.framework.slave.Slave varys://10.0.0.31:1606 -n
```
where varys://10.0.0.31:1606 is the address of slave

- start the client


client will at the example code