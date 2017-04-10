/**
  * Created by zhanghan on 17/4/2.
  */
package Yosemite.framework.master

import java.util.concurrent.ConcurrentHashMap

import Yosemite.framework.slave.{SlaveInfo, startSlave}
import Yosemite.{Logging, YosemiteException}
import Yosemite.framework._
import Yosemite.utils.AkkaUtils
import akka.actor.{Actor, ActorRef, ActorSystem, Address, Props}
import akka.remote.RemotingLifecycleEvent
import akka.routing.RoundRobinRouter
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable.ArrayBuffer


// define the communication system of the master
private[Yosemite] class Master(systemName:String,actorName:String, hostip:String,
                              port:Int) extends Logging{

  // number of master in our system
  val NUM_MASTER_INSTANCES = System.getProperty("varys.master.numInstances", "1").toInt

  var SlaveId = new ConcurrentHashMap[String,SlaveInfo]

  def now() =System.currentTimeMillis()

  var nextJobNumber = new AtomicInteger()
  val idToJob = new ConcurrentHashMap[String, JobInfo]()
  val completedJobInfo = new ArrayBuffer[JobInfo]



  var nextSlaveNumber = new AtomicInteger()
  val idToSlave = new ConcurrentHashMap[String, SlaveInfo]

  val actorToSlave = new ConcurrentHashMap[ActorRef, SlaveInfo]
  val addressToSlave = new ConcurrentHashMap[Address, SlaveInfo]
  val hostToSlave = new ConcurrentHashMap[String, SlaveInfo]



  /**
    * Generate a new Job ID given a Job's submission date
    */
  def newJobId(submitDate: Date): String = {
    "Job-%06d".format(nextJobNumber.getAndIncrement())
  }



  def start():(ActorSystem,Int) ={
    val (actorSystem, boundPort) = AkkaUtils.createActorSystem(systemName, hostip, port)

    val actor = actorSystem.actorOf(
      Props(new MasterActor(hostip, boundPort)).withRouter(
        RoundRobinRouter(nrOfInstances = NUM_MASTER_INSTANCES)),
      name = actorName)

    (actorSystem, boundPort)


  }



  private[Yosemite] class MasterActor(hostip:String,
                                      boundPort:Int) extends Actor with Logging{

    val publicaddress: String =hostip


    /**
      * Generate a new coflow ID given a coflow's submission date
      */
    def newJobId(submitDate: Date): String = {
      "Job-%06d".format(nextJobNumber.getAndIncrement())
    }



    // Method will be called, when
    def addSlave(Slave:String,IP:String,Port:Int,actor:ActorRef):SlaveInfo={
      val currentsender=sender
      val startTime=new Date(now)
      logInfo("register slave %s@%s:%d".format(Slave,IP,Port))
      val slave =new SlaveInfo(now,Slave,IP,Port,actor)
      idToSlave.put(slave.id, slave)
      slave
    }

    def addJob(slave:SlaveInfo,jobdesc:JobDescription,actor:ActorRef): JobInfo = {
      //idToJob.put(jobdesc.jobid,jobdesc)
      val now = System.currentTimeMillis()
      val date = new Date(now)
      val jobinfo = new JobInfo(now, newJobId(date), jobdesc, slave, date, actor)
      idToJob.put(jobinfo.id, jobinfo)
      // Update its parent client
      slave.addJob(jobinfo)
      jobinfo
    }



    override def receive = {

      // register the slave
    case RegisterSlave(id,ip,slavePort)=> {
      logInfo("receive slave information: "+id)
      val currentSender = sender

      // check if the slave has already been registered
      if (idToSlave.containsKey(id)){
        // register the slave
        logInfo("register slave fail"+id)
        currentSender ! RegisterSlaveFailed("Duplitcate Slave ID")
      }
      else{
        // add the slave into register slave hashtable
        val slave = addSlave(id,ip,slavePort,currentSender)
        currentSender ! RegisteredSlave(slave.id,"http://"+ip+":"+slavePort)
      }
    }


    case RegisterJob(slaveid,jobDescription)=>{

      val currentSender=sender
      val st=now

      logTrace("Registering job "+jobDescription.jobid)
      val slave=idToSlave.get(slaveid)

      if(slave==null){
        // send fail information to slave
        currentSender ! RegisterJobFailed("Invalid clientId " + slaveid)
      }
      else{
        // send success information to slave
        val jobinfo=addJob(slave,jobDescription,currentSender)

        // if registered successfully, sends the information to the slaveactor
        val slaveurl="Yosemite://"+slave.IP+":"+slave.Port
        val slaveactor=AkkaUtils.getActorRef(startSlave.toAkkaUrl(slaveurl),context)

        // sends the information back to the slave
        slaveactor ! RegisteredJob(jobinfo.id)
        logInfo("Registered job "+jobinfo.id+" in " +(now - st) + " milliseconds")
      }

    }


    case AddFile(filedesc)=>{
      logInfo("receive file information"+filedesc.toString)
      sender ! true
    }


    case _=>{
      logInfo("wrong message")
    }

    }


    override def preStart() ={
      logInfo("start varys at Yosemite://"+hostip+":"+port)
      context.system.eventStream.subscribe(self, classOf[RemotingLifecycleEvent])
    }

    override def postStop(): Unit = super.postStop()

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = super.preRestart(reason, message)

    override def postRestart(reason: Throwable): Unit = super.postRestart(reason)

    override def unhandled(message: Any): Unit = super.unhandled(message)


  }

}




object startMaster {
  private val YosemiteUrlRegex = "Yosemite://([^:]+):([0-9]+)".r
  private val systemName="YosemiteMaster"
  private val actorName="Master"

  var ip="127.0.0.1"
  var port=1606

  if(System.getenv("Yosemite.Master_IP")!=null){
    ip= System.getenv("Yosemite.Master_IP")
  }

  if(System.getenv("Yosemite.Master_Port")!=null){
    port=System.getenv("Yosemite_Master_Port").toInt
  }


  def parse(args:List[String]):Unit=args match{
    case ("--ip"|"-i")::value::tail=>
      ip=value
      parse(tail)

    case("--port"|"-p")::value::tail=>
      port=value.toInt
      parse(tail)

    case Nil=>{}

    case _=>
      printUsageAndExit(1)

  }

  /**
    * Print usage and exit JVM with the given exit code.
    */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Master [options]\n" +
        "\n" +
        "Options:\n" +
        "  -i IP, --ip IP         IP address or DNS name to listen on\n" +
        "  -p PORT, --port PORT   Port to listen on (default: 1606)\n")
    System.exit(exitCode)
  }


  def main(args: Array[String]) {
    parse(args.toList)
    val masterObj = new Master(systemName, actorName,ip,port)
    val (actorSystem, _) = masterObj.start()
    actorSystem.awaitTermination()
  }



  def toAkkaUrl(YosemiteUrl: String): String = {
    YosemiteUrl match {
      case YosemiteUrlRegex(host, port) =>
        "akka.tcp://%s@%s:%s/user/%s".format(systemName, host, port, actorName)
      case _ =>
        throw new YosemiteException("Invalid master URL: " + YosemiteUrl)
    }


  }


}