/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.polygon.connector.jira.rest;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

/**
 * @author surmanek
 *
 */
public class JiraConfiguration extends AbstractConfiguration implements StatefulConfiguration{
	
	private static final Log LOG = Log.getLog(JiraConfiguration.class);

	private String name;
	private GuardedString password;
	private String baseUrl;
	
	// getter and setter methods for "baseUrl" attribute:
		@ConfigurationProperty(order = 1, displayMessageKey = "baseUrl.display", helpMessageKey = "baseUrl.help", required = true, confidential = false)
		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

	// getter and setter methods for "name" attribute:
	@ConfigurationProperty(order = 2, displayMessageKey = "username.display", helpMessageKey = "username.help", required = true, confidential = false)
	public String getUsername() {
		/*if (name == null || name.isEmpty()){
			String exceptionMsg = "Name is not provided";
			LOG.error(exceptionMsg.toString());
			throw new InvalidCredentialException(exceptionMsg);
		}*/
		return name;
	}

	public void setUsername(String name) {
		this.name = name;
	}

	private String stringPassword = "";
	// getter and setter methods for "password" attribute:
	@ConfigurationProperty(order = 3, displayMessageKey = "password.display", helpMessageKey = "password.help", required = true, confidential = false)
	public GuardedString getPassword() {
		return password;
	}
	public void setPassword(GuardedString password) {
		this.password = password;
	}
	
	//convert GuardedString passwd to String passwd
	public String getStringPassword() {
		/*if (password == null || "".equals(password)){
			String exceptionMsg = "Password is not provided";
			LOG.error(exceptionMsg.toString());
			throw new InvalidPasswordException(exceptionMsg);
		}*/
		password.access(new GuardedString.Accessor() {
			@Override
			public void access(char[] clearChars) {
				stringPassword = new String(clearChars);
			}
		});
		return stringPassword;
	}
	
	@Override
	public void validate() {

		LOG.info("Processing trough configuration validation procedure.");
		String exceptionMsg = "";
		if (baseUrl == null || StringUtil.isBlank(baseUrl)) {
			exceptionMsg = "Base url is not provided.";
			LOG.error(exceptionMsg.toString());
			throw new ConfigurationException(exceptionMsg);
		}
		if (name == null || StringUtil.isBlank(name)) {
			exceptionMsg = "Name is not provided.";
			LOG.error(exceptionMsg.toString());
			throw new ConfigurationException(exceptionMsg);
		}
		if (password == null || "".equals(password)) {
			exceptionMsg = "Password is not provided.";
			LOG.error(exceptionMsg.toString());
			throw new ConfigurationException(exceptionMsg);
		}

		LOG.info("Configuration id valid.");
	}
	
	@Override
	public void release() {
		LOG.info("The release of configuration resources is being performed");

		this.password = null;
		this.name = null;
		this.baseUrl = null;
	}
	
	@Override
	public String toString() {
		return "JiraConnectorConfiguration{" +
				"username='" + name + '\'' +
				", baseUrl='" + baseUrl + '\'' +
				'}';
	}
}
