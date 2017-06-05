# webMethods-IntegrationServer-SKYProfiler
SKYProfiler is a performance monitoring tool for Integration Server. SKYProfiler tracks the service invocations and the monitored data can be seen in real time. This helps users track the time each service invocation takes and further drills down to the child service to identify which service contributes to time.

## Description
SKY Profiler does real time analytics of profiled data from the production instances of webMethods Integration Server to identify potential bottlenecks and help operational teams avoid any downtimes.
SKY Profiler has two components. SKY Profiler Server and SKY Profiler Runtime.  
SKY Profiler Runtime is an Integration Server package which is required to send service invocation data to SKY Profiler server, which processes and displays information related to performance parameters.

## Requirements

The project was developed and tested on the following installation:

1. Integration Server 10.0  
Prepare your webMethods installation - your installation can contain only a plain Integration Server
2. Google Chrome Version 58.0
  
**Note: Currently SKY Profiler Runtime works only with webMethods Integration Server installed on Linux box.  
	The User Interface is tested on Google Chrome 58.0  
	The Service Summary Table shows only the latest data from the SKYProfiler server start. If there were services executed before server start those will be not be shown. However the report will show all the data.**

## Set-up

### Pre-requisite

The project needs below software as a pre-requisite to get started.
* Apache Ant
* Apache Maven
* MongoDB
* Zookeeper
* Apache Kafka

Download SKY Profiler by
```
git clone https://github.com/SoftwareAG/webMethods-IntegrationServer-SKYProfiler
```
 
Update maven path in _build.properties_.  
Create a data directory _data_ for MongoDB. (E.g., ```C:\Program Files\MongoDB\data```)  
Copy ```{sky-profiler_home}/zookeeper config/zoo.cfg``` to ```C:/zookeeper-3.4.9/conf```
Edit _zookeeper.properties_ in Apache Kafka to update the Data Directory location to C:/zookeeper-3.4.9/temp  
Edit _server.properties_ in Apache Kafka to update the Log Directory location to ```C:/kafka_2.11-0.10.1.1/kafka-logs```. Add _auto.create.topics.enable=true_ property at the end of the _server.properties_.  
Copy _wm-isclient.jar_ and _wm-isserver.jar_ from webMethods Integration Server installation to ```{sky-profiler_home}\libraries``` which are required for SKY Profiler Runtime component.

### Build
Before you run the command below make sure MongoDB, Zookeeper and Apache Kafka are started in the below fasion
* MongoDB
```
mongod.exe --dbpath=```"C:\Program Files\MongoDB\data"```
```

* Zookeeper
```
zkServer.cmd
```

* Apache Kafka
```
kafka-server-start.bat ..\..\config\server.properties
```

* Build SKY Profiler
```
ant all
```

The above command will build SKYProfiler.zip (webMethods Integration Server package) and skyprofiler-1.0-RELEASE.jar inside ```{sky-profiler_home}\dist``` directory. 

## How it works

Install the SKYProfiler package in the Integration Server which needs to be monitored.

**SKY Profiler Server requires the following services to be running to start-up**
* MongoDB
* Zookeeper
* Apache Kafka
SKY Profiler Server
```
java -jar skyprofiler-1.0-RELEASE.jar
```

Once the service is up, you could access the application on http://localhost:8080.  
Default credentials: admin/password1234


### Quick Start
	* Add a webMethods Integration Server which needs to be monitored and click on the server added
	* Navigate to Options->Configuration
	* Select the Package which needs to be monitored
	* Fill-in other details and Save
	* Click on Start to start monitoring
	* Any service execution that belongs to the selected will get displayed in the Service Monitoring table
		
### To obtain detailed information:
	* Click on the service name to expand the collapsible bar
	* A Response Time graph will be shown (Only the latest service execution reponse time will be displayed)
	* Click on one of the data point in the graph for further details. This will display Service Call Tree
	* Service call tree displays service hierarchy
	* This will show you which service is taking more time to execute
	* You could then click on the Graph icon corresponding to that service, which then opens up a window containing graphs like CPU, Response time, Threat Level CPU, etc
	* Click on the data point in one of the graphs to highlight the correlation line across all the graphs 
	* These graphs help you map the spiked response time with other performance parameters to decide which resource parameter could have caused the spike
		
### Report generation:
After all the profiling is completed you could generate a report as follows:
		
	* Navigate to home screen
	* Click on Options->Report
	* An HTML report is generated
	
## Notice
You could have MongoDB, Zookeeper and Apache Kafka running in different machines. The relevant information should be updated in each of the servers as required and in SKY Profiler Server _application.properties_ need to be updated.

Contact us at [TECHcommunity](mailto:technologycommunity@softwareag.com?subject=Github/SoftwareAG) if you have any questions.
