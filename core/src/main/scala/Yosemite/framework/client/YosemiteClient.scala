package Yosemite.framework.client

import java.io._
import java.net._
import java.util.concurrent.ConcurrentHashMap

import akka.actor.{Terminated, _}
import akka.pattern.{AskTimeoutException, ask}
import akka.remote.{DisassociatedEvent, RemotingLifecycleEvent}
import Yosemite.framework._
import Yosemite.framework.master.Master
import Yosemite.framework.slave.Slave
import Yosemite.util._
import Yosemite.{Logging, Utils, YosemiteException}

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class YosemiteClient(
                   clientName: String,
                   masterUrl: String,
                   isDNS: Boolean = false,
                   listener: ClientListener = null)
  extends Logging {

  val INTERNAL_ASK_TIMEOUT_MS: Int =
    System.getProperty("Yosemite.client.internalAskTimeoutMillis", "5000").toInt
  val RATE_UPDATE_FREQ = System.getProperty("Yosemite.client.rateUpdateIntervalMillis", "100").toLong
  val SHORT_FLOW_BYTES = System.getProperty("Yosemite.client.shortFlowMB", "0").toLong * 1048576
  val NIC_BPS = 1024 * 1048576
  val clientRegisterLock = new Object
  val flowToTIS = new ConcurrentHashMap[DataIdentifier, ThrottledInputStream]()
  // TODO: Currently using flowToBitPerSec inside synchronized blocks. Might consider replacing with
  // an appropriate data structure; e.g., Collections.synchronizedMap.
  val flowToBitPerSec = new ConcurrentHashMap[DataIdentifier, Double]()
  val flowToObject = new HashMap[DataIdentifier, Array[Byte]]
  var actorSystem: ActorSystem = null
  var masterActor: ActorRef = null
  var slaveId: String = null
  var slaveUrl: String = null

  // ExecutionContext for Futures
  implicit val futureExecContext = ExecutionContext.fromExecutor(Utils.newDaemonCachedThreadPool())
  var slaveActor: ActorRef = null
  var clientId: String = null
  var clientActor: ActorRef = null
  var regStartTime = 0L
  var serverThreadName = "ServerThread for Client@" + Utils.localHostName()
  if (isDNS == false) {
    serverThreadName = "ServerThread for Client@" + Utils.localIpAddress
  }
  var dataServer = new DataServer(0, serverThreadName, isDNS, flowToObject)
  dataServer.start()




  var clientHost = Utils.localHostName()
  if (isDNS == false) {
    clientHost = Utils.localIpAddress
  }

  var clientCommPort = dataServer.getCommPort



  def start() {
    // Just launch an actor; it will call back into the listener.
    val (actorSystem_, _) = AkkaUtils.createActorSystem("YosemiteClient", Utils.localIpAddress, 0)
    actorSystem = actorSystem_
    clientActor = actorSystem.actorOf(Props(new ClientActor))
  }

  def stop() {
    if (clientActor != null) {
      try {
        val timeout = INTERNAL_ASK_TIMEOUT_MS.millis
        val future = clientActor.ask(StopClient)(timeout)
        Await.result(future, timeout)
      } catch {
        case e: AskTimeoutException => // Ignore it, maybe master went away
      }
      clientActor = null
    }
    dataServer.stop()
  }

  def awaitTermination() {
    actorSystem.awaitTermination()
  }

  def registerCoflow(coflowDesc: CoflowDescription): String = {
    waitForRegistration

    // Register with the master
    val RegisteredCoflow(coflowId) = AkkaUtils.askActorWithReply[RegisteredCoflow](masterActor,
      RegisterCoflow(clientId, coflowDesc))

    // Let the local slave know
    AkkaUtils.tellActor(slaveActor, RegisteredCoflow(coflowId))

    coflowId
  }

  def unregisterCoflow(coflowId: String) {
    waitForRegistration

    // Let the master know
    AkkaUtils.tellActor(masterActor, UnregisterCoflow(coflowId))

    // Update local slave
    AkkaUtils.tellActor(slaveActor, UnregisterCoflow(coflowId))

    // Free local resources
    freeLocalResources(coflowId)
  }

  private def freeLocalResources(coflowId: String) {
    flowToTIS.retain((dataId, _) => dataId.coflowId != coflowId)
    flowToBitPerSec.synchronized {
      flowToBitPerSec.retain((dataId, _) => dataId.coflowId != coflowId)
    }
    flowToObject.retain((dataId, _) => dataId.coflowId != coflowId)
  }

  /**
    * Puts any data structure
    */
  def putObject[T: Manifest](
                              objId: String,
                              obj: T,
                              coflowId: String,
                              size: Long,
                              numReceivers: Int) {

    // TODO: Figure out class name
    val className = "UnknownType"
    val desc =
      new ObjectDescription(
        objId,
        className,
        coflowId,
        DataType.INMEMORY,
        size,
        numReceivers,
        clientHost,
        clientCommPort)

    val serialObj = Utils.serialize[T](obj)
    handlePut(desc, serialObj)
  }

  /**
    * Makes data available for retrieval, and notifies local slave, which will register it with the
    * master.
    * Non-blocking call.
    */
  private def handlePut(flowDesc: FlowDescription, serialObj: Array[Byte] = null) {
    waitForRegistration

    val st = now

    // Notify the slave, which will notify the master
    AkkaUtils.tellActor(slaveActor, AddFlow(flowDesc))

    logInfo("Registered " + flowDesc + " in " + (now - st) + " milliseconds")

    // Keep a reference to the object to be served when asked for.
    if (flowDesc.dataType == DataType.INMEMORY) {
      assert(serialObj != null)
      flowToObject(flowDesc.dataId) = serialObj
    }
  }

  private def now() = System.currentTimeMillis

  // Wait until the client has been registered
  private def waitForRegistration = {
    while (clientId == null) {
      clientRegisterLock.synchronized {
        clientRegisterLock.wait()
        clientRegisterLock.notifyAll()
      }
    }
  }

  /**
    * Puts a complete local file
    */
  def putFile(fileId: String, pathToFile: String, coflowId: String, size: Long, numReceivers: Int) {
    putFile(fileId, pathToFile, coflowId, 0, size, numReceivers)
  }

  /**
    * Puts a range of local file
    */
  def putFile(
               fileId: String,
               pathToFile: String,
               coflowId: String,
               offset: Long,
               size: Long,
               numReceivers: Int) {

    val desc =
      new FileDescription(
        fileId,
        pathToFile,
        coflowId,
        DataType.ONDISK,
        offset,
        size,
        numReceivers,
        clientHost,
        clientCommPort)

    handlePut(desc)
  }

  /**
    * Emulates the process without having to actually put anything
    */
  def putFake(blockId: String, coflowId: String, size: Long, numReceivers: Int) {
    val desc =
      new FlowDescription(
        blockId,
        coflowId,
        DataType.FAKE,
        size,
        numReceivers,
        clientHost,
        clientCommPort)

    handlePut(desc)
  }

  /**
    * Puts multiple blocks at the same time of same size and same number of receivers
    * blocks => (blockId, blockSize, numReceivers)
    */
  def putFakeMultiple(blocks: Array[(String, Long, Int)], coflowId: String) {
    val descs =
      blocks.map(blk =>
        new FlowDescription(
          blk._1,
          coflowId,
          DataType.FAKE,
          blk._2,
          blk._3,
          clientHost,
          clientCommPort))

    handlePutMultiple(descs, coflowId, DataType.FAKE)
  }

  /**
    * Makes multiple pieces of data available for retrieval, and notifies local slave, which will
    * register it with the master.
    * Non-blocking call.
    * FIXME: Handles only DataType.FAKE right now.
    */
  private def handlePutMultiple(
                                 flowDescs: Array[FlowDescription],
                                 coflowId: String,
                                 dataType: DataType.DataType) {

    if (dataType != DataType.FAKE) {
      val tmpM = "handlePutMultiple currently supports only DataType.FAKE"
      logWarning(tmpM)
      throw new YosemiteException(tmpM)
    }

    waitForRegistration

    val st = now

    // Notify the slave, which will notify the master
    AkkaUtils.tellActor(slaveActor, AddFlows(flowDescs, coflowId, dataType))

    logInfo("Registered Array[FlowDescription] in " + (now - st) + " milliseconds")
  }

  /**
    * Retrieves data from any of the feasible locations.
    */
  @throws(classOf[YosemiteException])
  def getObject[T](objectId: String, coflowId: String): T = {
    val resp = handleGet(objectId, DataType.INMEMORY, coflowId)
    Utils.deserialize[T](resp)
  }

  /**
    * Notifies the master and the slave. But everything is done in the client
    * Blocking call.
    */
  @throws(classOf[YosemiteException])
  private def handleGet(
                         blockId: String,
                         dataType: DataType.DataType,
                         coflowId: String): Array[Byte] = {

    waitForRegistration

    var st = now

    // Notify master and retrieve the FlowDescription in response
    var flowDesc: FlowDescription = null

    val gotFlowDesc = AkkaUtils.askActorWithReply[Option[GotFlowDesc]](masterActor,
      GetFlow(blockId, coflowId, clientId, slaveId))
    gotFlowDesc match {
      case Some(GotFlowDesc(x)) => flowDesc = x
      case None => {
        val tmpM = "Failed to receive FlowDescription for " + blockId + " of coflow " + coflowId
        logWarning(tmpM)
        // TODO: Define proper VarysExceptions
        throw new YosemiteException(tmpM)
      }
    }
    logInfo("Received " + flowDesc + " for " + blockId + " of coflow " + coflowId + " in " +
      (now - st) + " milliseconds")

    // Notify local slave
    AkkaUtils.tellActor(slaveActor, GetFlow(blockId, coflowId, clientId, slaveId, flowDesc))

    // Get it!
    val (origFlowDesc, retVal) = getOne(flowDesc)
    // Notify flow completion
    logInfo("after get completion time")

    masterActor ! FlowProgress(
      origFlowDesc.id,
      origFlowDesc.coflowId,
      origFlowDesc.sizeInBytes,
      true)

    retVal
  }

  /**
    * Gets a file
    */
  @throws(classOf[YosemiteException])
  def getFile(fileId: String, coflowId: String): Array[Byte] = {
    handleGet(fileId, DataType.ONDISK, coflowId)
  }

  /**
    * Paired get() for putFake. Doesn't return anything, but emulates the retrieval process.
    */
  @throws(classOf[YosemiteException])
  def getFake(blockId: String, coflowId: String) {
    handleGet(blockId, DataType.FAKE, coflowId)
  }

  /**
    * Paired get() for putFakeMultiple. Doesn't return anything, but emulates the retrieval process.
    */
  @throws(classOf[YosemiteException])
  def getFakeMultiple(blockIds: Array[String], coflowId: String) {
    handleGetMultiple(blockIds, DataType.FAKE, coflowId)
  }

  /**
    * Notifies the master and the slave. But everything is done in the client
    * Blocking call.
    * FIXME: Handles only DataType.FAKE right now.
    */
  @throws(classOf[YosemiteException])
  private def handleGetMultiple(
                                 blockIds: Array[String],
                                 dataType: DataType.DataType,
                                 coflowId: String) {

    if (dataType != DataType.FAKE) {
      val tmpM = "handleGetMultiple currently supports only DataType.FAKE"
      logWarning(tmpM)
      throw new YosemiteException(tmpM)
    }

    waitForRegistration

    var st = now

    // Notify master and retrieve the FlowDescription in response
    var flowDescs: Array[FlowDescription] = null

    val gotFlowDescs = AkkaUtils.askActorWithReply[Option[GotFlowDescs]](
      masterActor,
      GetFlows(blockIds, coflowId, clientId, slaveId))

    gotFlowDescs match {
      case Some(GotFlowDescs(x)) => flowDescs = x
      case None => {
        val tmpM = "Failed to receive FlowDescriptions for " + blockIds.size + " flows of coflow " +
          coflowId
        logWarning(tmpM)
        // TODO: Define proper VarysExceptions
        throw new YosemiteException(tmpM)
      }
    }
    logInfo("Received " + flowDescs.size + " flowDescs " + " of coflow " + coflowId + " in " +
      (now - st) + " milliseconds")

    // Notify local slave
    AkkaUtils.tellActor(slaveActor, GetFlows(blockIds, coflowId, clientId, slaveId, flowDescs))

    // Get 'em!
    val recvLock = new Object()
    var recvFinished = 0

    for (flowDesc <- flowDescs) {
      new Thread("Receive thread for " + flowDesc) {
        override def run() {
          val (origFlowDesc, retVal) = getOne(flowDesc)
          // Notify flow completion
          masterActor ! FlowProgress(
            origFlowDesc.id,
            origFlowDesc.coflowId,
            origFlowDesc.sizeInBytes,
            true)

          recvLock.synchronized {
            recvFinished += 1
            recvLock.notifyAll()
          }
        }
      }.start()
    }

    recvLock.synchronized {
      while (recvFinished < flowDescs.size) {
        recvLock.wait()
      }
    }

    // val futureList = Future.traverse(flowDescs.toList)(fd => Future(getOne(fd)))
    // futureList.onComplete {
    //   case Right(arrayOfFlowDesc_Res) => {
    //     arrayOfFlowDesc_Res.foreach { x =>
    //       val (origFlowDesc, res) = x
    //       masterActor ! FlowProgress(
    //         origFlowDesc.id,
    //         origFlowDesc.coflowId,
    //         origFlowDesc.sizeInBytes,
    //         true)
    //     }
    //   }
    //   case Left(vex) => throw vex
    // }
  }

  /**
    * Performs exactly one get operation
    */
  @throws(classOf[YosemiteException])
  private def getOne(flowDesc: FlowDescription): (FlowDescription, Array[Byte]) = {
    var st = now
    val sock = new Socket(flowDesc.originHost, flowDesc.originCommPort)
    val oos = new ObjectOutputStream(new BufferedOutputStream(sock.getOutputStream))
    oos.flush

    // Don't wait for scheduling for 'SHORT' flows
    val tisRate = if (flowDesc.sizeInBytes > SHORT_FLOW_BYTES) 0.0 else NIC_BPS

    val tis = new ThrottledInputStream(sock.getInputStream, clientName, tisRate)
    flowToTIS.put(flowDesc.dataId, tis)
    // logTrace("Created socket and " + tis + " for " + flowDesc + " in " + (now - st) +
    //   " milliseconds")

    oos.writeObject(GetRequest(flowDesc))
    oos.flush

    var retVal: Array[Byte] = null

    st = now
    // Specially handle DataType.FAKE
    if (flowDesc.dataType == DataType.FAKE) {
      val buf = new Array[Byte](65536)
      var bytesReceived = 0L
      while (bytesReceived < flowDesc.sizeInBytes) {
        val n = tis.read(buf)
        //logInfo("Received " + n + " bytes of " + flowDesc.sizeInBytes)
        if (n == -1) {
          logError("EOF reached after " + bytesReceived + " bytes")
          throw new YosemiteException("Too few bytes received")
        } else {
          bytesReceived += n
        }
      }
    } else {
      val ois = new ObjectInputStream(tis)
      val resp = ois.readObject.asInstanceOf[Option[Array[Byte]]]
      resp match {
        case Some(byteArr) => {
          logInfo("Received response of " + byteArr.length + " bytes")

          flowDesc.dataType match {
            case DataType.ONDISK => {
              retVal = byteArr
            }

            case DataType.INMEMORY => {
              retVal = byteArr
            }

            case _ => {
              logError("Invalid DataType!")
              throw new YosemiteException("Invalid DataType!")
            }
          }
        }
        case None => {
          logError("Nothing received!")
          throw new YosemiteException("Invalid DataType!")
        }
      }
    }
    logTrace("Received " + flowDesc.sizeInBytes + " bytes for " + flowDesc + " in " + (now - st) +
      " milliseconds")

    // Close everything
    flowToTIS.remove(flowDesc.dataId)
    tis.close
    sock.close

    (flowDesc, retVal)
  }

  def deleteFlow(flowId: String, coflowId: String) {
    // TODO: Do something!
    // AkkaUtils.tellActor(slaveActor, DeleteFlow(flowId, coflowId))
  }

  /**
    * Receive 'howMany' machines with the lowest incoming usage
    */
  def getBestRxMachines(howMany: Int, adjustBytes: Long): Array[String] = {
    val BestRxMachines(bestRxMachines) = AkkaUtils.askActorWithReply[BestRxMachines](
      masterActor,
      RequestBestRxMachines(howMany, adjustBytes))
    bestRxMachines
  }

  /**
    * Receive the machine with the lowest incoming usage
    */
  def getBestRxMachine(adjustBytes: Long): String = {
    val BestRxMachines(bestRxMachines) = AkkaUtils.askActorWithReply[BestRxMachines](
      masterActor,
      RequestBestRxMachines(1, adjustBytes))
    bestRxMachines(0)
  }

  /**
    * Receive 'howMany' machines with the lowest outgoing usage
    */
  def getTxMachines(howMany: Int, adjustBytes: Long): Array[String] = {
    val BestTxMachines(bestTxMachines) = AkkaUtils.askActorWithReply[BestTxMachines](
      masterActor,
      RequestBestTxMachines(howMany, adjustBytes))
    bestTxMachines
  }

  /**
    * Receive the machine with the lowest outgoing usage
    */
  def getBestTxMachine(adjustBytes: Long): String = {
    val BestTxMachines(bestTxMachines) = AkkaUtils.askActorWithReply[BestTxMachines](
      masterActor,
      RequestBestTxMachines(1, adjustBytes))
    bestTxMachines(0)
  }

  class ClientActor extends Actor with Logging {
    var masterAddress: Address = null

    // To avoid calling listener.disconnected() multiple times
    var alreadyDisconnected = false

    override def preStart() {
      logInfo("Connecting to master " + masterUrl)
      regStartTime = now
      try {
        masterActor = AkkaUtils.getActorRef(Master.toAkkaUrl(masterUrl), context)
        masterAddress = masterActor.path.address
        masterActor ! RegisterClient(clientName, clientHost, clientCommPort)
        context.system.eventStream.subscribe(self, classOf[RemotingLifecycleEvent])
      } catch {
        case e: Exception =>
          logError("Failed to connect to master", e)
          markDisconnected()
          context.stop(self)
      }
    }

    override def receive = {

      case RegisteredClient(clientId_, slaveId_, slaveUrl_) =>
        clientId = clientId_
        slaveId = slaveId_
        slaveUrl = slaveUrl_
        slaveActor = AkkaUtils.getActorRef(Slave.toAkkaUrl(slaveUrl), context)
        if (listener != null) {
          listener.connected(clientId)
        }
        clientRegisterLock.synchronized {
          clientRegisterLock.notifyAll()
        } // Ready to go!
        logInfo("Registered to master in " + (now - regStartTime) +
          " milliseconds. Local slave url = " + slaveUrl)

        // Thread to periodically uodate the rates of all existing ThrottledInputStreams
        context.system.scheduler.schedule(0.millis, RATE_UPDATE_FREQ.millis) {
          flowToBitPerSec.synchronized {
            flowToBitPerSec.foreach { kv => {
              // kv (key = FlowId, value = Rate)
              if (flowToTIS.containsKey(kv._1))
                flowToTIS.get(kv._1).setNewRate(kv._2)
            }
            }
          }
        }

      case Terminated(actor_) if actor_ == masterActor =>
        masterDisconnected()

      case e: DisassociatedEvent if e.remoteAddress == masterAddress =>
        masterDisconnected()

      case StopClient =>
        markDisconnected()
        sender ! true
        context.stop(self)

      case UpdatedRates(newRates) =>
        logInfo("Received updated shares")
        flowToBitPerSec.synchronized {
          for ((dataId, newBitPerSec) <- newRates) {
            logTrace(dataId + " ==> " + newBitPerSec + " bps")
            flowToBitPerSec.put(dataId, newBitPerSec)
          }
        }

      case RejectedCoflow(coflowId, rejectMessage) =>
        logInfo("Coflow " + coflowId + " has been rejected! " + rejectMessage)

        // Let the client know
        if (listener != null) {
          listener.coflowRejected(coflowId, rejectMessage)
        }

        // Close ongoing streams, if any. This will raise exceptions in getOne()
        // and go back to the application.
        // TODO: Find a more elegant solution.
        flowToTIS.foreach { kv => {
          // kv (key = dataId, value = TIS)
          if (kv._1.coflowId == coflowId)
            kv._2.close()
        }
        }

        // Free local resources
        freeLocalResources(coflowId)
    }

    @throws(classOf[YosemiteException])
    def masterDisconnected() {
      // TODO: It would be nice to try to reconnect to the master, but just shut down for now.
      // (Note that if reconnecting we would also need to assign IDs differently.)
      val connToMasterFailedMsg = "Connection to master failed; stopping client"
      logWarning(connToMasterFailedMsg)
      markDisconnected()
      context.stop(self)
      throw new YosemiteException(connToMasterFailedMsg)
    }

    /**
      * Notify the listener that we disconnected, if we hadn't already done so before.
      */
    def markDisconnected() {
      if (!alreadyDisconnected) {
        if (listener != null) {
          listener.disconnected()
        }
        alreadyDisconnected = true
      }
    }

  }
}
