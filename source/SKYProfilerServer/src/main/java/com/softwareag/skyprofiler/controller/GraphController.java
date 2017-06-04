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
 
package com.softwareag.skyprofiler.controller;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class GraphController {
	private String selectedServerName = "";
	private String selectedServiceName = "";

	@MessageMapping("/graph")
	public void showGraph(String message) throws Exception {
		DBObject obj = (DBObject) JSON.parse(message);
		this.selectedServerName = obj.get("serverName").toString();
		this.selectedServiceName = obj.get("serviceName").toString();
	}

	public String getSelectedServerName() {
		return selectedServerName;
	}

	public String getSelectedServiceName() {
		return selectedServiceName;
	}
}
