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

@SuppressWarnings("unused")
public class ServiceSummary {
	/** Package Name */
	private String pN;

	/** Service Name */
	private String sN;

	/** Service Type */
	private String sT;

	/** Total number of invocations */
	private int count;

	/** Elapsed time [sum of response time] */
	private double totalRT;

	/** Baseline response time */
	private Double baselineRT;

	/** Alert Threshold percentage */
	private int alertThresholdPercentage = 25;

	/** Baseline drift */
	private int drift;

	/** No of violations */
	private int violations = 0;

	/** Threshold percentage */
	private transient double thresholdRT;

	/**
	 * Constructor
	 * 
	 * @param currentBaseService
	 */
	public ServiceSummary(String pN, String sN, String sT, double rT) {
		this.pN = pN;
		this.sN = sN;
		this.sT = sT;
		this.totalRT = rT;
		this.count = 1;
	}

	public void setAlertThresholdPercentage(int alertThresholdPercentage) {
		this.alertThresholdPercentage = alertThresholdPercentage;

		if (baselineRT != null) {
			this.thresholdRT = this.baselineRT + (this.baselineRT * (this.alertThresholdPercentage / 100f));
		}
	}

	public void takeBaseline() {
		this.baselineRT = (this.totalRT / this.count) / 1000000;
		this.thresholdRT = this.baselineRT + (this.baselineRT * (this.alertThresholdPercentage / 100f));
	}

	public void addServiceData(double rT) {
		this.totalRT += rT;
		this.count++;
		checkForViolations(rT);
		calculateBaselineDrift();
	}

	private void checkForViolations(double rT) {
		if (baselineRT != null) {
			if ((rT / 1000000) > thresholdRT) {
				this.violations++;
			}
		}
	}

	public void calculateBaselineDrift() {
		if (baselineRT != null) {
			drift = (int) ((baselineRT - (totalRT / count)) / baselineRT * 100);
		}
	}
}