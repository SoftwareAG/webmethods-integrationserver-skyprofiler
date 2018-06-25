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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TimerTask;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.softwareag.skyprofiler.model.ResourceStatisticsNode;
import com.softwareag.skyprofiler.model.ResourceType;
import com.softwareag.skyprofiler.utility.Utils;

/**
 * Description : This class is used to collect the Network statistics of the
 * machine.
 * 
 * @author YEJ
 */
public class NetworkStatisticsCollectorTask extends TimerTask {
	private String topicName;

	private String externalHostname;

	private Producer<String, String> producer;

	private InetAddress remoteAddress;

	private int timeoutInSecond = 2;

	public NetworkStatisticsCollectorTask(Producer<String, String> producer, String topicName, String externalHostname)
			throws UnknownHostException {
		this.producer = producer;
		this.topicName = topicName;
		this.externalHostname = externalHostname;

		remoteAddress = InetAddress.getByName(externalHostname);
	}

	@Override
	public void run() {
		try {
			// Sleeping for 1 second to avoid network congestion. TODO: Do we really need this?
			Thread.sleep(1000);

			long startTime = System.currentTimeMillis();
			boolean isHostAlive = remoteAddress.isReachable(timeoutInSecond * 1000);
			long endTime = System.currentTimeMillis();
			long nwLatency = isHostAlive ? endTime - startTime : -1;

			producer.send(new ProducerRecord<String, String>(topicName, "NETWORK", Utils.convertToJson(
					new ResourceStatisticsNode(ResourceType.NETWORK_LATENCY, String.valueOf(nwLatency)))));
		} catch (InterruptedException e) {
			// TODO : Need to have proper exception handling and logging.
			System.out.println("Interrupted while sleeping the thread.");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			System.out.println("Lookup failed for " + externalHostname);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Destination host is not reachable");
			e.printStackTrace();
		}
	}
}
