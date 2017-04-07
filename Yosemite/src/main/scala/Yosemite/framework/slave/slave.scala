package Yosemite.framework.slave

import java.text.SimpleDateFormat
import java.util.Date

import Yosemite.Logging
import Yosemite.framework.RegisterSlave
import Yosemite.framework.master.startMaster
import Yosemite.utils.{AkkaUtils, IntParam, YosemiteUtil}
import akka.actor.{Actor, ActorRef, ActorSystem, Address, Props}
import akka.remote.RemotingLifecycleEvent

/**
  * Created by zhanghan on 17/4/3.
  * this file implements the slave action
  */


private[Yosemite] class SlaveActor(ip:String,port:Int,webUiPort: Int,
                                   commPort: Int,
                                   masterUrl: String,
                                   workDirPath: String = null) extends Actor with Logging{

  var master: ActorRef = null

  override def preStart() {
    connectToMaster()
  }

  override def postStop(): Unit = super.postStop()

  var masterAddress: Address = null


  val DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")  // For slave IDs
  val slaveId = generateSlaveId()


  def generateSlaveId(): String = {
    "slave-%s-%s-%d".format(DATE_FORMAT.format(new Date), ip, port)
  }

  override def receive = {
    case _=> println("DDD")
  }



  def connectToMaster()={
    logInfo("trying connected to master"+masterUrl)
    try {
      master = AkkaUtils.getActorRef(startMaster.toAkkaUrl(masterUrl), context)
      masterAddress = master.path.address
      master ! RegisterSlave(slaveId, ip, port)
      context.system.eventStream.subscribe(self, classOf[RemotingLifecycleEvent])
    } catch {
      case e: Exception =>
        logError("Failed to connect to master", e)
        System.exit(1)
    }

  }

}
private[Yosemite] object startSlave {

  // get slave ip
  var ip=YosemiteUtil.getLocalIPAddress
  var port =20000
  var masterurl:String=null

  if(System.getenv("Yosemite_SLAVE_IP")!=null){
    ip=System.getenv("Yosemite_SLAVE_IP")
  }

  if(System.getenv("Yosemite_SLAVE_PORT")!=null) {
    port = System.getenv("Yosemite_SLAVE_PORT").toInt
  }




  def parse(args:List[String]):Unit=args match {
    case ("--ip"|"-i")::value::tail=>
      ip= value
      parse(tail)

    case("--port"|"-p")::IntParam(value)::tail=>
      port=value
      parse(tail)

    case("--help"|"-h")::tail=>
      printUsageAndExit(0)

    case ("--master"|"-m")::value::tail=>
      if (masterurl !=null)
        printUsageAndExit(1)
      masterurl=value
      parse(tail)

    case Nil=>
      if(masterurl==null)
        printUsageAndExit(1)

    case _=>
      printUsageAndExit(1)
  }



  /**
    * Print usage and exit JVM with the given exit code.
    */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Slave [options] \n" +
        "Options:\n" +
        "  -d DIR, --work-dir DIR   Directory to run coflows in (default: YOSEMITE_HOME/work)\n" +
        "  -i IP, --ip IP           IP address or DNS name to listen on\n" +
        "  -p PORT, --port PORT     Port to listen on (default: random)\n" +
        "  --webui-port PORT        Port for web UI (default: 16017)\n" +
        "  -m MASTER,--master MASTER Address of the master\n"+
        "  --comm-port PORT        Port for Slave-To-Slave communication (default: 1607)")
    System.exit(exitCode)
  }

  def main(args:Array[String]): Unit ={

    parse(args.toList)
    val (actorSystem, _)=startSystemAndActor(ip,port,1111,1111,masterurl,"SDSD")
    actorSystem.awaitTermination()
  }

  def startSystemAndActor(host: String,
                           port: Int,
                           webUiPort: Int,
                           commPort: Int,
                           masterUrl: String,
                           workDir: String,
                           slaveNumber: Option[Int] = None): (ActorSystem, Int) = {

    // The LocalVarysCluster runs multiple local varysSlaveX actor systems
    val systemName = "YosemiteSlave" + slaveNumber.map(_.toString).getOrElse("")
    val (actorSystem, boundPort) = AkkaUtils.createActorSystem(systemName, host, port)
    val actor = actorSystem.actorOf(Props(new SlaveActor(host, boundPort, webUiPort, commPort,
      masterUrl, workDir)), name = "Slave")
    (actorSystem, boundPort)
  }

}
