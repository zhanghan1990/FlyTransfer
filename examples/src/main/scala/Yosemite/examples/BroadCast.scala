package Yosemite.examples

import java.io._
import java.net._
import java.util.concurrent.atomic.AtomicInteger

import Yosemite.framework._
import Yosemite.framework.client._
import Yosemite.{Logging, Utils}

import scala.concurrent.ExecutionContext


private[Yosemite] object BroadcastUtils {

  val BROADCAST_MASTER_PORT = 1608

  val BROADCAST_SLAVE_NUM_RETRIES = 5
  val BROADCAST_SLAVE_RETRY_INTERVAL_MS = 1000

}

private[Yosemite] case class BroadcastInfo(val coflowId: String, val DataName: String, val LEN_BYTES: Long)

private[Yosemite] case class BroadcastRequest()

private[Yosemite] case class BroadcastDone()

private[Yosemite] object BroadcastSender extends Logging {

  def exitGracefully(exitCode: Int) {

    System.exit(exitCode)
  }

  def main(args: Array[String]) {
    if (args.length < 5) {
      println("USAGE: BroadcastSender <YosemiteMasterUrl> <numSlaves> <dataName> <Size>B,<weight>")
      System.exit(1)
    }

    val url = args(0)
    val numSlaves = args(1).toInt
    val DataName = args(2)

    val LEN_BYTES = args(3).toInt

    val Weight=args(4).toDouble

    val listener = new TestListener
    val client = new YosemiteClient("BroadcastSender", url, false, listener)
    client.start()


    val desc = new CoflowDescription("Broadcast-" + DataName, CoflowType.DEFAULT, numSlaves, LEN_BYTES * numSlaves,0,Weight)

    val coflowId = client.registerCoflow(desc)
    logInfo("Registered coflow " + coflowId)

    var i=0
    // PUT blocks of the input file
    for(i<-0 to numSlaves){
      client.putFake(DataName+i, coflowId, LEN_BYTES, 1)
    }
    // Start server after registering the coflow and relevant
    val masterThread = new MasterThread(BroadcastInfo(coflowId,DataName,LEN_BYTES), numSlaves)
    masterThread.start()
    logInfo("Started MasterThread. Now waiting for it to die.")
    logInfo("Broadcast Master Url: %s:%d".format(
      Utils.localIpAddress, BroadcastUtils.BROADCAST_MASTER_PORT))

    // Wait for all slaves to receive
    masterThread.join()

    logInfo("Unregistered coflow " + coflowId)
    client.unregisterCoflow(coflowId)
  }

  class TestListener extends ClientListener with Logging {
    def connected(id: String) {
      logInfo("Connected to master, got client ID " + id)
    }

    def disconnected() {
      logInfo("Disconnected from master")
      System.exit(0)
    }
  }

  class MasterThread(val bInfo: BroadcastInfo, val numSlaves: Int, val serverThreadName: String = "BroadcastMaster")
    extends Thread(serverThreadName) with Logging {

    val HEARTBEAT_SEC = System.getProperty("Yosemite.framework.heartbeat", "1").toInt
    var serverSocket: ServerSocket = new ServerSocket(BroadcastUtils.BROADCAST_MASTER_PORT)

    var connectedSlaves = new AtomicInteger()
    var finishedSlaves = new AtomicInteger()

    var stopServer = false
    this.setDaemon(true)

    override def run() {
      var threadPool = Utils.newDaemonCachedThreadPool

      try {
        while (!stopServer && !finished) {
          println("finished slaves"+finishedSlaves.get())
          var clientSocket: Socket = null
          try {
            serverSocket.setSoTimeout(HEARTBEAT_SEC * 1000)
            clientSocket = serverSocket.accept
            logInfo("now accepting client socket")
          } catch {
            case e: Exception => {
              if (stopServer) {
                logInfo("Stopping " + serverThreadName)
              }
            }
          }

          if (clientSocket != null) {
            try {
              threadPool.execute(new Thread {
                override def run: Unit = {
                  val oos = new ObjectOutputStream(clientSocket.getOutputStream)
                  oos.flush
                  val ois = new ObjectInputStream(clientSocket.getInputStream)

                  try {
                    // Mark start of slave connection
                    val bMsg1 = ois.readObject.asInstanceOf[BroadcastRequest]
                    connectedSlaves.getAndIncrement()
                    println("connected Slaves"+connectedSlaves)
                    // Send file information
                    oos.writeObject(bInfo)
                    oos.flush
                    println("after writing the object to receiver")
                    // Mark end of slave connection
                    val bMsg2 = ois.readObject.asInstanceOf[BroadcastDone]
                    finishedSlaves.getAndIncrement()
                    println("finished slaves"+finishedSlaves)
                  } catch {
                    case e: Exception => {
                      logWarning(serverThreadName + " had a " + e)
                    }
                  } finally {
                    clientSocket.close
                  }
                }
              })
            } catch {
              // In failure, close socket here; else, client thread will close
              case e: Exception => {
                logError(serverThreadName + " had a " + e)
                clientSocket.close
              }
            }
          }
        }
      } finally {
        serverSocket.close
      }
      // Shutdown the thread pool
      threadPool.shutdown
    }

    def finished = (finishedSlaves.get() == numSlaves)

    def stopMaster() {
      stopServer = true
    }
  }
}

