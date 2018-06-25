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

package com.softwareag.skyprofiler.kafka;

import static kafka.consumer.Consumer.createJavaConsumerConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.mongodb.MongoClient;
import com.softwareag.skyprofiler.controller.GraphController;
import com.softwareag.skyprofiler.controller.TreeDataController;
import com.softwareag.skyprofiler.dao.ServiceSummaryCache;

import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;

public class TopicConsumer {
	private Logger logger = LoggerFactory.getLogger(TopicConsumer.class);

	private String topicName;

	private int numOfPartitions = 1;

	private ConsumerConnector consumer;

	private ExecutorService threadPool;

	private MongoClient mongoDBClient;

	private ServiceSummaryCache cache;

	private GraphController graphController;

	private SimpMessagingTemplate messagingTemplate;

	private TreeDataController treeController;

	public TopicConsumer(Properties props, String topicName, int numOfPartitions, MongoClient mongoDBClient,
			ServiceSummaryCache cache, GraphController graphController, SimpMessagingTemplate messagingTemplate,
			TreeDataController treeController) {
		this.topicName = topicName;
		this.numOfPartitions = numOfPartitions;
		this.threadPool = Executors.newFixedThreadPool(numOfPartitions);
		this.consumer = createJavaConsumerConnector(new ConsumerConfig(props));
		this.mongoDBClient = mongoDBClient;
		this.cache = cache;
		this.graphController = graphController;
		this.messagingTemplate = messagingTemplate;
		this.treeController = treeController;
	}

	public void startConsumer() {
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		topicCountMap.put(topicName, numOfPartitions);
		Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
		List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topicName);

		for (final KafkaStream<byte[], byte[]> stream : streams) {
			threadPool.submit(new MessageHandler(stream, topicName, mongoDBClient, cache, graphController,
					messagingTemplate, treeController));
		}
	}

	public void stopConsumer() {
		if (consumer != null) {
			consumer.shutdown();
		}

		if (threadPool != null) {
			threadPool.shutdown();
		}

		try {
			if (!threadPool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
				logger.error("Timed out waiting for consumer threads to shut down, exiting uncleanly");
			}
		} catch (InterruptedException e) {
			logger.error("Interrupted during consumer shutdown, exiting uncleanly");
		}
	}
}
