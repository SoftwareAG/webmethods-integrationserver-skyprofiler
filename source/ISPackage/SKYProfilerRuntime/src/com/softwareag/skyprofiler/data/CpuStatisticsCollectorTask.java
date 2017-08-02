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

package com.softwareag.skyprofiler.data;

import java.lang.management.ManagementFactory;
import java.util.TimerTask;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.softwareag.skyprofiler.model.ResourceStatisticsNode;
import com.softwareag.skyprofiler.model.ResourceType;
import com.softwareag.skyprofiler.utility.Utils;
import com.sun.management.OperatingSystemMXBean;

/**
 * This class is used to collect the CPU statistics of the machine.
 */
@SuppressWarnings("restriction")
public class CpuStatisticsCollectorTask extends TimerTask {
	private String topicName;

	private Producer<String, String> producer;

	// Bean for CPU usage monitoring
	private OperatingSystemMXBean osMXBean;

	public CpuStatisticsCollectorTask(Producer<String, String> producer, String topicName) {
		this.producer = producer;

		this.topicName = topicName;

		osMXBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	}

	@Override
	public void run() {
		producer.send(new ProducerRecord<String, String>(topicName, "SYSTEM_CPU", Utils
				.convertToJson(new ResourceStatisticsNode(ResourceType.SYSTEM_CPU_LOAD, osMXBean.getSystemCpuLoad()))));
		producer.send(new ProducerRecord<String, String>(topicName, "PROCESS_CPU", Utils.convertToJson(
				new ResourceStatisticsNode(ResourceType.PROCESS_CPU_LOAD, osMXBean.getProcessCpuLoad()))));
	}
}
