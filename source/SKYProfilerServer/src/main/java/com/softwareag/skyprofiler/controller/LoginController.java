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

package com.softwareag.skyprofiler.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.softwareag.skyprofiler.dao.ServerDataRepository;
import com.softwareag.skyprofiler.model.ServerData;

@Controller
public class LoginController {
	@Autowired
	private ServerDataRepository serverDataRepository;

	@RequestMapping("/login")
	public String getLoginPage() {
		return "login";
	}

	@RequestMapping(value = { "/" }, method = RequestMethod.GET)
	public ModelAndView getHomePage(Authentication authentication) {
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		return new ModelAndView("index", "username", userDetails.getUsername());
	}

	@RequestMapping(value = "/{value}", method = RequestMethod.GET)
	public void redirect(@PathVariable("value") String value, HttpServletResponse response) throws IOException {

		ServerData serverData = serverDataRepository.getServerData(value);
		if (serverData != null) {
			response.sendRedirect("/#/" + value);
		} else {
			response.sendRedirect("/");
		}
	}
}
