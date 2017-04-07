package Yosemite.utils

import java.io._
import java.net.{Inet4Address, InetAddress, NetworkInterface}
import java.util.concurrent.{Executors, ThreadFactory, ThreadPoolExecutor}
import java.util.{Locale, Random}

import Yosemite.Logging
import com.google.common.util.concurrent.ThreadFactoryBuilder

import scala.io.Source
import scala.reflect.ClassTag

/**
  * Created by zhanghan on 17/4/5.
  */
object YosemiteUtil extends Logging {
  /**
    * Serialize an object using Java serialization
    */
  def serialize[T](o: T): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(o)
    oos.close()
    return bos.toByteArray
  }

  /**
    * Deserialize an object using Java serialization
    */
  def deserialize[T](bytes: Array[Byte]): T = {
    val bis = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bis)
    return ois.readObject.asInstanceOf[T]
  }

  /**
    * Deserialize an object using Java serialization and the given ClassLoader
    */
  def deserialize[T](bytes: Array[Byte], loader: ClassLoader): T = {
    val bis = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bis) {
      override def resolveClass(desc: ObjectStreamClass) =
        Class.forName(desc.getName, false, loader)
    }
    return ois.readObject.asInstanceOf[T]
  }

  def isAlpha(c: Char): Boolean = {
    (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
  }

  /**
    * Shuffle the elements of a collection into a random order, returning the
    * result in a new collection. Unlike scala.util.Random.shuffle, this method
    * uses a local random number generator, avoiding inter-thread contention.
    */
  def randomize[T: ClassTag](seq: TraversableOnce[T]): Seq[T] = {
    randomizeInPlace(seq.toArray)
  }

  /**
    * Shuffle the elements of an array into a random order, modifying the
    * original array. Returns the original array.
    */
  def randomizeInPlace[T](arr: Array[T], rand: Random = new Random): Array[T] = {
    for (i <- (arr.length - 1) to 1 by -1) {
      val j = rand.nextInt(i)
      val tmp = arr(j)
      arr(j) = arr(i)
      arr(i) = tmp
    }
    arr
  }

  /**
    * Get the local host's IP address in dotted-quad format (e.g. 1.2.3.4).
    */

  def getLocalIPAddress():String={
    val retAddress=System.getenv("Yosemite_LOCAL_IP")
    if(retAddress!=null){
      retAddress
    }
    else{
      val address=NetworkInterface.getNetworkInterfaces
      while(address!=null && address.hasMoreElements){
        var n=address.nextElement()
        val is = n.getInetAddresses
        while(is.hasMoreElements){
          var i=is.nextElement()
          if(i.isLoopbackAddress==false &&i.isMulticastAddress==false && i.isLinkLocalAddress==false && i.isInstanceOf[Inet4Address])
            return i.getHostAddress
        }
      }
      "None"
    }
  }

  private var customHostname: Option[String] = None

  /**
    * Allow setting a custom host name because when we run on Mesos we need to use the same
    * hostname it reports to the master.
    */
  def setCustomHostname(hostname: String) {
    customHostname = Some(hostname)
  }

  /**
    * Get the local machine's hostname.
    */
  def localHostName(): String = {
    customHostname.getOrElse(InetAddress.getLocalHost.getHostName)
  }

  private[Yosemite] val daemonThreadFactory: ThreadFactory =
    new ThreadFactoryBuilder().setDaemon(true).build()

  /**
    * Wrapper over newCachedThreadPool.
    */
  def newDaemonCachedThreadPool(): ThreadPoolExecutor =
    Executors.newCachedThreadPool(daemonThreadFactory).asInstanceOf[ThreadPoolExecutor]

  /**
    * Wrapper over newFixedThreadPool.
    */
  def newDaemonFixedThreadPool(nThreads: Int): ThreadPoolExecutor =
    Executors.newFixedThreadPool(nThreads, daemonThreadFactory).asInstanceOf[ThreadPoolExecutor]

  /**
    * Convert a quantity in bytes to a human-readable string such as "4.0 MB".
    */
  def bytesToString(size: Long): String = {
    val TB = 1L << 40
    val GB = 1L << 30
    val MB = 1L << 20
    val KB = 1L << 10

    val (value, unit) = {
      if (size >= 2*TB) {
        (size.asInstanceOf[Double] / TB, "TB")
      } else if (size >= 2*GB) {
        (size.asInstanceOf[Double] / GB, "GB")
      } else if (size >= 2*MB) {
        (size.asInstanceOf[Double] / MB, "MB")
      } else if (size >= 2*KB) {
        (size.asInstanceOf[Double] / KB, "KB")
      } else {
        (size.asInstanceOf[Double], "B")
      }
    }
    "%.1f %s".formatLocal(Locale.US, value, unit)
  }

  /**
    * Return a string containing part of a file from byte 'start' to 'end'.
    */
  def offsetBytes(path: String, start: Long, end: Long): String = {
    val file = new File(path)
    val length = file.length()
    val effectiveEnd = math.min(length, end)
    val effectiveStart = math.max(0, start)
    val buff = new Array[Byte]((effectiveEnd-effectiveStart).toInt)
    val stream = new FileInputStream(file)

    stream.skip(effectiveStart)
    stream.read(buff)
    stream.close()
    Source.fromBytes(buff).mkString
  }

}
