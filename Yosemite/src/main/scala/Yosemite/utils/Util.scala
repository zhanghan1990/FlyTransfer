package Yosemite.utils

/**
  * Created by zhanghan on 17/4/5.
  * Define some util function
  */

private[Yosemite] object IntParam {
  def unapply(str: String): Option[Int] = {
    try {
      Some(str.toInt)
    } catch {
      case e: NumberFormatException => None
    }
  }
}
