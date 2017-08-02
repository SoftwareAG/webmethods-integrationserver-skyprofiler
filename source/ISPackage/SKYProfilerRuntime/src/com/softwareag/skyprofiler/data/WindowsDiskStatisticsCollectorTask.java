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

import java.text.DecimalFormat;
import java.util.TimerTask;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.softwareag.skyprofiler.model.ResourceStatisticsNode;
import com.softwareag.skyprofiler.model.ResourceType;
import com.softwareag.skyprofiler.utility.OshiDiskStatsCollectorUtil;
import com.softwareag.skyprofiler.utility.Utils;

/**
 * This class helps in collecting the disk statics of Windows operating system.
 * 
 * @author YEJ
 */
public class WindowsDiskStatisticsCollectorTask extends TimerTask {
	private Producer<String, String> producer;

	private String topicName;

	private DecimalFormat decimalPrecision = new DecimalFormat(".##");

	public WindowsDiskStatisticsCollectorTask(Producer<String, String> producer, String topicName) {
		this.producer = producer;

		this.topicName = topicName;

		OshiDiskStatsCollectorUtil.init();
	}

	@Override
	public void run() {
		long dataInNano = OshiDiskStatsCollectorUtil.get();
		double dataInMillSec = dataInNano / 1000000.0;
		producer.send(new ProducerRecord<String, String>(topicName, "DISK", Utils.convertToJson(
				new ResourceStatisticsNode(ResourceType.DISK_LATENCY, decimalPrecision.format(dataInMillSec)))));
	}

	@Override
	public boolean cancel() {
		OshiDiskStatsCollectorUtil.close();
		return true;
	}
}
