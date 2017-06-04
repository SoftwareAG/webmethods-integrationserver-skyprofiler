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

package com.softwareag.skyprofiler.dao;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

@Repository
@SuppressWarnings("unchecked")
public class ServiceDataRepository {
	@Autowired
	MongoClient mongoDBClient;

	public void clearServerProfiledData(String serverName) {
		MongoDatabase database = mongoDBClient.getDatabase(serverName);
		for (String collectionName : database.listCollectionNames()) {
			database.getCollection(collectionName).drop();
		}
	}

	/**
	 * This method will help in creating array service summary of all the
	 * services belonging to a server.
	 * 
	 * @param serverName
	 *            : server for which service summary required
	 * 
	 * @return
	 */
	public String getServiceSummaryJson(String serverName) throws Exception {
		JSONArray serviceDataArray = new JSONArray();
		MongoDatabase serviceDatabase = mongoDBClient.getDatabase(serverName);
		MongoIterable<String> collectionNames = serviceDatabase.listCollectionNames();

		for (String collectionName : collectionNames) {
			MongoCollection<Document> collection = serviceDatabase.getCollection(collectionName);
			MongoIterable<Document> i = collection.find();
			MongoCursor<Document> cursor = i.iterator();

			Map<String, JSONObject> childServiceMap = new HashMap<String, JSONObject>();

			JSONObject mainServiceData = null;
			JSONArray graphDataArray = new JSONArray();

			long count = 0;

			double minRT = 0;
			double maxRT = 0;
			double totalRT = 0;

			while (cursor.hasNext()) {
				Document d = cursor.next();

				double rT = d.getDouble("rT");
				if (mainServiceData == null) {
					mainServiceData = new JSONObject();
					mainServiceData.put("sN", d.getString("sN"));
					mainServiceData.put("pN", d.getString("pN"));
					mainServiceData.put("sT", d.getString("sT"));

					minRT = rT;
					maxRT = rT;
					totalRT = rT;
				} else {
					minRT = minRT > rT ? rT : minRT;
					maxRT = maxRT < rT ? rT : maxRT;
					totalRT += rT;
				}

				count++;

				JSONArray graphDataPoint = new JSONArray();
				graphDataPoint.put(String.valueOf(d.get("eT")));
				graphDataPoint.put(rT / 1000000);
				graphDataArray.put(graphDataPoint);

				Object childObj = d.get("children");
				if (childObj != null && childObj instanceof List) {
					List<Document> children = (List<Document>) childObj;
					if (children.size() > 0) {
						recursiveChildProcess(childServiceMap, children);
					}
				}
			}

			if (mainServiceData != null) {
				mainServiceData.put("minRT", minRT);
				mainServiceData.put("maxRT", maxRT);
				mainServiceData.put("totalRT", totalRT);
				mainServiceData.put("avgRT", totalRT / count);
				mainServiceData.put("count", count);

				JSONArray childArray = new JSONArray();
				Iterator<JSONObject> ri = childServiceMap.values().iterator();
				while (ri.hasNext()) {
					childArray.put(ri.next());
				}

				mainServiceData.put("children", childArray);
				mainServiceData.put("graphData", graphDataArray);
			}

			serviceDataArray.put(mainServiceData);
		}

		return serviceDataArray.toString();
	}

	private void recursiveChildProcess(Map<String, JSONObject> childServiceMap, List<Document> childDocumentList) {
		for (Document childDocument : childDocumentList) {
			String sN = childDocument.getString("sN");
			String pN = childDocument.getString("pN");

			double rT = childDocument.getDouble("rT");

			if (childServiceMap.containsKey(sN + pN)) {
				JSONObject dataObj = childServiceMap.get(sN + pN);

				dataObj.put("rT", dataObj.getDouble("rT") + rT);
				dataObj.put("minRT", dataObj.getDouble("minRT") > rT ? rT : dataObj.getDouble("minRT"));
				dataObj.put("maxRT", dataObj.getDouble("maxRT") < rT ? rT : dataObj.getDouble("maxRT"));
				dataObj.put("totalRT", dataObj.getDouble("totalRT") + rT);
				dataObj.put("count", dataObj.getLong("count") + 1);
				dataObj.put("avgRT", dataObj.getDouble("totalRT") / dataObj.getLong("count"));
			} else {
				JSONObject childServiceData = new JSONObject();
				childServiceData.put("sN", sN);
				childServiceData.put("pN", pN);
				childServiceData.put("sT", childDocument.get("sT"));
				childServiceData.put("rT", rT);
				childServiceData.put("minRT", rT);
				childServiceData.put("maxRT", rT);
				childServiceData.put("avgRT", rT);
				childServiceData.put("totalRT", rT);
				childServiceData.put("count", 1);

				childServiceMap.put(sN + pN, childServiceData);
			}

			List<Document> children = (List<Document>) childDocument.get("children");
			if (children.size() > 0) {
				recursiveChildProcess(childServiceMap, children);
			}
		}
	}
}
