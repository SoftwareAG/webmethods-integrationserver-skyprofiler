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

package com.softwareag.skyprofiler.kafka;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import com.softwareag.skyprofiler.controller.GraphController;
import com.softwareag.skyprofiler.controller.TreeDataController;
import com.softwareag.skyprofiler.dao.ServiceSummaryCache;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import net.minidev.json.JSONArray;

public class MessageHandler implements Runnable {
	private KafkaStream<byte[], byte[]> kafkaStream;

	private String topicName;

	private MongoDatabase db;

	private ServiceSummaryCache serviceSummaryCache;

	private GraphController graphController;

	private SimpMessagingTemplate messagingTemplate;

	private TreeDataController treeController;

	private String graphDataWebSockName;

	private String resourceDataWebSockName;

	private Gson gson;

	public MessageHandler(KafkaStream<byte[], byte[]> kafkaStream, String topicName, MongoClient mongoDBClient,
			ServiceSummaryCache cache, GraphController graphController, SimpMessagingTemplate messagingTemplate,
			TreeDataController treeController) {
		this.kafkaStream = kafkaStream;
		this.topicName = topicName;
		this.serviceSummaryCache = cache;
		this.graphController = graphController;
		this.messagingTemplate = messagingTemplate;
		this.treeController = treeController;
		this.graphDataWebSockName = "/topic/" + topicName + "/graphData";
		this.resourceDataWebSockName = "/topic/" + topicName + "/resource";

		this.db = mongoDBClient.getDatabase(topicName);
		this.gson = new Gson();
	}

	public void run() {
		ConsumerIterator<byte[], byte[]> it = kafkaStream.iterator();

		byte[] messageData;

		String message;
		String sN;
		String eT;
		String result;
		Double rT;

		while (it.hasNext()) {
			messageData = it.next().message();
			message = new String(messageData);

			DBObject obj = (DBObject) JSON.parse(message);
			if (obj.get("sN") != null) {
				sN = obj.get("sN").toString();

				this.db.getCollection(sN, DBObject.class).insertOne(obj);

				this.serviceSummaryCache.addServiceData(topicName, obj);

				if (graphController.getSelectedServerName().equals(topicName)
						&& sN.equals(graphController.getSelectedServiceName())) {
					eT = obj.get("eT").toString();
					rT = Double.parseDouble(obj.get("rT").toString()) / 1000000;
					JsonObject jsonObject = new JsonObject();
					jsonObject.addProperty("eT", eT);
					jsonObject.addProperty("rT", rT);
					jsonObject.addProperty("index", obj.get("_id").toString());

					this.messagingTemplate.convertAndSend(graphDataWebSockName, jsonObject.toString());

					if (treeController.getSelectedServerName().equals(topicName)
							&& !treeController.getSelectedServiceJsonPath().isEmpty()) {
						Object selectedJsonObject = JsonPath.read(message, treeController.getSelectedServiceJsonPath());
						if (selectedJsonObject != null) {
							if (selectedJsonObject instanceof JSONArray) {
								result = gson.toJson(((JSONArray) selectedJsonObject).get(0));
							} else {
								result = gson.toJson(selectedJsonObject);
							}

							this.messagingTemplate.convertAndSend(resourceDataWebSockName, result);
						}
					}
				}
			} else {
				this.messagingTemplate.convertAndSend(resourceDataWebSockName, message);
			}
		}
	}
}
