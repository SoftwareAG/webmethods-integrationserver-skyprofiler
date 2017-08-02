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

import java.lang.management.GarbageCollectorMXBean;
import java.net.UnknownHostException;
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

import com.softwareag.skyprofiler.model.ResourceStatisticsNode;
import com.softwareag.skyprofiler.model.ResourceType;
import com.softwareag.skyprofiler.utility.Utils;
import com.sun.management.GarbageCollectionNotificationInfo;

/**
 * 
 * Description : This class helps in getting System resource statistics
 * information both from JVM and OS.
 * 
 * @author YEJ
 */
@SuppressWarnings("restriction")
public class ResourceStatisticsCollector {
	private Timer timer;

	private NotificationListener listener;

	private TimerTask diskStatsCollectorTask;

	private TimerTask networkStatsCollectorTask;

	private TimerTask cpuStatsCollectorTask;

	public ResourceStatisticsCollector(Producer<String, String> producer, String topicName, String externalHostname) {
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

		timer = new Timer();

		// Collect disk statistics for every one second
		try {
			diskStatsCollectorTask = DiskStatisticsCollectorFactory.getDiskCollectorTask(producer, topicName);
			timer.schedule(diskStatsCollectorTask, 0, 1000);
		} catch (Exception e) {

		}

		// Collect network statistics for every one second
		if (!(externalHostname == null || externalHostname.equals(""))) {
			try {
				networkStatsCollectorTask = new NetworkStatisticsCollectorTask(producer, topicName, externalHostname);
				timer.schedule(networkStatsCollectorTask, 0, 1000);
			} catch (UnknownHostException e) {
				System.out.println("Failed while starting network statistics collector : " + e.getMessage());
			}
		}

		// Collect cpu statistics for every one second
		cpuStatsCollectorTask = new CpuStatisticsCollectorTask(producer, topicName);
		timer.schedule(cpuStatsCollectorTask, 0, 1000);
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

		diskStatsCollectorTask.cancel();
		timer.cancel();
		timer.purge();
	}
}
