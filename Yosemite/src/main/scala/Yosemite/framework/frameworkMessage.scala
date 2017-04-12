package Yosemite.framework

/**
  * Created by zhanghan on 17/4/3.
  */


// Base class to transfer between actors
private[Yosemite] sealed trait FrameworkMessage extends Serializable


// Register slave send from slave to master when slave starts
private[Yosemite] case class RegisterSlave(
                                         id: String,
                                         ip: String,
                                         port: Int) extends FrameworkMessage

// Slave register fails sent from master to slave
private[Yosemite] case class RegisterSlaveFailed(message:String) extends FrameworkMessage

// Slave register susscess sent from master to slave
private[Yosemite] case class RegisteredSlave(id:String,url:String) extends FrameworkMessage

// Job register sent from slave to master
private[Yosemite] case class RegisterJob(id:String, jobDescription: JobDescription) extends FrameworkMessage

// Job registered successfully information sent from master to slave
private[Yosemite] case class RegisteredJob(id:String) extends FrameworkMessage

// Job registered failed from master to slave
private[Yosemite] case class RegisterJobFailed(message:String) extends FrameworkMessage

// Job unregister sent from slave to master
private[Yosemite] case class UnregisterJob(id:String) extends FrameworkMessage


// File add sent from slave to master
private[Yosemite] case class AddFile(desc:FileDescription) extends FrameworkMessage

// file get sent from master to slave
private[Yosemite] case class GetFILE(fileId:String,jobId:String) extends FrameworkMessage

// flow description sent from master to the slave nodes
private[Yosemite] case class GetFlowDesc(flowDesc: Array[FlowDescription]) extends FrameworkMessage