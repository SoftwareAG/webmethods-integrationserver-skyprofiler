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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.sun.management.OperatingSystemMXBean;
import com.softwareag.skyprofiler.model.ResourceStatisticsNode;
import com.softwareag.skyprofiler.model.ResourceType;
import com.softwareag.skyprofiler.utility.Utils;
import com.sun.management.GarbageCollectionNotificationInfo;

/**
 * 
 * Description : This class helps in getting System resource statistics
 * information both from JVM and OS.
 */
@SuppressWarnings("restriction")
public class ResourceStatisticsCollector {

	private Timer time;

	private String topicName;

	private NotificationListener listener;

	private OperatingSystemMXBean osMXBean;

	private Producer<String, String> producer;

	public ResourceStatisticsCollector(Producer<String, String> producer, String topicName, String externalHostname) {
		this.producer = producer;

		this.topicName = topicName;

		// Bean for CPU usage monitoring
		osMXBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

		// For GC monitoring
		List<GarbageCollectorMXBean> gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean gcbean : gcbeans) {
			NotificationEmitter emitter = (NotificationEmitter) gcbean;

			listener = new NotificationListener() {
				@Override
				public void handleNotification(Notification notification, Object handback) {
					if (notification.getType()
							.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
						GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo
								.from((CompositeData) notification.getUserData());

						ResourceType gcType = ResourceType.GC_MINOR;
						if ("end of major GC".equals(info.getGcAction())) {
							gcType = ResourceType.GC_MAJOR;
						}

						producer.send(new ProducerRecord<String, String>(topicName, "GC",
								Utils.convertToJson(new ResourceStatisticsNode(gcType, info.getGcInfo().getDuration(),
										notification.getTimeStamp()))));
					}
				}
			};
			emitter.addNotificationListener(listener, null, null);
		}

		ScheduledTaskDiskStats stDisk = new ScheduledTaskDiskStats();

		// Collect disk statistics for every one second
		time = new Timer();
		time.schedule(stDisk, 0, 1000);

		// Collect network statistics for every one second
		if (!(externalHostname == null || externalHostname.equals(""))) {
			ScheduledTaskNetworkStats stNW = new ScheduledTaskNetworkStats(externalHostname);
			time.schedule(stNW, 0, 1000);
		}

		// Collect cpu statistics for every one second
		ScheduledTaskCPUStats stCPU = new ScheduledTaskCPUStats();
		time.schedule(stCPU, 0, 1000);
	}

	/**
	 * Method to stop resource statistics collection.
	 */
	public void stop() {
		List<GarbageCollectorMXBean> gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean gcbean : gcbeans) {
			NotificationEmitter emitter = (NotificationEmitter) gcbean;
			try {
				emitter.removeNotificationListener(listener, null, null);
			} catch (ListenerNotFoundException e) {
				// Listener not found. Nothing to do.
			}
		}

		this.time.cancel();
	}

	// This task collects Disk Statistics
	class ScheduledTaskDiskStats extends TimerTask {

		@Override
		public void run() {
			BufferedReader bufferedReader = null;
			DecimalFormat decimalPrecision = new DecimalFormat(".###");

			String line;
			String diskLatency = "";
			String deviceName = "sda";

			double utilPercent = 0;
			double diskValue1 = 0;
			double diskValue2 = 0;

			// Delay
			int duration = 1;

			int counter = 0;

			// Counter to match nth line which contains device string "sda"
			int matchCounter = 0;

			try {
				bufferedReader = new BufferedReader(new FileReader("/proc/diskstats"));
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
				Thread.sleep(duration * 1000);

				bufferedReader = new BufferedReader(new FileReader("/proc/diskstats"));
				while ((line = bufferedReader.readLine()) != null) {
					counter++;
					if (counter == matchCounter) {
						diskValue2 = Double.parseDouble(line.split(deviceName)[1].trim().split(" ")[9].trim());
						break;

					}

				}
			} catch (FileNotFoundException e) {
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

			utilPercent = (diskValue2 - diskValue1) / (duration * 1000) * 100.0;
			if (utilPercent > 100)
				utilPercent = 100;
			diskLatency = decimalPrecision.format(utilPercent);

			producer.send(new ProducerRecord<String, String>(topicName, "DISK",
					Utils.convertToJson(new ResourceStatisticsNode(ResourceType.DISK_LATENCY, diskLatency))));

		}
	}

	// This task collects Network Statistics
	class ScheduledTaskNetworkStats extends TimerTask {
		InetAddress remoteAddress;
		String hostname;
		double endTime, deltaInMS;
		double startTime, nwLatencyDB;
		boolean isHostAlive;

		int timeoutInSecond = 2;

		public ScheduledTaskNetworkStats(String externalHostname) {
			hostname = externalHostname;
		}

		public void run() {
			try {
				// Sleeping for 1 second to avoid network congestion
				Thread.sleep(1000);

				endTime = 0;
				deltaInMS = 0;

				startTime = System.currentTimeMillis();
				remoteAddress = InetAddress.getByName(hostname);
				isHostAlive = remoteAddress.isReachable(timeoutInSecond * 1000);
				endTime = System.currentTimeMillis();
				deltaInMS = endTime - startTime;
				nwLatencyDB = isHostAlive ? deltaInMS : -1;
				producer.send(new ProducerRecord<String, String>(topicName, "NETWORK", Utils.convertToJson(
						new ResourceStatisticsNode(ResourceType.NETWORK_LATENCY, String.valueOf(nwLatencyDB)))));
			} catch (InterruptedException e) {
				System.out.println("Interrupted while sleeping the thread.");
				e.printStackTrace();
			} catch (UnknownHostException e) {
				System.out.println("Lookup failed for " + hostname);
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Destination host is not reachable");
				e.printStackTrace();
			}
		}
	}

	// This task collects CPU Statistics
	class ScheduledTaskCPUStats extends TimerTask {
		public void run() {
			producer.send(new ProducerRecord<String, String>(topicName, "SYSTEM_CPU", Utils.convertToJson(
					new ResourceStatisticsNode(ResourceType.SYSTEM_CPU_LOAD, osMXBean.getSystemCpuLoad()))));
			producer.send(new ProducerRecord<String, String>(topicName, "PROCESS_CPU", Utils.convertToJson(
					new ResourceStatisticsNode(ResourceType.PROCESS_CPU_LOAD, osMXBean.getProcessCpuLoad()))));
		}
	}
}
