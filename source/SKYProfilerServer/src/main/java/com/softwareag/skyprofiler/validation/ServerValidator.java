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

package com.softwareag.skyprofiler.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.softwareag.skyprofiler.model.ServerData;

public class ServerValidator implements Validator {
	private static final String NAME_PATTERN = "^[a-zA-Z]+([-._a-zA-Z0-9_]+)*$";

	private static final String SERVER_NAME_LENGTH = "Server name should contain minimum of 4 characters and maximum of 50 characters.";

	private static final String SERVER_NAME_PATTERN = "Server name should start with alphabets and can contain only alphabets, numbers, -, _ and .";

	@Override
	public boolean supports(Class<?> clazz) {
		return ServerData.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		ServerData serverData = (ServerData) target;

		if (!isNameLengthValid(serverData.getServerName())) {
			errors.rejectValue("serverName", SERVER_NAME_LENGTH);
		}

		if (!isValidName(serverData.getServerName())) {
			errors.rejectValue("serverName", SERVER_NAME_PATTERN);
		}
	}

	private boolean isValidName(String name) {
		Pattern pattern = Pattern.compile(NAME_PATTERN);
		Matcher matcher = pattern.matcher(name);
		return matcher.matches();
	}

	private boolean isNameLengthValid(String name) {
		if (name == null || name.isEmpty() || name.length() < 4 || name.length() > 12) {
			return false;
		}

		return true;
	}
}
