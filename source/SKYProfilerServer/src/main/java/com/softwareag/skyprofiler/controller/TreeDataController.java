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

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.softwareag.skyprofiler.dao.ServiceSummaryCache;
import com.softwareag.skyprofiler.model.ServiceSummary;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.bson.Document;
import org.bson.types.ObjectId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TreeDataController {
	@Autowired(required = true)
	MongoClient mongoClient;

	@Autowired(required = true)
	ServiceSummaryCache cache;

	private String selectedServerName = "";

	private String selectedServiceJsonPath = "";

	@RequestMapping(value = "/tree", method = RequestMethod.POST)
	public String getSelectedServiceJson(HttpServletRequest request) throws Exception {
		Document resultServiceObj;

		String serverName = request.getParameter("serverName");
		String serviceName = request.getParameter("serviceName");
		String _id = request.getParameter("index");

		MongoDatabase db = mongoClient.getDatabase(serverName);
		MongoCollection<Document> collection = db.getCollection(serviceName);

		BasicDBObject query = new BasicDBObject();
		
		query.put("_id", new ObjectId(_id));
		resultServiceObj = (Document) collection.find(query).first();

		if (resultServiceObj == null) {
			return "";
		}

		return resultServiceObj.toJson();
	}

	@RequestMapping(value = "/baseline", method = RequestMethod.POST)
	public String takeBaseline(HttpServletRequest request) throws Exception {
		
		String serverName = request.getParameter("serverName");

		String result = "";
		Map<String, Map<String, ServiceSummary>> cacheMap = cache.getMap();
		if (serverName != null && serverName != null && cacheMap.containsKey(serverName)) {
			Map<String, ServiceSummary> serviceMap = cacheMap.get(serverName);
			for (ServiceSummary serviceSummary : serviceMap.values()) {
				serviceSummary.takeBaseline();
			}

			result = new Gson().toJson(serviceMap.values());
		} else {
			throw new Exception(serverName + " not found in the cache to take baseline.");
		}

		return result;
	}

	@RequestMapping(value = "/threshold", method = RequestMethod.POST)
	public void setThreshold(HttpServletRequest request) throws Exception {
		String serverName = request.getParameter("serverName");
		String serviceName = request.getParameter("serviceName");

		cache.getMap().get(serverName).get(serviceName)
				.setAlertThresholdPercentage(Integer.parseInt(request.getParameter("newThreshold")));
	}

	@RequestMapping(value = "/startCorrelation", method = RequestMethod.POST)
	public void startCorrelation(HttpServletRequest request) throws Exception {
		this.selectedServerName = request.getParameter("serverName");
		this.selectedServiceJsonPath = request.getParameter("selectedServiceJsonPath");
	}

	@MessageMapping("/stopCorrelation")
	public void stopCorrelation(HttpServletRequest request) throws Exception {
		this.selectedServerName = "";
		this.selectedServiceJsonPath = "";
	}

	public String getSelectedServerName() {
		return selectedServerName;
	}

	public String getSelectedServiceJsonPath() {
		return selectedServiceJsonPath;
	}
}
