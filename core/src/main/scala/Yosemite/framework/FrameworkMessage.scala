package Yosemite.framework

import Yosemite.framework.master.{ClientInfo, CoflowInfo, SlaveInfo}


private[Yosemite] sealed trait FrameworkMessage extends Serializable

// Slave to Master
private[Yosemite] case class RegisterSlave(
                                         id: String,
                                         host: String,
                                         port: Int,
                                         webUiPort: Int,
                                         commPort: Int,
                                         publicAddress: String)
  extends FrameworkMessage

private[Yosemite] case class Heartbeat(
                                     slaveId: String,
                                     rxBps: Double,
                                     txBps: Double)
  extends FrameworkMessage

// Master to Slave
private[Yosemite] case class RegisteredSlave(
                                           masterWebUiUrl: String)
  extends FrameworkMessage

private[Yosemite] case class RegisterSlaveFailed(
                                               message: String)
  extends FrameworkMessage

// Client to Master
private[Yosemite] case class RegisterClient(
                                          clientName: String,
                                          host: String,
                                          commPort: Int)
  extends FrameworkMessage

private[Yosemite] case class RegisterCoflow(
                                          clientId: String,
                                          coflowDescription: CoflowDescription)
  extends FrameworkMessage

private[Yosemite] case class UnregisterCoflow(
                                            coflowId: String)
  extends FrameworkMessage

private[Yosemite] case class RequestBestRxMachines(
                                                 howMany: Int,
                                                 adjustBytes: Long)
  extends FrameworkMessage

private[Yosemite] case class RequestBestTxMachines(
                                                 howMany: Int,
                                                 adjustBytes: Long)
  extends FrameworkMessage

// Master/Client to Client/Slave
private[Yosemite] case class RegisteredClient(
                                            clientId: String,
                                            slaveId: String,
                                            slaveUrl: String)
  extends FrameworkMessage

private[Yosemite] case class CoflowKilled(
                                        message: String)
  extends FrameworkMessage

// Master to Client
private[Yosemite] case class RegisterClientFailed(
                                                message: String)
  extends FrameworkMessage

private[Yosemite] case class RegisteredCoflow(
                                            coflowId: String)
  extends FrameworkMessage

private[Yosemite] case class RegisterCoflowFailed(
                                                message: String)
  extends FrameworkMessage

private[Yosemite] case class UnregisteredCoflow(
                                              coflowId: String)
  extends FrameworkMessage

private[Yosemite] case class RejectedCoflow(
                                          coflowId: String,
                                          message: String)
  extends FrameworkMessage

private[Yosemite] case class BestRxMachines(
                                          bestRxMachines: Array[String])
  extends FrameworkMessage

private[Yosemite] case class BestTxMachines(
                                          bestTxMachines: Array[String])
  extends FrameworkMessage

private[Yosemite] case class UpdatedRates(
                                        newRates: Map[DataIdentifier, Double])
  extends FrameworkMessage

// Client/Slave to Slave/Master
private[Yosemite] case class AddFlow(
                                   flowDescription: FlowDescription)
  extends FrameworkMessage

private[Yosemite] case class AddFlows(
                                    flowDescriptions: Array[FlowDescription],
                                    coflowId: String,
                                    dataType: DataType.DataType)
  extends FrameworkMessage

private[Yosemite] case class GetFlow(
                                   flowId: String,
                                   coflowId: String,
                                   clientId: String,
                                   slaveId: String,
                                   flowDesc: FlowDescription = null)
  extends FrameworkMessage

private[Yosemite] case class GetFlows(
                                    flowIds: Array[String],
                                    coflowId: String,
                                    clientId: String,
                                    slaveId: String,
                                    flowDescs: Array[FlowDescription] = null)
  extends FrameworkMessage

private[Yosemite] case class FlowProgress(
                                        flowId: String,
                                        coflowId: String,
                                        bytesSinceLastUpdate: Long,
                                        isCompleted: Boolean)
  extends FrameworkMessage

private[Yosemite] case class DeleteFlow(
                                      flowId: String,
                                      coflowId: String)
  extends FrameworkMessage

// Slave/Master to Client/Slave
private[Yosemite] case class GotFlowDesc(
                                       flowDesc: FlowDescription)
  extends FrameworkMessage

private[Yosemite] case class GotFlowDescs(
                                        flowDescs: Array[FlowDescription])
  extends FrameworkMessage

// Internal message in Client/Slave
private[Yosemite] case object StopClient

private[Yosemite] case class GetRequest(
                                      flowDesc: FlowDescription,
                                      targetHost: String = null,
                                      targetCommPort: Int = 0) {

  // Not extending FrameworkMessage because it is NOT going through akka serialization
  // override def toString: String = "GetRequest(" + flowDesc.id+ ":" + flowDesc.coflowId + ")"
}

// Internal message in Master
private[Yosemite] case object ScheduleRequest

private[Yosemite] case object CheckForSlaveTimeOut

private[Yosemite] case object RequestWebUIPort

private[Yosemite] case class WebUIPortResponse(webUIBoundPort: Int)

// MasterWebUI To Master
private[Yosemite] case object RequestMasterState

// Master to MasterWebUI
private[Yosemite] case class MasterState(
                                       host: String,
                                       port: Int,
                                       slaves: Array[SlaveInfo],
                                       activeCoflows: Array[CoflowInfo],
                                       completedCoflows: Array[CoflowInfo],
                                       activeClients: Array[ClientInfo]) {

  def uri = "Yosemite://" + host + ":" + port
}

//  SlaveWebUI to Slave
private[Yosemite] case object RequestSlaveState

// Slave to SlaveWebUI
private[Yosemite] case class SlaveState(
                                      host: String,
                                      port: Int,
                                      slaveId: String,
                                      masterUrl: String,
                                      rxBps: Double,
                                      txBps: Double,
                                      masterWebUiUrl: String)
