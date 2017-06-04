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

package com.softwareag.skyprofiler.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.mongodb.MongoClient;
import com.softwareag.skyprofiler.model.ServerData;

@Repository
public class ServerDataRepository {
	public static final String COLLECTION_NAME = "servers";

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private MongoClient client;

	public void addServerData(ServerData newServerData) {
		mongoTemplate.insert(newServerData, COLLECTION_NAME);
	}

	public ServerData updateServerData(ServerData toUpdate) {
		
		// TODO Implement Update.
		
		deleteServerData(toUpdate.getServerName());
		addServerData(toUpdate);
		
		return toUpdate;
	}

	public ServerData deleteServerData(String serverName) {
		ServerData deletedServerData = mongoTemplate.findAndRemove(Query.query(Criteria.where("_id").is(serverName)),
				ServerData.class, COLLECTION_NAME);
		client.dropDatabase(serverName);

		return deletedServerData;
	}

	public List<ServerData> getAllServerData() {
		Query query = new Query().with(new Sort("_id", "-1"));
		return mongoTemplate.find(query, ServerData.class, COLLECTION_NAME);
	}

	public ServerData getServerData(String serverName) {
		return mongoTemplate.findOne(Query.query(Criteria.where("_id").is(serverName)), ServerData.class,
				COLLECTION_NAME);
	}
}
