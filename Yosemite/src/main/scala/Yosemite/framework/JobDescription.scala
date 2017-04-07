package Yosemite.framework
/**
  * Created by zhanghan on 17/4/5.
  */


/**
  * job description used to describe the job
  * @param jobName: job's name
  * @param width: width of the job
  * @param length: maxlength of the job
  */
class JobDescription(var jobName:String, var width:Int,var length:Int) extends Serializable{
  override def toString: String ="JobName:"+jobName+" width:"+width+" length:"+length
}

