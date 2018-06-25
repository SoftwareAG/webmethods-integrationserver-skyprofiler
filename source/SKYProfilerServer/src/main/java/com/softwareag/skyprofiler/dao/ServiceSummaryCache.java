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
 
package com.softwareag.skyprofiler.dao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.mongodb.DBObject;
import com.softwareag.skyprofiler.model.ServiceSummary;

@Repository
public class ServiceSummaryCache {
	Map<String, Map<String, ServiceSummary>> serverServiceSummaryMap;

	public ServiceSummaryCache() {
		serverServiceSummaryMap = new ConcurrentHashMap<String, Map<String, ServiceSummary>>();
	}

	public void addServiceData(String serverName, DBObject serviceData) {
		String sN;
		String pN;
		String sT;
		double rT;
		sN = serviceData.get("sN").toString();
		rT = Double.parseDouble(serviceData.get("rT").toString());
		try {
			serverServiceSummaryMap.get(serverName).get(sN).addServiceData(rT);
		} catch (NullPointerException npe) {
			pN = serviceData.get("pN").toString();
			sT = serviceData.get("sT").toString();

			if (!serverServiceSummaryMap.containsKey(serverName)) {
				// Server not exist, add server
				Map<String, ServiceSummary> serviceSummaryMap = new ConcurrentHashMap<String, ServiceSummary>();
				serviceSummaryMap.put(sN, new ServiceSummary(pN, sN, sT, rT));
				serverServiceSummaryMap.put(serverName, serviceSummaryMap);
			} else {
				// Server exist but the service not exist
				serverServiceSummaryMap.get(serverName).put(sN, new ServiceSummary(pN, sN, sT, rT));
			}
		}
	}

	public void clearServiceData(String serverName) {
		if (serverServiceSummaryMap.containsKey(serverName)) {
			serverServiceSummaryMap.get(serverName).clear();
		}
	}

	public Map<String, Map<String, ServiceSummary>> getMap() {
		return serverServiceSummaryMap;
	}
}
