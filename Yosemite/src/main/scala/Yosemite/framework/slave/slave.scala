package Yosemite.framework.slave

import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

import Yosemite.framework.master.startMaster
import Yosemite.framework.{JobDescription, _}
import Yosemite.utils.{AkkaUtils, IntParam, YosemiteUtil}
import Yosemite.{Logging, YosemiteException}
import akka.actor.{Actor, ActorRef, ActorSystem, Address, Props}
import akka.remote.RemotingLifecycleEvent

/**
  * Created by zhanghan on 17/4/3.
  * this file implementation of  the slave
  */


private[Yosemite] object startSlave extends  Logging{


  var master: ActorRef = null
  var slaveIdentify:String=null


  val slaveRegisterLock = new Object
  val NameToJob = new ConcurrentHashMap[String, String]()

//  val jobRegisterLock = new Object
//
//  //wait until the job has been registered
//  private def waitForJobReigstration={
//
//  }

  // Wait until the client has been registered
  private def waitForRegistration = {
    while (slaveIdentify == null) {
      slaveRegisterLock.synchronized {
        slaveRegisterLock.wait()
        slaveRegisterLock.notifyAll()
      }
    }
  }




  class SlaveActor(ip:String,port:Int,webUiPort: Int,
                                     commPort: Int,
                                     masterUrl: String,
                                     workDirPath: String = null) extends Actor with Logging{



    // The ingress and egress bandwidth of the ports
    var lastRxBytes = -1.0
    var lastTxBytes = -1.0

    var curRxBps = 0.0
    var curTxBps = 0.0



    override def preStart() {
      connectToMaster()
    }

    override def postStop(): Unit = super.postStop()

    var masterAddress: Address = null


    val DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss")  // For slave IDs


    override def receive = {


      case RegisterSlaveFailed(message)=>
        logInfo(message)

      case RegisteredSlave(id,slaveInfo)=>
        slaveIdentify = id
        // open all the rock
        slaveRegisterLock.synchronized { slaveRegisterLock.notifyAll() }
        //logInfo("Registered to master in " +  (now - regStartTime) +
         // " milliseconds. Local slave url = " + slaveUrl)
        logInfo("receive slave information: "+slaveInfo)

      case RegisteredJob(message)=>
        var list=message.split(" ")
        logInfo("job"+list(0)+" id"+list(1))
        NameToJob.put(list(0),list(1))

      case _=>
        logError("receive error message please check the syetem")

    }



    // The Method is called when constructing the slave object
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



  // get slave ip
  var ip=YosemiteUtil.getLocalIPAddress
  var port =20000
  var masterurl:String=null

  private val systemName = "YosemiteSlave"
  private val actorName = "Slave"
  private val YosemiteUrlRegex = "Yosemite://([^:]+):([0-9]+)".r





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

  val slaveId = generateSlaveId(ip)


  def generateSlaveId(ip:String): String = {
    "slave-%s".format(ip)
  }



  /**
    * Print usage and exit JVM with the given exit code.
    */
  def printUsageAndExit(exitCode: Int) {
    System.err.println(
      "Usage: Slave [options] \n" +
        "Options:\n" +
        "  -d DIR, --work-dir DIR   Directory to run Job in (default: YOSEMITE_HOME/work)\n" +
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

    RegisterFileJob("TEST","ddd",2,1.2)

    val SLEEP_MS1 = 5000

    Thread.sleep(SLEEP_MS1)

    println("Registered coflow " + NameToJob.get("TEST") + ". Now sleeping for " + SLEEP_MS1 + " milliseconds.")

    PUTFILE("/Users/zhanghan/Documents/文件资料/coflow/Yosemite/run",NameToJob.get("TEST"),"DDD")

    actorSystem.awaitTermination()
  }


  //@API
  // Register the FileJob, tell the master the job description
  def RegisterFileJob(jobName:String,jobId:String,width:Int,weight:Double): Unit ={
    //construct the job description
    var jobdesc:JobDescription= new JobDescription(jobName,jobId,width,weight)
    Reigster(generateSlaveId(ip),jobdesc)
  }



  //@API
  // Register the job, tell the master the job description
  def Reigster(clientid:String,jobDescription: JobDescription): Unit ={
    waitForRegistration
    // tell me, the file description, then I give the description to master
    master ! RegisterJob(clientid,jobDescription)
  }


  // @API
  // PUT: tell the master of file description
  def PUTFILE(filePath:String,jobId:String,fileId:String) {

    // This information is useful, as the client may be null, if the put operation is too fast
    waitForRegistration
    var file = new File(filePath)
    var filesize:Long=0
    if(file.exists()==false){
      // for file does not exit
      logWarning("file"+filePath+"does not exit")
      System.exit(1)
    }
    else{
      filesize=file.length()
    }
    // We use file path to represent filepath now
    var desc=new FileDescription(fileId,filePath,jobId,DataType.ONDISK,filesize,ip,port)
    // tell me, the file description, then I give the description to master
    master !AddFile(desc)
  }

  /**
    * API: GETFILE
    * GET FILE FROM ACTOR
    * @param fileId: file identifier
    * @param jodId:  job identifier
    * @return
    */

  def GETFILE(fileId:String,jodId:String) {
    waitForRegistration
    var flowDesc:FlowDescription=null
    val gotFlowDesc = AkkaUtils.askActorWithReply[Option[GetFlowDesc]](master, GetFILE(fileId, jodId))

  }

  def toAkkaUrl(slaveUrl:String):String={
    slaveUrl match{
      case YosemiteUrlRegex(ip,port)=>
        "akka.tcp://%s@%s:%s/user/%s".format(systemName, ip, port, actorName)
      case _=>
        throw new YosemiteException("Invalid Yosemite url"+slaveUrl)
    }
  }

  def startSystemAndActor(host: String,
                           port: Int,
                           webUiPort: Int,
                           commPort: Int,
                           masterUrl: String,
                           workDir: String,
                           slaveNumber: Option[Int] = None): (ActorSystem, Int) = {


    val systemName = "YosemiteSlave" + slaveNumber.map(_.toString).getOrElse("")
    val (actorSystem, boundPort) = AkkaUtils.createActorSystem(systemName, host, port)
    val actor = actorSystem.actorOf(Props(new SlaveActor(host, boundPort, webUiPort, commPort,
      masterUrl, workDir)), name = "Slave")
    (actorSystem, boundPort)
  }

}
