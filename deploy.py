#-*- coding: utf-8 -*-
#!/usr/bin/python 
import paramiko
import threading


# import paramiko
# import select
# client = paramiko.SSHClient()
# client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
# client.connect("192.168.1.102","22","zhanghan","zhanghan",timeout=5)
# transport = client.get_transport()
# channel = transport.open_session()
# channel.exec_command("tail -f /var/log/everything/current")
# while True:
#   rl, wl, xl = select.select([channel],[],[],0.0)
#   if len(rl) > 0:
#       # Must be stdout
#       print channel.recv(1024)



# def ssh2(ip,username,passwd,cmd,port):
#     try:
#         ssh = paramiko.SSHClient()
#         ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
#         ssh.connect(ip,port,username,passwd,timeout=5)
#         stdin, stdout, stderr = ssh.exec_command(cmd)
#         while True:
#         	for line in iter(lambda: stdout.readline(2048), ""):
#         		print(line)

#     except :
#         print '%s\tError\n'%(ip)





# if __name__=='__main__':
#     cmd = 'nonhup /root/Yosemite/bin/start-all.sh &'#你要执行的命令列表
#     username = "root"  #用户名
#     passwd = "zhanghan"    #密码
#     threads = []   #多线程
#     ip="192.168.1.102"
#     # print "Begin......"
#     # for i in range(1,254):
#     #     ip = '192.168.1.'+str(i)
#     #     a=threading.Thread(target=ssh2,args=(ip,username,passwd,cmd,port))
#     #     a.start() 

#     a=threading.Thread(target=ssh2,args=(ip,username,passwd,cmd,11111))
#     a.start()

# #     a=threading.Thread(target=ssh2,args=(ip,username,passwd,cmd,11112))
# #     a.start()

# #     a=threading.Thread(target=ssh2,args=(ip,username,passwd,cmd,11113))
# #     a.start()








import docker
client = docker.DockerClient(base_url='tcp://192.168.1.102:2375')

# # To create the dockers in our cloud

# # First check if the container exists
# container=client.containers.run(image='192.168.1.102:5000/varys',command='nohup /root/Yosemite/run varys.framework.master.Master -n &',detach=True,name="master",ports={'22/tcp': 22226})


# List all the containers
#print client.containers.list(all)

# Filter the particular docker
# see https://github.com/docker/docker-py/blob/master/docker/models/containers.py for more detail of the usage
# c1=client.containers.list(filters={"id":"371841afc7"})
# try:
# 	r=c1[0].exec_run(cmd='nohup /root/Yosemite/run varys.framework.master.Master -n &',tty=True,stream=True)
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

containerall=client.containers.list(filters={"ancestor":"slave"})
for c in containerall:
    print c.attrs['NetworkSettings']['Networks']['bridge']['IPAddress']
# containerall[0].exec_run(cmd='java -cp varys-examples-assembly-0.2.0-SNAPSHOT.jar varys.examples.BroadcastSender varys://192.168.5.10:1606 2 \"DDDD\" 10000',detach=True)
# containerall[1].exec_run(cmd='java -cp varys-examples-assembly-0.2.0-SNAPSHOT.jar varys.examples.BroadcastSender varys://192.168.5.10:1606 1 \"111\" 10000',detach=True)
# containerall[1].exec_run(cmd='java -cp varys-examples-assembly-0.2.0-SNAPSHOT.jar varys.examples.BroadcastReceiver varys://192.168.5.10:1606 192.168.5.42:1608  1',detach=True)
# containerall[2].exec_run(cmd='java -cp varys-examples-assembly-0.2.0-SNAPSHOT.jar varys.examples.BroadcastReceiver varys://192.168.5.10:1606 192.168.5.42:1608  2',detach=True)
# containerall[2].exec_run(cmd='java -cp varys-examples-assembly-0.2.0-SNAPSHOT.jar varys.examples.BroadcastReceiver varys://192.168.5.10:1606 192.168.5.43:1608  2',detach=True)
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
#print client.images(name='192.168.1.102:5000/varys')
#c.containers.run("192.168.1.102:5000/varys", detach=True)