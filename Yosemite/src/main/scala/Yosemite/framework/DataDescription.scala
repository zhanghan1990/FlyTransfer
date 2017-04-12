package Yosemite.framework

private[Yosemite] object DataType extends Enumeration {
  type DataType = Value
  val FAKE, INMEMORY, ONDISK = Value
}


private[Yosemite] case class DataIdentifier(
                                          dataId: String,
                                          jobId: String)


private [Yosemite] class FlowDescription(val id:String, val jobid:String, val datatype:DataType.DataType,
                                        val size:Long,val src:String, val srcPort:Int)
  extends Serializable{

  val DataId=DataIdentifier(id,jobid)

  override def toString: String =
    "flow "+id+" jobid:"+jobid+" size:"+size+" src"+src+" srcport"+srcPort
}



private[Yosemite] class FileDescription(val fid:String,val filepath:String,val _jobid:String,val _datatype:DataType.DataType,val _size:Long,val _src:String,val _srcPort:Int)
  extends FlowDescription(fid,_jobid,_datatype,_size,_src,_srcPort){
  override def toString: String = "file(id: "+fid+" filepath "+filepath+" jobid "+_jobid
}