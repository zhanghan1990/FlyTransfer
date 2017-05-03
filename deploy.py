#-*- coding: utf-8 -*-
#!/usr/bin/python 

import docker
import thread
import time
import sys

def startCoflow(masterip,coflowname,coflowsize,clientnumber):
    print "start coflow  "+coflowname+" coflow size "+str(coflowsize)
    cmd='java -cp Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastSender Yosemite://'+masterip+':1606 '+str(clientnumber)+' '+ str(coflowsize)
    print cmd





if __name__ == "__main__":

    # get the number of slaves, we want to start
    print "Master ip  "+sys.argv[1]
    masterip=sys.argv[1]

    print "Start slave client number  "+sys.argv[2]
    clientnum = int(sys.argv[2])



    '''
    stop all the slave in the data center at first
    '''

    client = docker.DockerClient(base_url='tcp://192.168.1.102:2375')
    containerall=client.containers.list(filters={"ancestor":"slave"})
    for c in containerall:
        print "now removing slave"+c.name
        c.stop()
        c.remove()



    '''
    Start the slave we want to use in the experiment
    clientArrat stores client array
    '''
    clientArray=[]
    for i in range(0,clientnum):
        c=client.containers.run(image='slave',detach=True)
        print "run slave "+str(c)
        clientArray.append(c)

    '''
    Start the experiment
    '''

    for i in range(0,clientnum):
        try:
            thread.start_new_thread(startCoflow,("192.168.1.102", "dsdsdsd",111,2))
        except:
            print "thread occurs exception"

    while 1:
        pass
#     client = docker.DockerClient(base_url='tcp://192.168.1.102:2375')
#     containerall=client.containers.list(filters={"ancestor":"slave"})
#     for c in containerall:
#         print c.attrs['NetworkSettings']['Networks']['bridge']['IPAddress']
# # # To create the dockers in our cloud

# # First check if the container exists
# container=client.containers.run(image='192.168.1.102:5000/Yosemite',command='nohup /root/Yosemite/run Yosemite.framework.master.Master -n &',detach=True,name="master",ports={'22/tcp': 22226})


# List all the containers
#print client.containers.list(all)

# Filter the particular docker
# see https://github.com/docker/docker-py/blob/master/docker/models/containers.py for more detail of the usage
# c1=client.containers.list(filters={"id":"371841afc7"})
# try:
# 	r=c1[0].exec_run(cmd='nohup /root/Yosemite/run Yosemite.framework.master.Master -n &',tty=True,stream=True)
# 	print r.next()
# 	print r.next()
# 	print r.next()
# 	print r.next()
# 	print r.next()
# except Exception as e:
# 	print e
#print c1[0].logs()
# for i in range(0,30):
#     print client.containers.run(image='slave',detach=True)


#containerall[0].exec_run(cmd='java -cp Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastSender Yosemite://192.168.5.10:1606 2 \"DDDD\" 10000',detach=True)
# containerall[1].exec_run(cmd='java -cp Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastSender Yosemite://192.168.5.10:1606 1 \"111\" 10000',detach=True)
# containerall[1].exec_run(cmd='java -cp Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastReceiver Yosemite://192.168.5.10:1606 192.168.5.42:1608  1',detach=True)
# containerall[2].exec_run(cmd='java -cp Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastReceiver Yosemite://192.168.5.10:1606 192.168.5.42:1608  2',detach=True)
# containerall[2].exec_run(cmd='java -cp Yosemite-examples-assembly-0.2.0-SNAPSHOT.jar Yosemite.examples.BroadcastReceiver Yosemite://192.168.5.10:1606 192.168.5.43:1608  2',detach=True)
# for c in containerall:
#     c.stop()
#     c.remove()
#master=client.containers.list(filters={"name":"master"})
#print containerall
# for c in master:
# 	c.exec_run
	#print c.status
	# print the statua of the machine
	#print c.status

# 	# delete the server, should have the 2 steps
# 	c.stop()
# 	c.remove()

	#c.stop()
#stop container 
#
#container.logs()
#print c.info()
#print  c.images(all='True')
#print client.images(name='192.168.1.102:5000/Yosemite')
#c.containers.run("192.168.1.102:5000/Yosemite", detach=True)