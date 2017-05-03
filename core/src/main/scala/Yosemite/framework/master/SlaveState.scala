package Yosemite.framework.master

private[Yosemite] object SlaveState extends Enumeration {
  type SlaveState = Value

  val ALIVE, DEAD, DECOMMISSIONED = Value
}
