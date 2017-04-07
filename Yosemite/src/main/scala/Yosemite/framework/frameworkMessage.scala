package Yosemite.framework

/**
  * Created by zhanghan on 17/4/3.
  */

private[Yosemite] sealed trait FrameworkMessage extends Serializable

private[Yosemite] case class RegisterSlave(
                                         id: String,
                                         ip: String,
                                         port: Int) extends FrameworkMessage

private[Yosemite] case class RegisterJob(id:String, jobDescription: JobDescription) extends FrameworkMessage
private[Yosemite] case class UnregisterJob(id:String) extends FrameworkMessage