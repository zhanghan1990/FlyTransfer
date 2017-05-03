/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package Yosemite.framework.master.ui

import akka.actor._

import javax.servlet.http.HttpServletRequest

import org.eclipse.jetty.server.{Handler, Server}

import scala.concurrent.duration.Duration

import Yosemite.{Logging, Utils}
import Yosemite.ui.JettyUtils
import Yosemite.ui.JettyUtils._

/**
  * Web UI server for the standalone master.
  */
private[Yosemite]
class MasterWebUI(masterActorRef_ : ActorRef, requestedPort: Int, bDNS: Boolean = true) extends Logging {
  implicit val timeout = Duration.create(
    System.getProperty("Yosemite.akka.askTimeout", "10").toLong, "seconds")
  val port = requestedPort
  if (bDNS == false) {
    host = Utils.localIpAddress
  }
  val masterActorRef = masterActorRef_
  val coflowPage = new CoflowPage(this)
  val indexPage = new IndexPage(this)
  val handlers = Array[(String, Handler)](
    ("/static", createStaticHandler(MasterWebUI.STATIC_RESOURCE_DIR)),
    ("/coflow/json", (request: HttpServletRequest) => coflowPage.renderJson(request)),
    ("/coflow", (request: HttpServletRequest) => coflowPage.render(request)),
    ("/json", (request: HttpServletRequest) => indexPage.renderJson(request)),
    ("*", (request: HttpServletRequest) => indexPage.render(request))
  )
  var host = Utils.localHostName()
  var server: Option[Server] = None
  var boundPort: Option[Int] = None

  def start() {
    try {
      val (srv, bPort) = JettyUtils.startJettyServer("0.0.0.0", port, handlers)
      server = Some(srv)
      boundPort = Some(bPort)
      logInfo("Started Master web UI at http://%s:%d".format(host, boundPort.get))
    } catch {
      case e: Exception =>
        logError("Failed to create Master JettyUtils", e)
        System.exit(1)
    }
  }

  def stop() {
    server.foreach(_.stop())
  }
}

private[Yosemite] object MasterWebUI {
  val STATIC_RESOURCE_DIR = "Yosemite/ui/static"
}
