import docker
client = docker.DockerClient(base_url='tcp://192.168.1.137:2375')

# To create the dockers in our cloud

# First check if the container exists
#container=client.containers.run(image='192.168.1.102:5000/varys',detach=True,name="master",ports={'22/tcp': 22222})


# List all the containers
print client.containers.list(all)

# Filter the particular docker
# see https://github.com/docker/docker-py/blob/master/docker/models/containers.py for more detail of the usage
#print client.containers.list(filters={"ancestor":"192.168.1.102:5000/varys"})

#containerall=client.containers.list(filters={"ancestor":"192.168.1.102:5000/varys"})
master=client.containers.list(filters={"name":"master"})
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