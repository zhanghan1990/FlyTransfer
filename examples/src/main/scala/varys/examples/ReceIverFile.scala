/**
  * Created by zhanghan on 17/4/24.
  */

package varys.examples

import java.io._
import java.net._
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{Future, Await, ExecutionContext}

import varys.util.AkkaUtils
import varys.{Logging, Utils}
import varys.framework.client._
import varys.framework._


private[varys] object ReceiverClientFile {

  def main(args: Array[String]) {
    if (args.length < 2) {
      println("USAGE: ReceiverClientFile <masterUrl> <coflowId> [fileName]")
      System.exit(1)
    }

    val url = args(0)
    val coflowId = args(1)
    val FILE_NAME = if (args.length > 2) args(2) else "INFILE"

    val listener = new TestListener
    val client = new VarysClient("ReceiverClientFile", url, false, listener)
    client.start()

    Thread.sleep(5000)

    println("Trying to retrieve " + FILE_NAME)
    client.getFile(FILE_NAME, coflowId)
    println("Got " + FILE_NAME + ". Now waiting to die.")

    client.awaitTermination()
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
