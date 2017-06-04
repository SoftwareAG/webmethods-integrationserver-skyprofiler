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
package com.softwareag.skyprofiler.utility;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import com.google.gson.Gson;
import com.wm.app.b2b.server.ServerAPI;

/**
 * Class Name : Utils
 * 
 * Description : A utility class for the application. This class contains some
 * useful methods and constants that are used across the application.
 */
public class Utils {
	public static final String PACKAGE_NAME = "SKYProfiler";

	public static final String CONFIG_FILE_NAME = "config.properties";

	public static final String INCLUDED_PACKAGES_ATTR = "packages.included";

	public static final String KAFKA_BOOTSTRAP_URL_ATTR = "kafka.bootstrap.url";

	public static final String KAFKA_TOPIC_NAME_ATTR = "kafka.topic.name";

	public static final String EXTERNAL_HOSTNAME_ATTR = "external.hostname";

	private static Gson GSON;

	static {
		GSON = new Gson();
	}

	/**
	 * This utility method helps in converting a given object to json string.
	 * 
	 * @param input
	 * @return String - representing json
	 */
	public static String convertToJson(Object input) {
		return GSON.toJson(input);
	}

	/**
	 * This method helps in converting given input string containing comma
	 * separated package names to a Map.
	 * 
	 * @param includedPkgsStr
	 * @return Map
	 */
	public static Map<String, Object> parsePackageString(String includedPkgsStr) {
		Map<String, Object> includedPkgMap = new HashMap<>();

		if (includedPkgsStr != null && !includedPkgsStr.trim().isEmpty()) {
			String[] pkgNames = includedPkgsStr.split(",");
			for (String pkgName : pkgNames) {
				includedPkgMap.put(pkgName.trim(), null);
			}
		}

		return includedPkgMap;
	}

	/**
	 * This method helps in reading the config.properties file and creates
	 * properties object
	 * 
	 * @return Properties
	 */
	public static Properties readConfigToFile() {
		Properties props = new Properties();
		try {
			File file = ServerAPI.getPackageConfigDir(Utils.PACKAGE_NAME);
			File configFile = new File(file.getAbsolutePath() + File.separator + Utils.CONFIG_FILE_NAME);
			FileReader reader = new FileReader(configFile);
			props.load(reader);
			reader.close();
		} catch (IOException ex) {
			// file does not exist
		}

		return props;
	}

	/**
	 * This method will update the config.properties with the supplied data.
	 * 
	 * @param props
	 * @param includedPkgsStr
	 * @param kafkaBootstrapUrl
	 * @param kafkaTopicName
	 * @param externalHostName
	 * @throws IOException
	 */
	public static void writeConfigToFile(Properties props, String includedPkgsStr, String kafkaBootstrapUrl,
			String kafkaTopicName, String externalHostName) throws IOException {
		props.setProperty(Utils.INCLUDED_PACKAGES_ATTR, includedPkgsStr);
		props.setProperty(Utils.KAFKA_BOOTSTRAP_URL_ATTR, kafkaBootstrapUrl);
		props.setProperty(Utils.KAFKA_TOPIC_NAME_ATTR, kafkaTopicName);
		props.setProperty(Utils.EXTERNAL_HOSTNAME_ATTR, externalHostName);

		File file = ServerAPI.getPackageConfigDir(Utils.PACKAGE_NAME);
		File configFile = new File(file.getAbsolutePath() + File.separator + Utils.CONFIG_FILE_NAME);
		FileWriter writer = new FileWriter(configFile);
		props.store(writer, "SkyProfiler Config File");
		writer.close();
	}

	/**
	 * This method helps in creating kafka producer which will help in sending
	 * the monitored service data to the server via kafka
	 * 
	 * @param kafkaBootstrapUrl
	 * @return Producer
	 */
	public static Producer<String, String> createKafkaProducer(String kafkaBootstrapUrl) {
		// create instance for properties to access producer configs
		Properties props = new Properties();

		// Assign localhost id
		props.put("bootstrap.servers", kafkaBootstrapUrl);

		// Set acknowledgements for producer requests.
		props.put("acks", "all");

		// If the request fails, the producer can automatically retry,
		props.put("retries", 0);

		// Specify buffer size in config
		props.put("batch.size", 16384);

		// Reduce the no of requests less than 0
		props.put("linger.ms", 1);

		// The buffer.memory controls the total amount of memory available to
		// the producer for buffering.
		props.put("buffer.memory", 33554432);

		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		return new KafkaProducer<String, String>(props);
	}
}
