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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.MongoClient;
import com.softwareag.skyprofiler.config.KafkaConsumerConfig;
import com.softwareag.skyprofiler.controller.GraphController;
import com.softwareag.skyprofiler.controller.TreeDataController;
import com.softwareag.skyprofiler.dao.ServiceSummaryCache;

@Component
public class TopicManager {
	@Autowired
	KafkaConsumerConfig consumerConfig;

	@Autowired
	ServiceSummaryCache cache;

	@Autowired
	MongoClient mongoDBClient;

	@Autowired
	GraphController graphController;

	@Autowired
	SimpMessagingTemplate messagingTemplate;

	@Autowired
	TreeDataController treeController;

	@Value("${kafka.client.consumerspertopic}")
	int noOfConsumersPerTopic = 1;

	private Map<String, TopicConsumer> topicConsumerMap;

	public TopicManager() {
		topicConsumerMap = new HashMap<String, TopicConsumer>();
	}

	public void addConsumer(String topicName) {
		TopicConsumer topicConsumer = new TopicConsumer(consumerConfig.consumerConfigs(), topicName,
				noOfConsumersPerTopic, mongoDBClient, cache, graphController, messagingTemplate, treeController);
		topicConsumer.startConsumer();

		topicConsumerMap.put(topicName, topicConsumer);
	}

	public void stopConsumer(String topicName) {
		if (topicConsumerMap.containsKey(topicName)) {
			TopicConsumer topicConsumer = topicConsumerMap.get(topicName);
			topicConsumer.stopConsumer();
		}
	}
}
