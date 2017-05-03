package Yosemite.framework.master

private[Yosemite] object CoflowState extends Enumeration {

  type CoflowState = Value

  val WAITING, READY, RUNNING, FINISHED, FAILED, REJECTED = Value
}
