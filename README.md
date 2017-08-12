# webmethods-integrationserver-skyprofiler
SKYProfiler is a performance monitoring tool for Integration Server. SKYProfiler tracks the service invocations and the monitored data can be seen in real time. This helps users track the time each service invocation takes and further drills down to the child service to identify which service contributes to time.

## Description
SKY Profiler does real time analytics of profiled data from the production instances of webMethods Integration Server to identify potential bottlenecks and help operational teams avoid any downtimes.
SKY Profiler has two components. SKY Profiler Server and SKY Profiler Runtime.  
SKY Profiler Runtime is an Integration Server package which is required to send service invocation data to SKY Profiler server, which processes and displays information related to performance parameters.

## Requirements

The project was developed and tested on the following installation:

1. Integration Server 10.0  
2. Google Chrome Version 58.0
  
Note:    
 1. Currently SKY Profiler Runtime works only with webMethods Integration Server installed on Linux and Windows box.   
 2. The Service Summary Table shows only the latest data. If there were services executed before SKY Profiler server start those will be not be shown. However the report will show all the data.

## Set-up with Docker


### Required tools

With docker, nothing else is needed but Docker engine and Docker-compose. (not even JAVA, ANT, etc...)

* [Docker engine](https://www.docker.com/products/overview)
* [Docker compose](https://docs.docker.com/compose/install/)

### Start SkyProfiler and related components

 1. Download the docker-compose file and save it anywhere on your docker-enabled workstation or server
 
 Click [skyprofiler docker-compose](./docker-compose.yml)

 2. Set some required environment variables
 
```bash
export KAFKA_ADVERTISED_HOST_NAME=$(hostname)
export MONITORED_WM_HOST1=<IP for IS1>
export MONITORED_WM_HOST2=<IP for IS2>
```

**Notes:**
 * KAFKA_ADVERTISED_HOST_NAME is needed so the Kafka component can be accessed from external servers (like the wM IS to be monitored)
 * MONITORED_WM_HOST variables are currently needed so the docker instance can route network requests to the external IS nodes to monitor
    (__TODO Fabien or others__: This is needed currently because the docker host's DNS server is not shared with the docker instances, but a better more generic way surely can be put in place)
 If you need more than 2 (very likely), currently you can simply modify the [docker-compose.yml](./docker-compose.yml) and add more hosts in the "extra_hosts" parameter.
 
 3. Start SkyProfiler and related components

```bash
docker-compose up -d
```

All the needed images will be downloaded from docker-hub and started in the right order, ready to operate.

 4. Open Skyprofiler Web UI

Once all docker instances are up and running (less than 1 minute), click [Skyprofiler Web UI](https://localhost:8080)

NOTE: if you started the Skyprofiler server on another machine, replace localhost with the docker VM IP address

 5. Login
 
 Default Login Credentials: admin/password1234
 
 6. Then, go to [How it works](#how_it_works) for configurations.

## Set-up without Docker

### Pre-requisite

The project needs below software as a pre-requisite to get started. 
* Apache Ant
* Apache Maven
* MongoDB
* Zookeeper
* Apache Kafka
* bower

**MongoDB**  
MongoDB will be used to store the executed service data. To install and configure MongoDB refer [FAQ](https://github.com/SoftwareAG/webMethods-IntegrationServer-SKYProfiler/blob/master/doc/FAQ.txt)  
Start the service  
```
C:\Program Files\MongoDB\Server\3.4\bin> mongod.exe --dbpath="C:\Program Files\MongoDB\data"
```

**Zookeeper**  
Zookeeper is used to provide distributed configuration service for Kafka. To install and configure Zookeeper refer [FAQ](https://github.com/SoftwareAG/webMethods-IntegrationServer-SKYProfiler/blob/master/doc/FAQ.txt)  
Start the service  
```
C:\zookeeper-3.4.9\bin> zkServer.cmd
```

**Apache Kafka**  
Kafka is a distributed messsaging system used for sending events from SKY Profiler Runtime to SKY Profiler Server. To install and configure Kafka refer [FAQ](https://github.com/SoftwareAG/webMethods-IntegrationServer-SKYProfiler/blob/master/doc/FAQ.txt)  
Start the service
```
C:\kafka_2.11-0.10.1.1\bin\windows> kafka-server-start.bat ..\..\config\server.properties
```

### SKY Profiler
Download SKY Profiler by
```
git clone https://github.com/SoftwareAG/webMethods-IntegrationServer-SKYProfiler
```
 
SKY Profiler Server requires Apache Ant, Apache Maven and bower to be present in the machine for build process. To install and configure Apache Ant and Apache Maven refer [FAQ](https://github.com/SoftwareAG/webMethods-IntegrationServer-SKYProfiler/blob/master/doc/FAQ.txt)  
Update maven path in _build.properties_.  
Copy _wm-isclient.jar_ and _wm-isserver.jar_ from webMethods Integration Server installation to ```{webMethods-IntegrationServer-SKYProfiler}\libraries``` which are required for SKY Profiler Runtime component.
Update MongoDB and Kafka configuration in ```{webMethods-IntegrationServer-SKYProfiler}\SKYProfilerServer\src\main\application.properties```  
bower will be used to resolve JS and CSS dependencies

**Build and Run SKY Profiler** 

* Build SKY Profiler
SKY Profiler build requires the following services to be up and running
1. MongoDB
2. Zookeeper
3. Apache Kafka

```
C:\{webMethods-IntegrationServer-SKYProfiler}> ant
```

The above command will create SKYProfiler.zip (webMethods Integration Server package) and skyprofiler-1.0-RELEASE.jar inside ```{webMethods-IntegrationServer-SKYProfiler}\dist``` directory. 

Install the SKYProfiler package in the Integration Server which needs to be monitored. Refer webMethods_Integration_Server_Administrators_Guide section "Installing and Updating Packages on a Server Instance" on how to install the package.  

* To start SKY Profiler Server
```
C:\{webMethods-IntegrationServer-SKYProfiler}\dist> java -jar skyprofiler-1.0-RELEASE.jar
```
Once the service is up, you could access the application in the URL http://localhost:8080.  
Default Login Credentials: admin/password1234

## How it works

### Quick Start
	* Login to the application
	* Add webMethods Integration Server which needs to be monitored
	* Select the added server
	* Navigate to Options->Configuration
	* Select the Package which needs to be monitored
	* Fill-in other details and Save
	* Click on Start to start monitoring
	* Any service execution that belongs to the selected package/s will get displayed in the Service Monitoring table
		
### To obtain detailed information
	* Click on the service name to expand the collapsible bar
	* A Response Time graph will be shown (Only the latest service execution reponse time will be displayed)
	* Click on one of the data point in the graph to get Service Call Tree
	* Service call tree displays service hierarchy and it shows the service level break-ups
	* You could then click on the Graph icon corresponding to that service which is taking more time
	* This opens up a modal window containing graphs like CPU, Response time, Threat Level CPU, etc
	* Click on the data point in one of the graphs to highlight the correlation line across all the graphs
	* These graphs help you map the response time with other performance parameters
		
### Report generation
After all the profiling is completed you could generate a report as follows:

	* Click on Options->Report
	* An HTML report will be generated
______________________
These tools are provided as-is and without warranty or support. They do not constitute part of the Software AG product suite. Users are free to use, fork and modify them, subject to the license agreement. While Software AG welcomes contributions, we cannot guarantee to include every contribution in the master project.	

Contact us at [TECHcommunity](mailto:technologycommunity@softwareag.com?subject=Github/SoftwareAG) if you have any questions.
