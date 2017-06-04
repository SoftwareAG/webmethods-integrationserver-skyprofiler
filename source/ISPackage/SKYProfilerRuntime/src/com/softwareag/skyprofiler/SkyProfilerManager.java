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
package com.softwareag.skyprofiler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.Producer;

import com.softwareag.skyprofiler.data.ResourceStatisticsCollector;
import com.softwareag.skyprofiler.utility.Utils;
import com.wm.app.b2b.server.invoke.InvokeManager;

/**
 * Class Name : SkyProfilerManager 
 * Description : This is the main interface to the Integration Server SkyProfiler services.
 * This provides set of APIs which are easy to understand and helps the functionality.
 */
public class SkyProfilerManager {

	private static SkyProfilerManager INSTANCE;

	private Properties props;

	private boolean isRunning = false;

	private ServiceInvokeProcessor processor;

	private Map<String, Object> includedPkgsMap;

	private ResourceStatisticsCollector resourceStatisticsManager;

	/**
	 * Constructor marked private to restrict access from outside.
	 */
	private SkyProfilerManager() {
		props = Utils.readConfigToFile();

		if (props.containsKey(Utils.INCLUDED_PACKAGES_ATTR)) {
			includedPkgsMap = Utils.parsePackageString(props.getProperty(Utils.INCLUDED_PACKAGES_ATTR));
		} else {
			includedPkgsMap = new HashMap<>();
		}
	}

	/**
	 * The static method to access singleton object of this class
	 * 
	 * @return SkyProfilerManager instance
	 */
	public static SkyProfilerManager getInstance() {
		if (INSTANCE == null) {
			synchronized (SkyProfilerManager.class) {
				if (INSTANCE == null) {
					INSTANCE = new SkyProfilerManager();
				}
			}
		}

		return INSTANCE;
	}

	/**
	 * This method helps in updating the configuration information.
	 * 
	 * @param includedPkgsStr
	 * @param kafkaBootstrapUrl
	 * @param kafkaTopicName
	 * @param externalHostName
	 * 
	 * @exception IOException
	 */
	public void updateConfig(String includedPkgsStr, String kafkaBootstrapUrl, String kafkaTopicName,
			String externalHostName) throws IOException {
		String oldIncludedPkgsStr = props.getProperty(Utils.INCLUDED_PACKAGES_ATTR);
		String oldKafkaBootstrapUrl = props.getProperty(Utils.KAFKA_BOOTSTRAP_URL_ATTR);
		String oldKafkaTopicName = props.getProperty(Utils.KAFKA_TOPIC_NAME_ATTR);
		String oldExternalHostName = props.getProperty(Utils.EXTERNAL_HOSTNAME_ATTR);

		try {
			Utils.writeConfigToFile(props, includedPkgsStr, kafkaBootstrapUrl, kafkaTopicName, externalHostName);
			includedPkgsMap = Utils.parsePackageString(includedPkgsStr);
		} catch (IOException ex) {
			// Rolling back the new configuration data if at all saved
			Utils.writeConfigToFile(props, oldIncludedPkgsStr, oldKafkaBootstrapUrl, oldKafkaTopicName,
					oldExternalHostName);

			includedPkgsMap = Utils.parsePackageString(oldIncludedPkgsStr);

			throw ex;

		} finally {
			if (processor != null) {
				processor.setIncludedPkgMap(includedPkgsMap);
			}
		}
	}

	/**
	 * This method returns the package names which are selected for profiling.
	 * 
	 * @return Map - included packages map
	 */
	public Map<String, Object> getIncludedPackageNameMap() {
		return includedPkgsMap;
	}

	/**
	 * This method will returns the properties configured
	 * 
	 * @return Properties - configuration properties
	 */
	public Properties getConfigurationProperties() {
		return props;
	}

	/**
	 * This method needs to be called to start the profiler.
	 */
	public void startProfiler() {
		if (!isRunning) {

			Producer<String, String> producer = Utils
					.createKafkaProducer(props.getProperty(Utils.KAFKA_BOOTSTRAP_URL_ATTR));
			processor = new ServiceInvokeProcessor(producer, props.getProperty(Utils.KAFKA_TOPIC_NAME_ATTR),
					includedPkgsMap);

			InvokeManager manager = InvokeManager.getDefault();
			manager.registerProcessor(processor);
			resourceStatisticsManager = new ResourceStatisticsCollector(producer,
					props.getProperty(Utils.KAFKA_TOPIC_NAME_ATTR), props.getProperty(Utils.EXTERNAL_HOSTNAME_ATTR));

			isRunning = true;
		}
	}

	/**
	 * This method can be used to check the status of the profiler.
	 * 
	 * @return boolean : running or stopped status
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * This method can be used to stop profiling.
	 */
	public void stopProfiler() {
		if (isRunning) {
			InvokeManager manager = InvokeManager.getDefault();
			manager.unregisterProcessor(processor);

			if (resourceStatisticsManager != null) {
				resourceStatisticsManager.stop();
				resourceStatisticsManager = null;
			}

			isRunning = false;
		}
	}
}
