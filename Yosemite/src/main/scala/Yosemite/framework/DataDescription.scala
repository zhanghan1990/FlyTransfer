package Yosemite.framework

private[Yosemite] object DataType extends Enumeration {
  type DataType = Value
  val FAKE, INMEMORY, ONDISK = Value
}


private[Yosemite] case class DataIdentifier(
                                          dataId: String,
                                          jobId: String)




private [Yosemite] class FlowDescription(val id:String, val jobid:String, val datatype:DataType.DataType,
                                        val size:Long,val src:String, val srcPort:Int, val dst:String, val dstPort:Int)
  extends Serializable{

  val DataId=DataIdentifier(id,jobid)

  override def toString: String =
    "flow description flow id"+id+" jobid:"+jobid+" size:"+size+" src"+src+" srcport"+srcPort+" dst"+dst+"dstPort"+dstPort
}





private[Yosemite] class FileDescription(val fid:String,val filepath:String,val offset:Long, val _jobid:String,val _datatype:DataType.DataType,val _size:Long,val _src:String,val _srcPort:Int, val _dst:String, val _dstPort:Int)
  extends FlowDescription(fid,_jobid,_datatype,_size,_src,_srcPort,_dst,_dstPort){
  override def toString: String = "file description file id"+fid+" filepath"+filepath+" offset"+offset+" "+super.toString
}