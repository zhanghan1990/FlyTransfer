package Yosemite.framework
/**
  * Created by zhanghan on 17/4/5.
  */


/**
  * job description used to describe the job
  * @param jobid: job's name
  * @param width: width of the job
  * @param weight: weight of the job
  */
class JobDescription(var jobid:String, var width:Int,var weight:Double =1.0) extends Serializable{
  override def toString: String =" "+jobid+","+width+","+weight
}

