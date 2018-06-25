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

package com.softwareag.skyprofiler.model;

/**
 * Class Name : ResourceStatisticsNode
 * 
 * Description : A POJO class to store collected System resource statistics
 * information both from JVM and OS.
 */
@SuppressWarnings("unused")
public class ResourceStatisticsNode {
	/** Time of execution */
	private long eT;

	/** Type of resource */
	private ResourceType type;

	/** Value of the resource */
	private Object value;

	public ResourceStatisticsNode(ResourceType type, Object value) {
		this.eT = System.currentTimeMillis();
		this.type = type;
		this.value = value;
	}

	public ResourceStatisticsNode(ResourceType type, Object value, long eT) {
		this.eT = eT;
		this.type = type;
		this.value = value;
	}
}
