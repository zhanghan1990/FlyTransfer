package Yosemite.framework
/**
  * Created by zhanghan on 17/4/5.
  */


/**
  * job description used to describe the job
  * @param jobname: job's name
  * @param jobid: job id which is the identifier of job
  * @param width: width of the job
  * @param weight: weight of the job
  */
class JobDescription(var jobname:String,var jobid:String, var width:Int,var weight:Double =1.0) extends Serializable{
  override def toString: String =jobname+","+jobid+","+width+","+weight
}

