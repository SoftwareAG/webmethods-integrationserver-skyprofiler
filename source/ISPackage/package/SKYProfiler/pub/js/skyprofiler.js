/*
 * Copyright 2017 Software AG
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var app=angular.module('skyProfilerApp', ['ui-notification', 'ui.bootstrap', 'nya.bootstrap.select']);

app.config(['NotificationProvider', '$qProvider', function(NotificationProvider, $qProvider) {
    NotificationProvider.setOptions({
        delay: 1500,
        startTop: 20,
        startRight: 10,
        verticalSpacing: 10,
        horizontalSpacing: 10,
        positionX: 'right',
        positionY: 'bottom',
        closeOnClick: true,
        maxCount: 2
    });
	
	$qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('skyProfilerCtrl', ['$scope', '$http', '$location', '$modal', 'Notification', function($scope, $http, $location, $modal, Notification) {
    $scope.targetURL='';

    $scope.isRunning=false;

    $scope.init = function () {
        $scope.targetURL=$location.protocol()+'://'+$location.host()+":" + $location.port();
        $http.get($scope.targetURL+"/invoke/SKYProfiler.svc:isRunning")
        .then(function(response) {
            $scope.isRunning=response.data.status;
        }, function (error) {
			Notification.error({message: "Error while fetching the status of the server : " + error.data.$error});
		});	
    }

    $scope.doStart = function(item, event) {
        $http.get($scope.targetURL+"/invoke/SKYProfiler.svc:start")
        .then(function(response) {
            $scope.isRunning=true;
        }, function(error) {
            Notification.error({message: "Error while starting the profiler : " + error.data.$error});
        });
    };

    $scope.doStop = function(item, event) {
        $http.get($scope.targetURL+"/invoke/SKYProfiler.svc:stop")
        .then(function(response) {
            $scope.isRunning=false;
        }, function(error) {
            Notification.error({message: "Error while stopping the profiler : " + error.data.$error});
        });
    };

    $scope.openConfigModal = function() {
        $scope.modalInstance = $modal.open({
            templateUrl: 'configModal.html',
            controller: 'ModalInstanceCtrl',
            resolve: {
                targetURL: function() {
                    return $scope.targetURL;
                }
            }
        });

        $scope.modalInstance.opened.then(function() {
            $http.get($scope.targetURL+"/invoke/SKYProfiler.svc:getConfiguration")
            .then(function(response) {
                $scope.modalInstance.setConfigurationData(response.data, $scope.targetURL);
            }, function(error) {
                Notification.error({message: "Error while fetching the configuration information : " + error.data.$error });
            });
        }, null); 
    };  
}]);

app.controller('ModalInstanceCtrl', ['$scope', '$http', '$modalInstance', 'Notification', function($scope, $http, $modalInstance, Notification) {
    $scope.configurationData = {};

    $scope.targetURL = '';

    $scope.selectedPackageList=[];

    $modalInstance.setConfigurationData = function(configurationData, targetURL) {
        $scope.configurationData = configurationData;

        var packagesArr = $scope.configurationData.packages;
        var len = packagesArr.length;
        for (var i=0; i< len; i++) {
            if (packagesArr[i].selected === 'selected') {
                $scope.selectedPackageList.push(packagesArr[i].name);
            } 
        }

        $scope.targetURL = targetURL;
    };

    $scope.saveConfig = function() {
        var kafkaBootstrapUrl = angular.element('#kafkaBootstrapUrl').val();
        var kafkaTopicName = angular.element('#kafkaTopicName').val();
        var externalHostname = angular.element('#externalHostname').val();
        
        $http({
            url: $scope.targetURL+"/invoke/SKYProfiler.svc:updateSettings",
            dataType: "json",
            method: "POST",
            headers: {
                "Content-type": "application/json",
                "Accept": "*/*"
            },
            data: {"includedPackages" : $scope.selectedPackageList.join(), "kafkaBootstrapUrl" : kafkaBootstrapUrl, "kafkaTopicName" : kafkaTopicName, "externalHostname" : externalHostname }
        }).then(function(response){
            Notification.success(response.data.message);
        }, function(error){
            Notification.error(error.data.$error);
        });
        $modalInstance.close('close');
    };

    $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
    };
}]);
