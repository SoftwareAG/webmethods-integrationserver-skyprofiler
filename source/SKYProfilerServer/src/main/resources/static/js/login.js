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

(function () {
	"use strict";
	
	var app = angular.module('SkyProfilerApp', ['ui-notification']);

	app.config(function(NotificationProvider) {
    	NotificationProvider.setOptions({
    		delay : 5000,
    		startTop : 20,
    		startRight : 10,
    		verticalSpacing : 10,
    		horizontalSpacing : 10,
    		positionX : 'right',
    		positionY : 'bottom',
    		closeOnClick : true,
    		maxCount : 2
    	});
    });

	app.controller('LoginController', function($scope, $http, $window, Notification) {
		$scope.login = function (username, password) {
			$http.post('/login?username=' + username + '&password=' + password)
			.success(function (data, status, header, config) {
				Notification.success({
				    message : 'Login successful.<br/>Redirecting to home page...'
			    });
				$window.location.href = "/";
			})
			.error(function (data, status, header, config) {
				Notification.error({
				    message : 'Login failed !!!<br/>Verify username/password entered.'
			    });
			});	
		}
	});
}());
