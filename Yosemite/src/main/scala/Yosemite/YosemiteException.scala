package Yosemite

/**
  * Created by zhanghan on 17/4/2.
  */

class YosemiteException(
                         message: String,
                         cause: Throwable)
  extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}