private[Yosemite] object BroadcastReceiver extends Logging {
  private val broadcastMasterUrlRegex = "([^:]+):([0-9]+)".r

  var sock: Socket = null
  var oos: ObjectOutputStream = null
  var ois: ObjectInputStream = null

  // ExecutionContext for Futures
  implicit val futureExecContext = ExecutionContext.fromExecutor(Utils.newDaemonCachedThreadPool())

  def main(args: Array[String]) {
    if (args.length < 3) {
      println("USAGE: BroadcastReceiver <varysMasterUrl> <broadcastMasterUrl><dataid>")
      System.exit(1)
    }

    val url = args(0)
    val bUrl = args(1)
    val clientid=args(2)

    var masterHost: String = null
    var masterPort: Int = 0
    var bInfo: BroadcastInfo = null

    bUrl match {
      case broadcastMasterUrlRegex(h, p) =>
        masterHost = h
        masterPort = p.toInt
      case _ =>
        logError("Invalid broadcastMasterUrl: " + bUrl)
        logInfo("broadcastMasterUrl should be given as host:port")
        exitGracefully(1)
    }

    // Connect to broadcast master, retry silently if required
    sock = createSocket(masterHost, masterPort)
    if (sock == null) {
      exitGracefully(1)
    }

    oos = new ObjectOutputStream(sock.getOutputStream)
    oos.flush
    ois = new ObjectInputStream(sock.getInputStream)

    // Mark start
    oos.writeObject(BroadcastRequest())
    oos.flush

    // Receive FileInfo
    bInfo = ois.readObject.asInstanceOf[BroadcastInfo]
    logInfo("Preparing to receive " + bInfo)

    // Now create coflow client
    val listener = new TestListener
    val client = new YosemiteClient("BroadcastReceiver", url, false, listener)
    client.start()


    logInfo("Getting " + bInfo.DataName+clientid + " of " + bInfo.LEN_BYTES + " from coflow " + bInfo.coflowId)

    client.getFake(bInfo.DataName+clientid,bInfo.coflowId)

    logInfo("get Fake Finished")
    // Mark end
    oos.writeObject(BroadcastDone())
    oos.flush
    logInfo("after getting the Fake process")
    // Close everything
    exitGracefully(0)
  }

  private def createSocket(host: String, port: Int): Socket = {
    var retriesLeft = BroadcastUtils.BROADCAST_SLAVE_NUM_RETRIES
    while (retriesLeft > 0) {
      try {
        val sock = new Socket(host, port)
        return sock
      } catch {
        case e: Exception => {
          logWarning("Failed to connect to " + host + ":" + port + " due to " + e.toString)
        }
      }
      Thread.sleep(BroadcastUtils.BROADCAST_SLAVE_RETRY_INTERVAL_MS)
      retriesLeft -= 1
    }
    null
  }

  def exitGracefully(exitCode: Int) {
    if (sock != null)
      sock.close

    System.exit(exitCode)
  }

  class TestListener extends ClientListener with Logging {
    def connected(id: String) {
      logInfo("Connected to master, got client ID " + id)
    }

    def disconnected() {
      logInfo("Disconnected from master")
      System.exit(0)
    }
  }
}
