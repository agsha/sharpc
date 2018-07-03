# Sharpc: A fast rpc mechanism.

### How to run on localhost (intellij)

If you just want to run it on localhost, There is a `ExampleMain` class which starts a client and server on localhost. If you are running through intellij, then make sure you add a vm argument by clicking "edit configuration" for log4j config file path (edit the path appropriately)

````
-Dlog4j.configurationFile=/Users/sharath.g/code/Javademos/sharpc/src/main/resources/log4j2.xml
````

### How to run client and server on remote machines


First compile the code

````
mvn package
````

Copy all the jars to a remote locations (both client and server machines)

````
scp -r target/dependency-jars/ 188.166.204.110:~/sharpc/
````


Run the server (on the server machine)

````
sharath.g@stage-fdpingestion-none-1060693:~/sharpc$ java -cp  '/home/sharath.g/sharpc/:/home/sharath.g/sharpc/*' sha.example.ExampleServer 19838

started listening on port 19838

````


On the client machine:

````
[sharath.g@stage-fdpingestion-none-1064365 /home/sharath.g]$ /usr/lib/jvm/j2sdk1.8-oracle/jre/bin/java -cp  '/home/sharath.g/sharpc/:/home/sharath.g/sharpc/*' sha.example.ExampleClient 10.32.55.150 19838
client finished connect
 req_per_sec: 3,660,046   total_requests:7,320,611
 req_per_sec: 4,481,395   total_requests:16,324,212
 req_per_sec: 4,598,689   total_requests:25,523,818
 req_per_sec: 4,684,805   total_requests:34,894,995
 req_per_sec: 4,641,582   total_requests:44,179,862
````
