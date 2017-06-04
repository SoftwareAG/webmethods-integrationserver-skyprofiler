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

package com.softwareag.skyprofiler.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.softwareag.skyprofiler.dao.ServerDataRepository;
import com.softwareag.skyprofiler.dao.ServiceDataRepository;
import com.softwareag.skyprofiler.dao.ServiceSummaryCache;
import com.softwareag.skyprofiler.kafka.TopicManager;
import com.softwareag.skyprofiler.model.ServerData;
import com.softwareag.skyprofiler.model.ServiceSummary;
import com.softwareag.skyprofiler.utils.Utility;
import com.softwareag.skyprofiler.validation.ServerValidator;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerController {
	Logger logger = LoggerFactory.getLogger(ServerController.class);

	@Autowired
	TopicManager kafkaManager;

	@Autowired
	private ServerDataRepository serverDataRepository;

	@Autowired
	private ServiceDataRepository serviceDataRepository;

	@Autowired
	private ServiceSummaryCache cache;

	@Autowired
	private SimpMessagingTemplate template;

	private String selectedServer;

	private Gson gson = new Gson();

	@RequestMapping(value = "/addServer", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody void addServer(@ModelAttribute("ServerData") ServerData newServerData, BindingResult result,
			HttpServletResponse response) throws Exception {

		ServerValidator serverValidator = new ServerValidator();
		serverValidator.validate(newServerData, result);
		if (result.hasErrors()) {
			List<String> errors = new ArrayList<String>();
			result.getAllErrors().forEach((value) -> {
				errors.add(value.getCode());
			});
			response.sendError(HttpStatus.SC_BAD_REQUEST, gson.toJson(errors));
		} else {
			try {
				HttpResponse isResponse = Utility.sendConfigurationToServer(newServerData, "", "",
						newServerData.getServerName(), "");
				if (isResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					serverDataRepository.addServerData(newServerData);
					kafkaManager.addConsumer(newServerData.getServerName());
				} else {
					List<String> errors = new ArrayList<String>();
					errors.add(isResponse.getStatusLine().getReasonPhrase());
					response.sendError(isResponse.getStatusLine().getStatusCode(), gson.toJson(errors));
				}
			} catch (Exception e) {
				List<String> errors = new ArrayList<String>();
				errors.add(e.getMessage());
				response.sendError(HttpStatus.SC_BAD_REQUEST, gson.toJson(errors));
			}
		}
	}

	@RequestMapping(value = "/updateServer", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody void updateServer(@ModelAttribute("ServerData") ServerData updateServerData,
			BindingResult result, HttpServletResponse response) throws Exception {

		ServerValidator serverValidator = new ServerValidator();
		serverValidator.validate(updateServerData, result);
		
		if (result.hasErrors()) {
			List<String> errors = new ArrayList<String>();
			result.getAllErrors().forEach((value) -> {
				errors.add(value.getCode());
			});
			response.sendError(HttpStatus.SC_BAD_REQUEST, gson.toJson(errors));
		} else {
			// Check server connectivity
			try {
				String targetURL = "http://" + updateServerData.getHostName() + ":" + updateServerData.getPort()
						+ "/invoke/SKYProfiler.svc:isRunning";
				String encoding = Base64.getEncoder().encodeToString(
						(updateServerData.getUserName() + ":" + updateServerData.getPassword()).getBytes());

				HttpPost httppost = new HttpPost(targetURL);
				httppost.setHeader("Authorization", "Basic " + encoding);
				httppost.setHeader("Accept", "application/json");

				HttpClient client = HttpClientBuilder.create().build();
				HttpResponse isResponse = client.execute(httppost);
				if (isResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					serverDataRepository.updateServerData(updateServerData);
				} else {
					List<String> errors = new ArrayList<String>();
					errors.add(isResponse.getStatusLine().getReasonPhrase());
					response.sendError(isResponse.getStatusLine().getStatusCode(), gson.toJson(errors));
				}
			} catch (Exception e) {
				List<String> errors = new ArrayList<String>();
				errors.add(e.getMessage());
				response.sendError(HttpStatus.SC_BAD_REQUEST, gson.toJson(errors));
			}
		}
	}

	@RequestMapping(value = "/getAllServer", method = RequestMethod.GET)
	public List<ServerData> getAllServer() {
		return serverDataRepository.getAllServerData();
	}

	@RequestMapping(value = "/getServerInformation", method = RequestMethod.POST)
	public ServerData getServerInformation(@RequestParam(value = "serverName", required = true) String serverName) {
		return serverDataRepository.getServerData(serverName);
	}

	@RequestMapping(value = "/selectedServer", method = RequestMethod.POST)
	public void setSelectedServer(@RequestParam(value = "serverName", required = true) String serverName) {
		this.selectedServer = serverName;
	}

	@RequestMapping(value = "/clearProfiledData", method = RequestMethod.POST)
	public void clearServerProfiledData(@RequestParam(value = "serverName", required = true) String serverName) {
		// The serverName should be same as selectedServer.
		serviceDataRepository.clearServerProfiledData(serverName);
		cache.clearServiceData(serverName);
	}

	@RequestMapping(value = "/deleteServer", method = RequestMethod.POST)
	public void deleteServer(@RequestParam(value = "serverName", required = true) String serverName) {
		serverDataRepository.deleteServerData(serverName);
		kafkaManager.stopConsumer(serverName);
	}

	@RequestMapping(value = "/isRunning", method = RequestMethod.GET)
	public boolean isRunning(@RequestParam(value = "serverName", required = true) String serverName) throws Exception {
		ServerData serverData = serverDataRepository.getServerData(serverName);

		if (serverData != null) {
			String targetURL = "http://" + serverData.getHostName() + ":" + serverData.getPort()
					+ "/invoke/SKYProfiler.svc:isRunning";
			String encoding = Base64.getEncoder()
					.encodeToString((serverData.getUserName() + ":" + serverData.getPassword()).getBytes());

			HttpPost httppost = new HttpPost(targetURL);
			httppost.setHeader("Authorization", "Basic " + encoding);
			httppost.setHeader("Accept", "application/json");

			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse response = client.execute(httppost);
			String jsonResponse = EntityUtils.toString(response.getEntity());
			JsonObject responseJsonObject = new Gson().fromJson(jsonResponse, JsonObject.class);

			return responseJsonObject.get("status").getAsBoolean();
		} else {
			logger.error(serverName + " data is missing in database to verify if the server is running.");
			throw new Exception("Unable to fetch " + serverName + " data from database.");
		}
	}

	@RequestMapping(value = "/start", method = RequestMethod.GET)
	public void startServer(@RequestParam(value = "serverName", required = true) String serverName,
			HttpServletResponse response) throws Exception {
		ServerData serverData = serverDataRepository.getServerData(serverName);

		if (serverData != null) {
			String targetURL = "http://" + serverData.getHostName() + ":" + serverData.getPort()
					+ "/invoke/SKYProfiler.svc:start";
			String encoding = Base64.getEncoder()
					.encodeToString((serverData.getUserName() + ":" + serverData.getPassword()).getBytes());

			HttpPost httppost = new HttpPost(targetURL);
			httppost.setHeader("Authorization", "Basic " + encoding);
			httppost.setHeader("Accept", "application/json");

			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse isResponse = client.execute(httppost);

			if (isResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return;
			}

			response.sendError(isResponse.getStatusLine().getStatusCode(),
					isResponse.getStatusLine().getReasonPhrase());
		} else {
			logger.info(serverName + " details not found in the database to start the server.");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					serverName + " details not found in the database to start the server.");
		}
	}

	@RequestMapping(value = "/getServerConfiguration", method = RequestMethod.POST)
	public String getServerConfiguration(@RequestParam(value = "serverName", required = true) String serverName)
			throws Exception {
		ServerData serverData = serverDataRepository.getServerData(serverName);

		if (serverData != null) {
			String targetURL = "http://" + serverData.getHostName() + ":" + serverData.getPort()
					+ "/invoke/SKYProfiler.svc:getConfiguration";
			String encoding = Base64.getEncoder()
					.encodeToString((serverData.getUserName() + ":" + serverData.getPassword()).getBytes());

			HttpPost httppost = new HttpPost(targetURL);
			httppost.setHeader("Authorization", "Basic " + encoding);
			httppost.setHeader("Accept", "application/json");

			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse response = client.execute(httppost);
			String jsonResponse = EntityUtils.toString(response.getEntity());

			return jsonResponse;
		} else {
			logger.error(serverName + " data is missing in database to get configuration data.");
			throw new Exception("Unable to fetch " + serverName + " data from database.");
		}
	}

	@RequestMapping(value = "/saveServerConfiguration", method = RequestMethod.POST)
	public void saveServerConfiguration(@RequestParam(value = "serverName") String serverName,
			@RequestParam(value = "includedPackages") String includedPackages,
			@RequestParam(value = "kafkaBootstrapUrl") String kafkaBootstrapUrl,
			@RequestParam(value = "kafkaTopicName") String kafkaTopicName,
			@RequestParam(value = "externalHostname") String externalHostname, HttpServletResponse response)
			throws Exception {

		ServerData serverData = serverDataRepository.getServerData(serverName);
		if (serverData != null) {
			HttpResponse isResponse = Utility.sendConfigurationToServer(serverData, includedPackages, kafkaBootstrapUrl,
					kafkaTopicName, externalHostname);
			if (isResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return;
			}

			response.sendError(isResponse.getStatusLine().getStatusCode(),
					isResponse.getStatusLine().getReasonPhrase());
		} else {
			logger.error(serverName + " data is missing in database to get configuration data.");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Unable to find " + serverName + " data in the database.");
		}
	}

	@RequestMapping(value = "/stop", method = RequestMethod.GET)
	public boolean stopServer(@RequestParam(value = "serverName", required = true) String serverName) throws Exception {
		ServerData serverData = serverDataRepository.getServerData(serverName);

		if (serverData != null) {
			String targetURL = "http://" + serverData.getHostName() + ":" + serverData.getPort()
					+ "/invoke/SKYProfiler.svc:stop";
			String encoding = Base64.getEncoder()
					.encodeToString((serverData.getUserName() + ":" + serverData.getPassword()).getBytes());

			HttpPost httppost = new HttpPost(targetURL);
			httppost.setHeader("Authorization", "Basic " + encoding);
			httppost.setHeader("Accept", "application/json");

			HttpClient client = HttpClientBuilder.create().build();
			HttpResponse response = client.execute(httppost);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				return true;
			}
		}
		return false;
	}

	@RequestMapping(value = "/report/{serverName}", method = RequestMethod.GET)
	public void download(@PathVariable("serverName") String serverName, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		logger.info("Report Generation Trigger for " + serverName);

		try {
			String serviceSummaryJson = serviceDataRepository.getServiceSummaryJson(serverName);
			String htmlStr = Utility.getReportFileContent();
			htmlStr = htmlStr.replace("$PROFILED_DATA$", serviceSummaryJson);

			String filename = "SkyProfiler_Report_" + serverName + ".html";
			File reportFile = new File(filename);
			FileWriter fileWriter = new FileWriter(reportFile);
			fileWriter.write(htmlStr);
			fileWriter.close();

			response.setHeader("Content-Disposition", "attachment; filename=" + filename);
			response.setContentType("application/html");

			Path path = reportFile.toPath();
			Files.copy(path, response.getOutputStream());
			response.flushBuffer();

			logger.info("Report successfully generated for " + serverName);
		} catch (Exception e) {
			logger.warn("Exception in Report Generation :" + e.getMessage());
			throw e;
		}
	}

	@Scheduled(fixedDelay = 1000)
	public void publishUpdates() {
		Map<String, Map<String, ServiceSummary>> cacheMap;
		Map<String, ServiceSummary> selectedServerServiceMap;

		String jsonInString1;

		if (selectedServer != null) {
			cacheMap = cache.getMap();
			selectedServerServiceMap = cacheMap.get(selectedServer);

			if (selectedServerServiceMap != null) {
				jsonInString1 = gson.toJson(selectedServerServiceMap.values());
				this.template.convertAndSend("/topic/" + selectedServer + "/service", jsonInString1);
			}
		}
	}
}
