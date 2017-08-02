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

import java.util.TimerTask;

import org.apache.kafka.clients.producer.Producer;

/**
 * A Factory class that provides appropriate disk statistics collector timer
 * task based on the Operating System.
 * 
 * @author YEJ
 */
public class DiskStatisticsCollectorFactory {
	public static TimerTask getDiskCollectorTask(Producer<String, String> producer, String topicName) throws Exception {
		String osType = System.getProperty("os.name");

		if (osType.toUpperCase().startsWith("WINDOWS")) {
			return new WindowsDiskStatisticsCollectorTask(producer, topicName);
		} else if (osType.toUpperCase().startsWith("LINUX")) {
			return new LinuxDiskStatisticsCollectorTask(producer, topicName);
		}

		throw new Exception(osType + " not supported.");
	}
}
