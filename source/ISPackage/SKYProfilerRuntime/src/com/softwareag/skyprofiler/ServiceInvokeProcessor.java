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

package com.softwareag.skyprofiler;

import java.util.Iterator;
import java.util.Map;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.softwareag.skyprofiler.model.ServiceCallNode;
import com.softwareag.skyprofiler.utility.Utils;
import com.wm.app.b2b.server.BaseService;
import com.wm.app.b2b.server.invoke.InvokeChainProcessor;
import com.wm.app.b2b.server.invoke.ServiceStatus;
import com.wm.data.IData;
import com.wm.util.ServerException;

/**
 * Class Name : ServiceInvokeProcessor
 * 
 * Description : The subscriber class whose object is added to the invoke chain
 * to get the notification from Integration Server when the service execution
 * gets triggered and before the actual service invocation. When called it
 * stores the execution statistics of the service.
 */
public class ServiceInvokeProcessor implements InvokeChainProcessor {

	private String serviceTopicName;

	private Producer<String, String> producer;

	private Map<String, Object> includedPkgsMap;

	private ThreadLocal<ServiceCallNode> threadLocal;

	/**
	 * Constructor.
	 * 
	 * @param producer
	 * @param serviceTopicName
	 * @param includedPkgsMap
	 */
	public ServiceInvokeProcessor(Producer<String, String> producer, String serviceTopicName,
			Map<String, Object> includedPkgsMap) {
		this.threadLocal = new ThreadLocal<ServiceCallNode>();
		this.producer = producer;
		this.serviceTopicName = serviceTopicName;
		this.includedPkgsMap = includedPkgsMap;
	}

	/**
	 * Setter method to set the selected packages which needs to be monitored.
	 * Those services which are part of the selected packages will be considered
	 * for collecting the statistics. Whereas the services which are part of
	 * other packages will be ignored.
	 * 
	 * @param includedPkgsMap
	 */
	public void setIncludedPkgMap(Map<String, Object> includedPkgsMap) {
		this.includedPkgsMap = includedPkgsMap;
	}

	@Override
	public void process(@SuppressWarnings("rawtypes") Iterator chain, BaseService baseService, IData pipeline,
			ServiceStatus status) throws ServerException {
		double timeTaken;

		// PRE-PROCESSING
		if (status.isTopService()) {
			threadLocal.set(new ServiceCallNode(baseService));
		} else {
			threadLocal.set(new ServiceCallNode(baseService, threadLocal.get()));
		}

		// EXECUTION :: continuing the chain
		try {
			// required by InvokeChainProcessor definition
			if (chain.hasNext()) {
				((InvokeChainProcessor) chain.next()).process(chain, baseService, pipeline, status);
			}
		} finally {
			// What do you want to do here
		}

		// POST-PROCESSING
		ServiceCallNode currentNode = threadLocal.get();
		timeTaken = currentNode.endService();
		ServiceCallNode parent = currentNode.getParentNode();
		if (parent != null) {
			parent.setChildrenResponseTime(timeTaken);
			threadLocal.set(parent);
		} else {
			if (includedPkgsMap.containsKey(currentNode.getPackageName())) {
				producer.send(new ProducerRecord<String, String>(serviceTopicName,
						currentNode.getPackageName() + currentNode.getServiceName(), Utils.convertToJson(currentNode)));
			}
		}
	}
}
