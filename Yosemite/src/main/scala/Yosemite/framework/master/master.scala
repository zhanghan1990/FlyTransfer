/**
  * Created by zhanghan on 17/4/2.
  */
package Yosemite.framework.master

import java.util.concurrent.ConcurrentHashMap

import Yosemite.framework.slave.SlaveInfo
import Yosemite.{Logging, YosemiteException}
import Yosemite.framework.{RegisterJob, RegisterSlave}
import Yosemite.utils.AkkaUtils
import akka.actor.{Actor, ActorSystem, Props}
import akka.remote.RemotingLifecycleEvent
import akka.routing.RoundRobinRouter


// define the communication system of the master
private[Yosemite] class Master(systemName:String,actorName:String, hostip:String,
                              port:Int) extends Logging{

  // number of master in our system
  val NUM_MASTER_INSTANCES = System.getProperty("varys.master.numInstances", "1").toInt

  var SlaveId = new ConcurrentHashMap[String,SlaveInfo]

  def now() =System.currentTimeMillis()

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


    override def receive = {

      // register the slave
    case RegisterSlave(id,ip,slavePort)=>{
      logInfo(id+" "+ip+" "+slavePort)
      val currentSender=sender
      currentSender ! "Duplicate slave ID"
      }

    case RegisterJob(id,jobDescription)=>{
      logInfo("register the job "+jobDescription.toString)
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