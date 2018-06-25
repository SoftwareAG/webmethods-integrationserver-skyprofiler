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

package com.softwareag.skyprofiler.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.TimerTask;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.softwareag.skyprofiler.model.ResourceStatisticsNode;
import com.softwareag.skyprofiler.model.ResourceType;
import com.softwareag.skyprofiler.utility.Utils;

/**
 * This class helps in collecting the disk statics of Linux operating system.
 * 
 * @author YEJ
 */
public class LinuxDiskStatisticsCollectorTask extends TimerTask {
	private static String deviceName = "sda";

	private String topicName;

	private Producer<String, String> producer;

	private DecimalFormat decimalPrecision = new DecimalFormat(".###");

	// Delay
	private int sleepDuration = 1 * 1000;

	public LinuxDiskStatisticsCollectorTask(Producer<String, String> producer, String topicName) {
		this.producer = producer;

		this.topicName = topicName;
	}

	@Override
	public void run() {
		BufferedReader bufferedReader = null;

		try {
			double diskValue1 = 0;

			// Counter to match nth line which contains device string "sda"
			int matchCounter = 0;

			bufferedReader = new BufferedReader(new FileReader("/proc/diskstats"));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				matchCounter++;
				line = line.replaceAll(" +", " ").trim();
				if (deviceName.equalsIgnoreCase(line.split(" ")[2].trim())) {
					diskValue1 = Double.parseDouble(line.split(deviceName)[1].trim().split(" ")[9].trim());
					break;

				}
			}

			bufferedReader.close();

			// Sleep "duration" second before fetching again
			Thread.sleep(sleepDuration);

			bufferedReader = new BufferedReader(new FileReader("/proc/diskstats"));

			int counter = 0;

			double diskValue2 = 0;

			while ((line = bufferedReader.readLine()) != null) {
				counter++;
				if (counter == matchCounter) {
					diskValue2 = Double.parseDouble(line.split(deviceName)[1].trim().split(" ")[9].trim());
					break;

				}
			}

			double utilPercent = (diskValue2 - diskValue1) / (sleepDuration * 1000) * 100.0;
			utilPercent = utilPercent > 100 ? 100 : utilPercent;

			producer.send(new ProducerRecord<String, String>(topicName, "DISK", Utils.convertToJson(
					new ResourceStatisticsNode(ResourceType.DISK_LATENCY, decimalPrecision.format(utilPercent)))));
		} catch (FileNotFoundException e) {
			// TODO : Need to have proper exception handling and logging.
			System.out.println("Encountered an error in finding 'diskstats' file inside /proc/.");
			e.printStackTrace();
		} catch (NumberFormatException | IOException e) {
			System.out.println("Encountered an error reading the diskstats file.");
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("Encountered an error while sleeping the thread.");
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
