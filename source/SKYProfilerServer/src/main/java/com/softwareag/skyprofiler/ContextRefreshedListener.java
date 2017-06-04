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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.softwareag.skyprofiler.dao.ServerDataRepository;
import com.softwareag.skyprofiler.kafka.TopicManager;
import com.softwareag.skyprofiler.model.ServerData;

@Component
public class ContextRefreshedListener implements ApplicationListener<ContextRefreshedEvent> {
	@Autowired
	TopicManager topicManager;

	@Autowired
	private ServerDataRepository serverDataRepository;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
		if (serverDataRepository != null && topicManager != null) {
			List<ServerData> serverDataList = serverDataRepository.getAllServerData();

			for (ServerData serverData : serverDataList) {
				topicManager.addConsumer(serverData.getServerName());
			}
		}
	}
}