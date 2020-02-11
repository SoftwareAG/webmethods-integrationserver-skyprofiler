/*
* Copyright © 2013 - 2018 Software AG, Darmstadt, Germany and/or its licensors
*
* SPDX-License-Identifier: Apache-2.0
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.                                                            
*
*/

(function () {
	
    "use strict";
    
    var app = angular.module('skyProfilerApp', [ 'ngRoute', 'angular-flot', 'ui.bootstrap', 'treeGrid', 'xeditable', 'ui-notification', 'ngSanitize', 'nya.bootstrap.select', 'ngMaterial' ]);

    app.config(['$routeProvider', '$locationProvider', 'NotificationProvider', function($routeProvider, $locationProvider, NotificationProvider) {
    	$locationProvider.html5Mode(true);
    	$locationProvider.hashPrefix('');
    	
    	$routeProvider
    		.when('/:serverId', {
    			templateUrl : 'views/server.html',
    			controller : 'serverCtrl',
    			controllerAs : 'server'
    		});

    	NotificationProvider.setOptions({
    		delay : 3000,
    		startTop : 20,
    		startRight : 10,
    		verticalSpacing : 10,
    		horizontalSpacing : 10,
    		positionX : 'right',
    		positionY : 'bottom',
    		closeOnClick : true,
    		maxCount : 2
    	});
    }]);

    app.factory('serverDataService', function() {
    	var connected = {};
    	var serverServiceDataMap = {}; // Holds table data

    	return {
    		setConnectionStatus : function(serverName, status) {
    			connected[serverName] = status;
    		},
    		getConnectionStatus : function(serverName) {
    			return connected[serverName];
    		},
    		setServiceData : function(serverName, serviceData) {
    			serverServiceDataMap[serverName] = serviceData;
    		},
    		getServiceData : function(serverName) {
    			return serverServiceDataMap[serverName];
    		}
    	};
    });

    app.controller('SkyProfilerCtrl', ['$scope', '$route', '$routeParams', '$location', '$http', '$window', 'serverDataService', '$mdDialog', 'Notification', function($scope, $route, $routeParams, $location, $http, $window, serverDataService, $mdDialog, Notification) {
    	init();

    	function init(){
    		$("#loader").hide();

    		$scope.menuTitle = "Servers";
    		
    		$scope.settings = {
    			close : true,
    			closeIcon : "fa fa-times"
    		};
    		
    		$scope.sideitems = [];

    		$scope.serverNames = [];

    		$http.get("/getAllServer")
    		.then(function(response) {
    		    $scope.serverNames = response.data;
    		    for (var i in response.data) {
    			    $scope.sideitems.push({
    				    name : response.data[i].serverName,
    				    link : response.data[i].serverName,
    				    icon : "fa fa-trash",
    				    target : ""
    			    });
    		    }
    		}, function(error) {
			    Notification.error({
				    message : 'Error while getting configured server list from server.'
			    });
    		});

    		$scope.erroritems = [];

    		$('#errorModal').hide();
    		$('#successModal').hide();
    	} 

    	$scope.beforeAddServer = function(serverName){
    		$scope.serverInfo = {};
    		$scope.serverInfo.title='Add';
    	}

    	$scope.saveServerData = function(actionType) {
    		$('#errorModal').hide();
    		$('#successModal').hide();
    		$("#loader").show();
    		
    		$scope.erroritems = [];

    		var restApi = "addServer";
    		if (actionType === 'Edit') {
    			restApi = "updateServer";
    		}

    		$http.post('/' + restApi + '?' + $.param($scope.serverInfo) )
    		.then(function(response) {
    			$('#successModal').show();
    			$("#loader").hide();
    			$window.location.href = "/";
    		}, function(error) {
				$scope.erroritems = JSON.parse(error.data.message);
    			$('#errorModal').show();
    			$("#loader").hide();
    		});
    	}
    	
    	$scope.beforeEdit = function(serverName){
    		$http.post('/getServerInformation?serverName=' + serverName)
    		.then(function(response) {
    			$scope.serverInfo = response.data;
    			$scope.serverInfo.title='Edit';
    		}, function(error) {
				Notification.error({
				    message : 'Error while retrieving server information from server : ' + error
			    });
    		});
    	}

    	$scope.openServer = function(serverName) {
    		$http.post('/selectedServer?serverName='+serverName)
    		.then(function(response) {
    			$scope.selectedServerName = serverName;
    			$window.location.href = "/#/" + serverName;
    		}, function(error) {
    			Notification.error({
    			    message : 'Error while launching the selected server : ' + error
    		    });
    		});
    	}
    	
    	$scope.removeServer = function(serverName) {
    		if (confirm("Are you sure you want to delete " + serverName + "?") == true) {
    			$http.post('/deleteServer?serverName='+serverName)
    			.then(function(response) {
    				$window.location.href = "/";
    			}, function(error) {
    				Notification.error({
        			    message : 'Error while deleting server : ' + error
        		    });
    			});
    		}
    	}

    	$scope.showAbout = function(about){
    		$mdDialog.show(
    		    $mdDialog.alert()
    			.parent(angular.element(document.querySelector('#popupContainer')))
    			.clickOutsideToClose(true)
    			.title('SKY Profiler About')
    			.htmlContent('SKY Profiler monitors Integration Server\'s service invocations. The monitored data can be seen real time.<br/> This helps users track the time taken for each service invocation and further drills down to the child service<br/>to identify which service contributes to time.<br/><br/>Version: 1.0.0<br/>Copyright 2017 Software AG<br/><br/>Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance<br/>with the License. You may obtain a copy of the License at<br/><br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</a><br/><br/>Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on<br/>an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See<br/>the License for the specific language governing permissions and limitations under the License.')
    			.ariaLabel('SKY Profiler About Dialog')
    			.ok('Got it!')
    			.targetEvent(about)
    	    );
    	};

    	$scope.logout = function() {
    		$http.post('/logout', {})
    		.then(function(response) {
    			$window.location.href = "/login";
    		}, function(error) {
    			Notification.error({
    			    message : 'Error while logging out : ' + error
    		    });
    		});
    	}

        $scope.downloadRuntime = function() {
            var fileToDownload = '/resources/SKYProfilerRuntime.zip';
            window.location = fileToDownload;
        }
    }]);

    app.controller('serverCtrl', ['$scope', '$routeParams', '$http', '$location', 'serverDataService', 'Notification', function serverCtrl($scope, $routeParams, $http, $location, serverDataService, Notification) {
    	var self = this;
    	
    	self.serverName = $routeParams.serverId;

    	self.panelWidth = $(window).width() * 0.38 + "px";

    	self.selectedServiceName;
    	
    	self.selectedPackageName;

    	var serviceItems = [];

    	self.serviceContainer = {};
    	
    	self.currentItems = serverDataService.getServiceData(self.serverName);

    	self.tree_data = [];

    	self.isRunning = false;

    	self.expanding_property = {
    		field : "sN",
    		displayName : "ServiceName"
    	};

    	self.col_defs = [
    		{
    			field : "pN",
    			displayName : "PackageName",
    			sortable : false,
    			filterable : true
    		},
    		{
    			field : "sT",
    			displayName : "ServiceType",
    			sortable : false,
    			filterable : true
    		},
    		{
    			field : "rT",
    			displayName : "ResponseTime(msec)",
    			cellTemplate : '<span> {{ row.branch[col.field]/1000000 }}<span>',
    			sortable : false,
    			filterable : true
    		},
    		{
    			field : "tCPU",
    			displayName : "ThreadLevel CpuTime(msec)",
    			cellTemplate : '<span>{{ row.branch[col.field]/1000000 }}<span>',
    			sortable : false,
    			filterable : true
    		},
    		{
    			field : "Correlation",
    			displayName : "Correlation",
    			cellTemplate : "<a data-toggle='modal' class='fa fa-random' data-target='#correlationModal' ng-click='cellTemplateScope.click(row)' ng-src='{{ row }}'></a>",
    			cellTemplateScope : {
    				click : function(data) {
    					self.correlationServiceSelected = data.branch.sN;
    					self.diskdataset[0].data = [];
    					self.networkdataset[0].data = [];
    					self.cpudataset[0].data = [];
    					self.cpudataset[1].data = [];
    					self.gcdataset[0].data = [];
    					self.gcdataset[1].data = [];
    					self.correlationServiceDataset[0].data = [];
    					self.threadcpudataset[0].data = [];

    					startCorrelation(data.branch.jsonPath);
    				}
    			}
    		} ];

    	var targetURL = $location.protocol() + '://' + $location.host() + ":" + $location.port();
    	$http.get(targetURL + "/isRunning?serverName=" + self.serverName)
    	.then(function(response) {
    		self.isRunning = response.data;
    	}, function(error) {
    		Notification.error({
			    message : 'Failed to get the server status from server : ' + error
		    });
    	});

    	self.doStart = function() {
    		$http.get('/start?serverName=' + self.serverName)
    		.then(function(response) {
    			self.isRunning = true;
    		}, function(error) {
   				Notification.error({
   				    message : 'Error while starting the server <br/>' + error.data.status + " : " + error.data.error + " - " + error.data.message
   			    });
   			});
    	}

    	self.doStop = function() {
    		$http.get('/stop?serverName=' + self.serverName)
    		.then(function(response) {
    			self.isRunning = false;
    		}, function(error) {
   				Notification.error({
   				    message : 'Error while stopping the server : ' + error
   			    })
   			});
    	}

    	if (serverDataService.getConnectionStatus(self.serverName) == undefined || serverDataService.getConnectionStatus(self.serverName) == false) {
    		var topic = "/topic/" + self.serverName + "/service";
    		var graphTopic = "/topic/" + self.serverName + "/graphData";
    		var resourceTopic = "/topic/" + self.serverName + "/resource";

    		var socket = new SockJS('/skyprofiler-websocket');
    		var stompClient = Stomp.over(socket);

    		var connectCallback = function() {
    			stompClient.subscribe(topic, getServiceData);
    			stompClient.subscribe(graphTopic, getGraphData);
    			stompClient.subscribe(resourceTopic, getResourceData);

    			serverDataService.setConnectionStatus(self.serverName, true);
    		};

    		var errorCallback = function(error) {
    			Notification.error({
   				    message : 'Error while establishing websocket connection with server : ' + error
   			    });
    			serverDataService.setConnectionStatus(self.serverName, false);
    			setTimeout(reconnect, 1000);
    		};

    		stompClient.connect({}, connectCallback, errorCallback);
    	}

    	function reconnect() {
    		var topic = "/topic/" + self.serverName + "/service";
    		var graphTopic = "/topic/" + self.serverName + "/graphData";
    		var resourceTopic = "/topic/" + self.serverName + "/resource";

    		var socket = new SockJS('/skyprofiler-websocket');
    		var stompClient = Stomp.over(socket);

    		var connectCallback = function() {
    			stompClient.subscribe(topic, getServiceData);
    			stompClient.subscribe(graphTopic, getGraphData);
    			stompClient.subscribe(resourceTopic, getResourceData);

    			serverDataService.setConnectionStatus(self.serverName, true);
    		};

    		var errorCallback = function(error) {
    			Notification.error({
   				    message : 'Error while establishing websocket connection with server : ' + error
   			    });
    			serverDataService.setConnectionStatus(self.serverName, false);
    			setTimeout(reconnect, 1000);
    		};

    		stompClient.connect({}, connectCallback, errorCallback);
    	}

    	function getServiceData(message) {
    		var serviceData = JSON.parse(message.body);

    		for (var data in serviceData) {
    			if (self.serviceContainer[serviceData[data].sN] == undefined) {
    				self.serviceContainer[serviceData[data].sN] = {};
    				self.serviceContainer[serviceData[data].sN].count = serviceData[data].count;
    				self.serviceContainer[serviceData[data].sN].pN = serviceData[data].pN;
    				self.serviceContainer[serviceData[data].sN].sN = serviceData[data].sN;
    				self.serviceContainer[serviceData[data].sN].sT = serviceData[data].sT;
    				self.serviceContainer[serviceData[data].sN].violations = serviceData[data].violations;
    				self.serviceContainer[serviceData[data].sN].totalRT = serviceData[data].totalRT;
    				self.serviceContainer[serviceData[data].sN].alertThreshold = serviceData[data].alertThresholdPercentage;
    				self.serviceContainer[serviceData[data].sN].drift = serviceData[data].drift;
    				serviceItems.push(self.serviceContainer[serviceData[data].sN]);
    			} else {
    				self.serviceContainer[serviceData[data].sN].count = serviceData[data].count;
    				self.serviceContainer[serviceData[data].sN].violations = serviceData[data].violations;
    				self.serviceContainer[serviceData[data].sN].totalRT = serviceData[data].totalRT;
    				self.serviceContainer[serviceData[data].sN].drift = serviceData[data].drift;

    				if (serviceData[data].baselineRT != null) {
    					self.serviceContainer[serviceData[data].sN].baselineRT = serviceData[data].baselineRT;
    					self.isBaselineAvailable = true;
    				}
    			}

    			serverDataService.setServiceData(self.serverName, serviceItems);
    			self.currentItems = serverDataService.getServiceData(self.serverName);
    			$scope.$apply(function() {
    				self.currentItems = serverDataService.getServiceData(self.serverName);
    			});
    		}
    	}

    	function getGraphData(message) {
    		var graphData = JSON.parse(message.body);

    		var graphArray = [];
    		graphArray.push(graphData.eT);
    		graphArray.push(graphData.rT);
    		graphArray.push(graphData.index);

    		if (self.mainServiceGraphDataset[0].data.length > 2000) {
    			self.mainServiceGraphDataset[0].data.shift();
    		}

    		self.mainServiceGraphDataset[0].data.push(graphArray);
    	}

    	function getResourceData(message) {
    		var jsonResourceData = JSON.parse(message.body);

    		if (jsonResourceData.type == "DISK_LATENCY") {
    			var diskArray = [];
    			diskArray.push(jsonResourceData.eT);
    			diskArray.push(jsonResourceData.value);
    			self.diskdataset[0].data.push(diskArray);
    		}

    		if (jsonResourceData.type == "NETWORK_LATENCY") {
    			var networkArray = [];
    			networkArray.push(jsonResourceData.eT);
    			networkArray.push(jsonResourceData.value);
    			self.networkdataset[0].data.push(networkArray);
    		}

    		if (jsonResourceData.type == "SYSTEM_CPU_LOAD") {
    			var systemArray = [];
    			systemArray.push(jsonResourceData.eT);
    			systemArray.push(jsonResourceData.value * 100);
    			self.cpudataset[0].data.push(systemArray);
    		}

    		if (jsonResourceData.type == "PROCESS_CPU_LOAD") {
    			var processArray = [];
    			processArray.push(jsonResourceData.eT);
    			processArray.push(jsonResourceData.value * 100);
    			self.cpudataset[1].data.push(processArray);
    		}


    		if (jsonResourceData.type == "GC_MAJOR") {
    			var major = [];
    			major.push(jsonResourceData.eT);
    			major.push(jsonResourceData.value);
    			self.gcdataset[0].data.push(major);
    		} else {
    			var major = [];
    			major.push(jsonResourceData.eT);
    			major.push(0);
    			self.gcdataset[0].data.push(major);
    		}

    		if (jsonResourceData.type == "GC_MINOR") {
    			var minor = [];
    			minor.push(jsonResourceData.eT);
    			minor.push(jsonResourceData.value);
    			self.gcdataset[1].data.push(minor);
    		} else {
    			var minor = [];
    			minor.push(jsonResourceData.eT);
    			minor.push(0);
    			self.gcdataset[1].data.push(minor);
    		}

    		if (jsonResourceData.sN != undefined) {
    			var correlationData = [];
    			var threadCPU = [];

    			correlationData.push(jsonResourceData.eT);
    			correlationData.push(jsonResourceData.rT / 1000000);

    			threadCPU.push(jsonResourceData.eT);
    			threadCPU.push(jsonResourceData.tCPU / 1000000);

    			self.correlationServiceDataset[0].data.push(correlationData);
    			self.threadcpudataset[0].data.push(threadCPU);
    		} else {
    			var correlationData = [];
    			var threadCPU = [];

    			correlationData.push(jsonResourceData.eT);
    			correlationData.push(0);

    			threadCPU.push(jsonResourceData.eT);
    			threadCPU.push(0);

    			self.correlationServiceDataset[0].data.push(correlationData);
    			self.threadcpudataset[0].data.push(threadCPU);
    		}

    	}

    	function startCorrelation(selectedServiceJsonPath) {
    		$http.post('/startCorrelation?' +  $.param({serverName : self.serverName,selectedServiceJsonPath : selectedServiceJsonPath}))
    		.then(function(response) {
    			Notification.success({
   				    message : 'Correlation started successfully'
   			    });
    		}, function(error) {
    			Notification.error({
   				    message : 'Failed to start the correlation : ' + error
   			    });
    		});
    	}

    	self.generateReport = function(serverName) {
    		$("#loader").show();

    		$http.get('/report/' + serverName)
    		.then(function(response) {
    			var file = new Blob([ response.data ], {
    				type : 'application/html'
    			});
    			saveAs(file, 'SkyProfiler_Report_' + serverName + '.html');
    			$("#loader").hide();
    		}, function(error) {
    			$("#loader").hide();
    			Notification.error({
   				    message : 'Error while generating report : ' + error.data.message
   			    });
    		});
    	}

    	$('#correlationModal').on('hidden.bs.modal', function(e) {
    		var clearURL = $location.protocol() + '://' + $location.host() + ":" + $location.port();
    		$http.get(clearURL + "/stopCorrelation?serverName=" + self.serverName)
    		.then(function(response) {
    			console.log("Success")
    		}, function(error) {
    			console.log("Error");
    		});
    	});

    	self.xeditOpen = function($event, elementOpened) {
    		$event.preventDefault();
    		$event.stopPropagation();
    	};

    	self.clearProfiledData = function(serverName) {
    		$http.post('/clearProfiledData?serverName=' + serverName)
    		.then(function(response) {
    			self.selectedServiceName = '';
    			self.selectedPackageName = '';
    			self.tree_data = [];
    			self.serviceContainer = {};
    			serverDataService.setServiceData(self.serverName, []);
    			self.currentItems = [];
    			serviceItems = [];
    			self.isBaselineAvailable = false;

    			self.mainServiceGraphOptions.grid.markings[0].color = "#FFFFFF";
    			self.mainServiceGraphOptions.grid.markings[0].yaxis.from = 0;
    			self.mainServiceGraphOptions.grid.markings[0].yaxis.to = 0;

    			self.mainServiceGraphOptions.grid.markings[1].color = "#FFFFFF";
    			self.mainServiceGraphOptions.grid.markings[1].yaxis.from = 0;
    			self.mainServiceGraphOptions.grid.markings[1].yaxis.to = 0;

    			Notification.success({
    				message : 'Profiled data of ' + serverName + ' was deleted successfully.'
    			});
    		}, function(error) {
    			Notification.error({
    				message : 'Failed while clearing Profiled data of ' + serverName + '.'
    			});
    		});
    	}

    	self.changeServiceName = function(serviceName, packageName) {
    		if (self.selectedServiceName != serviceName) {
    			self.selectedServiceName = serviceName;
    			self.selectedPackageName = packageName;
    			
    			self.mainServiceGraphDataset[0].label = serviceName;
    			self.mainServiceGraphDataset[0].data = [];

    			if (self.serviceContainer[serviceName].baselineRT != undefined) {
    				self.mainServiceGraphOptions.grid.markings[0].color = "green";
    				self.mainServiceGraphOptions.grid.markings[0].yaxis.from = self.serviceContainer[serviceName].baselineRT;
    				self.mainServiceGraphOptions.grid.markings[0].yaxis.to = self.serviceContainer[serviceName].baselineRT;

    				self.mainServiceGraphOptions.grid.markings[1].color = "red";
    				self.mainServiceGraphOptions.grid.markings[1].yaxis.from = (self.serviceContainer[serviceName].baselineRT + (self.serviceContainer[serviceName].baselineRT * (self.serviceContainer[serviceName].alertThreshold / 100)));
    				self.mainServiceGraphOptions.grid.markings[1].yaxis.to = (self.serviceContainer[serviceName].baselineRT + (self.serviceContainer[serviceName].baselineRT * (self.serviceContainer[serviceName].alertThreshold / 100)));
    			} else {
    				self.mainServiceGraphOptions.grid.markings[0].color = "#FFFFFF";
    				self.mainServiceGraphOptions.grid.markings[0].yaxis.from = 0;
    				self.mainServiceGraphOptions.grid.markings[0].yaxis.to = 0;

    				self.mainServiceGraphOptions.grid.markings[1].color = "#FFFFFF";
    				self.mainServiceGraphOptions.grid.markings[1].yaxis.from = 0;
    				self.mainServiceGraphOptions.grid.markings[1].yaxis.to = 0;
    			}

    			self.tree_data = [];

    			var socketTwo = new SockJS('/skyprofiler-websocket');
    			var stompClientTwo = Stomp.over(socketTwo);
    			stompClientTwo.connect({}, function(frame) {
    				stompClientTwo.send("/app/graph", {}, JSON.stringify({
    					'serverName' : self.serverName,
    					'serviceName' : serviceName
    				}));
    			});

    			self.correlationServiceSelected = "";
    		}
    	}

    	self.configurationData = {};
    	self.selectedPackageList = [];
    	self.beforeConfigure = function(serverName) {
    		$http.post('/getServerConfiguration?serverName=' + serverName)
    		.then(function(response) {
    			self.configurationData = response.data;
    			var packagesArr = self.configurationData.packages;
    			var len = packagesArr.length;
    			for (var i = 0; i < len; i++) {
    				if (packagesArr[i].selected === 'selected') {
    					self.selectedPackageList.push(packagesArr[i].name);
    				}
    			}
    		}, function(error) {
    			Notification.error({
					message : 'Failed while fetching server configuration.'
				});
    		});
    	}

    	self.saveConfig = function(serverName) {
    		var kafkaBootstrapUrl = angular.element('#kafkaBootstrapUrl').val();
    		var kafkaTopicName = angular.element('#kafkaTopicName').val();
    		var externalHostname = angular.element('#externalHostname').val();

    		var configData = $.param({
    			serverName : serverName,
    			includedPackages : self.selectedPackageList.join(),
    			kafkaBootstrapUrl : kafkaBootstrapUrl,
    			kafkaTopicName : kafkaTopicName,
    			externalHostname : externalHostname
    		});

    		$http.post('/saveServerConfiguration?' + configData )
    		.then(function(response) {
    			Notification.success({
    				message : 'Configuration of ' + serverName + ' server saved successfully.'
    			});
    		}, function(error) {
    			Notification.error({
    				message : 'Failed while saving the configuration of the server ' + serverName + '.'
    			});
    		});
    	};

    	self.takeBaseline = function(serverName) {
    		$http.post('/baseline?serverName=' + serverName)
    		.then(function(response) {
    			var serviceData = response.data;
    			for (var data in serviceData) {
    				if (self.serviceContainer[serviceData[data].sN] == undefined) {
    					self.serviceContainer[serviceData[data].sN] = {};
    					self.serviceContainer[serviceData[data].sN].count = serviceData[data].count;
    					self.serviceContainer[serviceData[data].sN].pN = serviceData[data].pN;
    					self.serviceContainer[serviceData[data].sN].sN = serviceData[data].sN;
    					self.serviceContainer[serviceData[data].sN].sT = serviceData[data].sT;
    					self.serviceContainer[serviceData[data].sN].violations = serviceData[data].violations;
    					self.serviceContainer[serviceData[data].sN].totalRT = serviceData[data].totalRT;
    					self.serviceContainer[serviceData[data].sN].alertThreshold = serviceData[data].alertThresholdPercentage;
    					self.serviceContainer[serviceData[data].sN].drift = serviceData[data].drift;
    					serviceItems.push(self.serviceContainer[serviceData[data].sN]);
    				} else {
    					self.serviceContainer[serviceData[data].sN].count = serviceData[data].count;
    					self.serviceContainer[serviceData[data].sN].violations = serviceData[data].violations;
    					self.serviceContainer[serviceData[data].sN].totalRT = serviceData[data].totalRT;
    					self.serviceContainer[serviceData[data].sN].drift = serviceData[data].drift;
    				}

    				if (serviceData[data].baselineRT != null) {
    					self.serviceContainer[serviceData[data].sN].baselineRT = serviceData[data].baselineRT;
    					self.isBaselineAvailable = true;
 
    					if (serviceData[data].sN === self.selectedServiceName) {
    						self.mainServiceGraphOptions.grid.markings[0].color = "green";
    						self.mainServiceGraphOptions.grid.markings[0].yaxis.from = serviceData[data].baselineRT;
    						self.mainServiceGraphOptions.grid.markings[0].yaxis.to = serviceData[data].baselineRT;

    						self.mainServiceGraphOptions.grid.markings[1].color = "red";
    						self.mainServiceGraphOptions.grid.markings[1].yaxis.from = (self.serviceContainer[serviceData[data].sN].baselineRT + (self.serviceContainer[serviceData[data].sN].baselineRT * (self.serviceContainer[serviceData[data].sN].alertThreshold / 100)));
    						self.mainServiceGraphOptions.grid.markings[1].yaxis.to = (self.serviceContainer[serviceData[data].sN].baselineRT + (self.serviceContainer[serviceData[data].sN].baselineRT * (self.serviceContainer[serviceData[data].sN].alertThreshold / 100)));
    					}
    				}

    				serverDataService.setServiceData(self.serverName, serviceItems);
    				self.currentItems = serverDataService.getServiceData(self.serverName);
    			}

    			Notification.success({
    				message : 'Baseline taken successfully.'
    			});
    		}, function(error) {
    			Notification.error({
    				message : 'Error while taking baseline.'
    			});
    		});
    	}

    	self.showDataPointServiceInfo = function(item, selectedServerName) {
    		if (item != null) {
    			var configData = $.param({
    				serverName : selectedServerName,
    				serviceName : self.selectedServiceName,
    				index : item.series.data[item.dataIndex][2]
    			});

    			$http.post('/tree?' + configData)
    			.then(function(response) {
    				var jsonTreeData = {
    					"sN" : response.data.sN,
    					"sT" : response.data.sT,
    					"rT" : response.data.rT,
    					"tCPU" : response.data.tCPU,
    					"pN" : response.data.pN,
    					"children" : response.data.children
    				};
    				self.tree_data = [];
    				self.tree_data.push(jsonTreeData);
    			}, function(error) {
    				Notification.error({
        				message : 'Error while fetching tree data.'
        			});
    			});
    		}
    	}

    	self.validateThresholdValue = function(data) {
    		if (data < 0) {
    			Notification.error({
    				message : 'Threshold cannot be a negative number.'
    			});
    		}
    		return data > -1;
    	}

    	self.thresholdChanged = function(data, serverName, serviceName) {
    		var newThresholdValue = data;
    		var configData = $.param({
    			serverName : serverName,
    			serviceName : serviceName,
    			newThreshold : newThresholdValue
    		});

    		$http.post('/threshold?' + configData)
    		.then(function(response) {
    			if (serviceName === self.selectedServiceName && self.serviceContainer[serviceName].baselineRT != undefined) {
    				self.mainServiceGraphOptions.grid.markings[0].color = "green";
    				self.mainServiceGraphOptions.grid.markings[0].yaxis.from = self.serviceContainer[serviceName].baselineRT;
    				self.mainServiceGraphOptions.grid.markings[0].yaxis.to = self.serviceContainer[serviceName].baselineRT;

    				self.mainServiceGraphOptions.grid.markings[1].color = "red";
    				self.mainServiceGraphOptions.grid.markings[1].yaxis.from = (self.serviceContainer[serviceName].baselineRT + (self.serviceContainer[serviceName].baselineRT * (newThresholdValue / 100)));
    				self.mainServiceGraphOptions.grid.markings[1].yaxis.to = (self.serviceContainer[serviceName].baselineRT + (self.serviceContainer[serviceName].baselineRT * (newThresholdValue / 100)));
    			}
    			Notification.success({
    				message : 'Threshold adjusted successfully.'
    			});
    		}, function(error) {
    			Notification.error({
    				message : 'Error while setting the threshold value.'
    			});
    		});
    	}

    	self.drawMarking = function(event, pos, item) {
    		var yMarking = self.correlationServiceOptions.grid.markings[0];

    		self.correlationServiceOptions.grid.markings = [];
    		self.correlationServiceOptions.grid.markings.push(yMarking);

    		self.threadcpuoptions.grid.markings = [];
    		self.cpuoptions.grid.markings = [];
    		self.gcoptions.grid.markings = [];
    		self.diskoptions.grid.markings = [];
    		self.networkoptions.grid.markings = [];

    		self.correlationServiceOptions.grid.markings.push({
    			xaxis : {
    				from : item.datapoint[0],
    				to : item.datapoint[0]
    			},
    			color : "#ff8888"
    		});
    		self.threadcpuoptions.grid.markings.push({
    			xaxis : {
    				from : item.datapoint[0],
    				to : item.datapoint[0]
    			},
    			color : "#ff8888"
    		});
    		self.cpuoptions.grid.markings.push({
    			xaxis : {
    				from : item.datapoint[0],
    				to : item.datapoint[0]
    			},
    			color : "#ff8888"
    		});
    		self.gcoptions.grid.markings.push({
    			xaxis : {
    				from : item.datapoint[0],
    				to : item.datapoint[0]
    			},
    			color : "#ff8888"
    		});
    		self.diskoptions.grid.markings.push({
    			xaxis : {
    				from : item.datapoint[0],
    				to : item.datapoint[0]
    			},
    			color : "#ff8888"
    		});
    		self.networkoptions.grid.markings.push({
    			xaxis : {
    				from : item.datapoint[0],
    				to : item.datapoint[0]
    			},
    			color : "#ff8888"
    		});
    	}


    	// ****************** //
    	// MAIN GRAPH OPTIONS //
    	// ****************** //
    	self.mainServiceGraphDataset = [ {
    		color : '#333',
    		data : [],
    		yaxis : 1,
    		label : ''
    	} ];
    	self.mainServiceGraphOptions = {
    		legend : {
    			show : false
    		},
    		xaxis : {
    			mode : "time",
    			timeformat : "%H:%M:%S",
    			timezone : "browser"
    		},
    		yaxis : {
    			min : 0
    		},
    		series : {
    			lines : {
    				show : true,
    				lineWidth : 1
    			},
    			points : {
    				show : false
    			}
    		},
    		zoom : {
    			interactive : true,
    			mode : "xy"
    		},
    		pan : {
    			interactive : true
    		},

    		grid : {
    			borderWidth : 0,
    			hoverable : true,
    			clickable : true,
    			markings : [ {
    				color : '#FFFFFF',
    				lineWidth : 1,
    				yaxis : {
    					from : 0,
    					to : 0
    				}
    			},
    				{
    					color : '#FFFFFF',
    					lineWidth : 1,
    					yaxis : {
    						from : 0,
    						to : 0
    					}
    				} ]
    		}
    	};

    	// ****************** //
    	// CORRELATION GRAPH  //
    	// ****************** //
    	self.correlationServiceDataset = [ {
    		color : '#0C3B60',
    		data : [],
    		yaxis : 1,
    		label : ''
    	} ];
    	self.correlationServiceOptions = {
    		legend : {
    			show : false
    		},
    		xaxis : {
    			mode : "time",
    			timeformat : "%H:%M:%S",
    			timezone : "browser"
    		},
    		yaxis : {
    			min : 0
    		},
    		series : {
    			lines : {
    				show : true,
    				lineWidth : 1
    			},
    			points : {
    				show : false
    			}
    		},
    		zoom : {
    			interactive : true,
    			mode : "xy"
    		},
    		pan : {
    			interactive : true
    		},
    		grid : {
    			borderWidth : 0,
    			hoverable : true,
    			clickable : true,
    			markings : [ {
    				yaxis : {
    					from : 0,
    					to : 0
    				},
    				color : '#FFFFFF',
    				lineWidth : 1
    			} ]
    		}
    	};

    	self.threadcpudataset = [ {
    		color : "#800000",
    		data : [],
    		yaxis : 1,
    		label : ''
    	} ];
    	self.threadcpuoptions = {
    		legend : {
    			show : false
    		},
    		xaxis : {
    			mode : "time",
    			timeformat : "%H:%M:%S",
    			timezone : "browser"
    		},
    		yaxis : {
    			min : 0
    		},
    		series : {
    			lines : {
    				show : true,
    				lineWidth : 1
    			},
    			points : {
    				show : false
    			}
    		},
    		zoom : {
    			interactive : true,
    			mode : "xy"
    		},
    		pan : {
    			interactive : true
    		},

    		grid : {
    			borderWidth : 0,
    			hoverable : true,
    			clickable : true,
    			grid : {
    				markings : []
    			}
    		}
    	};

    	self.cpudataset = [ {
    		color : "#8B008B",
    		data : [],
    		yaxis : 1,
    		label : 'SYSTEM-CPU'
    	}, {
    		color : "#FFD700",
    		data : [],
    		yaxis : 1,
    		label : 'PROCESS-CPU'
    	} ];
    	self.cpuoptions = {
    		xaxis : {
    			mode : "time",
    			timeformat : "%H:%M:%S",
    			timezone : "browser"
    		},
    		yaxis : {
    			min : 0,
    			max : 100,
    			tickSize : 25
    		},
    		legend : {
    			show : true
    		},
    		series : {
    			lines : {
    				show : true,
    				lineWidth : 1
    			},
    			points : {
    				show : false
    			}
    		},
    		zoom : {
    			interactive : true,
    			mode : "xy"
    		},
    		pan : {
    			interactive : true
    		},
    		grid : {
    			borderWidth : 0,
    			hoverable : true,
    			clickable : true,
    			grid : {
    				markings : []
    			}
    		}
    	};

    	self.diskdataset = [ {
    		color : "#8B008B",
    		data : [],
    		yaxis : 1,
    		label : 'DISK LATENCY'
    	} ];
    	self.diskoptions = {
    		xaxis : {
    			mode : "time",
    			timeformat : "%H:%M:%S",
    			timezone : "browser"
    		},
    		yaxis : {
    			min : 0
    		},
    		legend : {
    			show : false
    		},
    		series : {
    			lines : {
    				show : true,
    				lineWidth : 1
    			},
    			points : {
    				show : false
    			}
    		},
    		zoom : {
    			interactive : true,
    			mode : "xy"
    		},
    		pan : {
    			interactive : true
    		},

    		grid : {
    			borderWidth : 0,
    			hoverable : true,
    			clickable : true,
    			grid : {
    				markings : []
    			}
    		}
    	};

    	self.networkdataset = [ {
    		color : "#0C3B60",
    		data : [],
    		yaxis : 1,
    		label : 'NETWORK LATENCY'
    	} ];
    	self.networkoptions = {
    		xaxis : {
    			mode : "time",
    			timeformat : "%H:%M:%S",
    			timezone : "browser"
    		},
    		yaxis : {
    			min : 0
    		},
    		legend : {
    			show : false
    		},
    		series : {
    			lines : {
    				show : true,
    				lineWidth : 1
    			},
    			points : {
    				show : false
    			}
    		},
    		zoom : {
    			interactive : true,
    			mode : "xy"
    		},
    		pan : {
    			interactive : true
    		},

    		grid : {
    			borderWidth : 0,
    			hoverable : true,
    			clickable : true,
    			grid : {
    				markings : []
    			}
    		}
    	};

    	self.gcdataset = [ {
    		color : "#0000FF",
    		data : [],
    		yaxis : 1,
    		label : 'MAJOR-GC'
    	}, {
    		color : "#cc0099",
    		data : [],
    		yaxis : 1,
    		label : 'MINOR-GC'
    	} ];
    	self.gcoptions = {
    		xaxis : {
    			mode : "time",
    			timeformat : "%H:%M:%S",
    			timezone : "browser"
    		},
    		yaxis : {
    			min : 0
    		},
    		legend : {
    			// container: '#legend',
    			show : true
    		},
    		series : {
    			lines : {
    				show : true,
    				lineWidth : 1
    			},
    			points : {
    				show : false
    			}
    		},
    		zoom : {
    			interactive : true,
    			mode : "xy"
    		},
    		pan : {
    			interactive : true
    		},
    		grid : {
    			borderWidth : 0,
    			hoverable : true,
    			clickable : true,
    			grid : {
    				markings : []
    			}
    		}
    	};
    }]);
}());
