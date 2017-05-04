#-*- coding: utf-8 -*-
#!/usr/bin/python 

import docker
import thread
import threading
import time
import sys
import random


Task=[]
slaveall=[]


MAXFLOWSIZE=1024*1024*100
MINFLOWSIZE=1024*1024

mutex = threading.Lock()
def startCoflow(clientid,masterip,coflowname,coflowsize,clientnumber):
    print "start coflow  "+coflowname+" coflow size "+str(coflowsize)
    coflowCmd='java -cp Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastSender Yosemite://'+masterip+':1606 '+str(clientnumber)+' '+ coflowname+' '+str(coflowsize)
    print coflowCmd
    slaveall[clientid].exec_run(cmd=coflowCmd,detach=True)
    if mutex.acquire(1):
        Task.append({"broadmaster":slaveall[clientid].attrs['NetworkSettings']['Networks']['bridge']['IPAddress']+":1608","clientnumber":clientnumber,"isdeal":False})
        mutex.release()



def receiveCoflow(clientid,flowid,masterip,broadmaster):
    coflowCmd='java -cp Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastReceiver Yosemite://'+masterip+':1606 '+' '+str(broadmaster)+' '+str(flowid)
    print coflowCmd
    slaveall[clientid].exec_run(cmd=coflowCmd,detach=True)






if __name__ == "__main__":

    '''
    Get the number of slaves, we want to start
    '''

    client = docker.DockerClient(base_url='tcp://192.168.1.102:2375')
    print "Start slave client number  "+sys.argv[1]
    clientnum = int(sys.argv[1])


    '''
    Stop all the slave in the data center at first
    '''
    slaveall=client.containers.list(filters={"ancestor":"slave"})
    for c in slaveall:
        print "now removing slave with the name "+c.name +" and ip address "+c.attrs['NetworkSettings']['Networks']['bridge']['IPAddress']
        c.stop()
        c.remove()


    '''
    Remove the master
    '''
    containerall=client.containers.list(filters={"ancestor":"master"})
    for c in containerall:
        print "now removing master with the name "+c.name +" and ip address "+c.attrs['NetworkSettings']['Networks']['bridge']['IPAddress']
        c.stop()
        c.remove()





    '''
    Start the master 
    '''
    master=client.containers.run(image='master',name='master',detach=True,ports={'16016/tcp': 16016})

    containerall=client.containers.list(filters={"ancestor":"master"})

    '''
    Get the master ip address and start the slave on the master 
    '''
    masterip=containerall[0].attrs['NetworkSettings']['Networks']['bridge']['IPAddress']
    mastercmd='java -cp Yosemite-core-assembly-0.2.0-SNAPSHOT.jar  Yosemite.framework.slave.Slave  Yosemite://'+str(masterip)+':1606 -n'
    containerall[0].exec_run(cmd=mastercmd,detach=True)
    print 'start the master '+master.name+'  with ip address '+masterip


    '''
    Start the slave we want to use in the experiment
    clientArrat stores slave array
    '''
    for i in range(0,clientnum):
        cmd='java -cp Yosemite-core-assembly-0.2.0-SNAPSHOT.jar  Yosemite.framework.slave.Slave  Yosemite://'+str(masterip)+':1606 -n'
        print cmd
        slavename='Yosemiteslave-'+str(i)
        c=client.containers.run(image='slave',name=slavename,command=cmd,detach=True)
        print "run slave "+str(c)



    '''
    Start the experiment
    '''

    slaveall=client.containers.list(filters={"ancestor":"slave"})

    for i in range(0,clientnum):
        try:
            ## Random generate the number of coflow width
            coflowName="coflow-"+str(i)
            flowsize=int(random.uniform(MINFLOWSIZE, MAXFLOWSIZE))
            width=int(random.uniform(1,clientnum))

            thread.start_new_thread(startCoflow,(i,masterip, coflowName,flowsize,width))
        except:
            print "thread occurs exception"


    time.sleep(10)

    while 1:
        for t in Task:
            if t['isdeal']==False:
                print t
                w=int(t['clientnumber'])
                used=[]
                for i in range(0,w):
                    clientid=int(random.uniform(0,clientnum))
                    while clientid in used:
                        clientid=int(random.uniform(0,clientnum))
                    used.append(clientid)
                    thread.start_new_thread(receiveCoflow,(clientid,i+1,masterip,t['broadmaster']))
                    if mutex.acquire(1):
                        t['isdeal']=True
                        mutex.release()
        time.sleep(5)
        pass