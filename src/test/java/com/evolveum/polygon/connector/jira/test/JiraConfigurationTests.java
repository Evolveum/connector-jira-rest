package com.evolveum.polygon.connector.jira.test;
/*******************************************************************************
 * Copyright 2017 Evolveum
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License.  
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
 * See the License for the specific language governing permissions and limitations under the License.
 ******************************************************************************/
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.testng.annotations.Test;

import com.evolveum.polygon.connector.jira.rest.JiraConfiguration;
import com.evolveum.polygon.connector.jira.rest.JiraConnector;

/**
 * @author surmanek
 *
 */
public class JiraConfigurationTests extends JiraTestHelper{
	
	private JiraConnector jiraConnector = new JiraConnector();
	
	@Test(priority = 1)
	public void configurationValidityTest(){
		jiraConnector.init(getConfiguration());
		jiraConnector.test();
	}
	
	@Test(priority = 2, expectedExceptions = InvalidCredentialException.class)
	public void invalidCredentialsNegativeTest() {
		JiraConfiguration config = new JiraConfiguration();
		
		config.setBaseUrl("172.16.1.253:8084");
		config.setUsername("administrator");
		GuardedString passwd = new GuardedString("training123".toCharArray());
		config.setPassword(passwd);
		 
		jiraConnector.init(config);
		jiraConnector.test();
	}
	
	@Test(priority = 2, expectedExceptions = ConfigurationException.class)
	public void missingMandatoryValueNegativeTest() {
		JiraConfiguration config = new JiraConfiguration();
		config.setBaseUrl("172.16.1.253:8084");
		config.setUsername("");
		GuardedString passwd = new GuardedString("training".toCharArray());
		config.setPassword(passwd);
		 
		jiraConnector.init(config);
		jiraConnector.test();
	}
	
	@Test(priority = 2, expectedExceptions = ConnectorIOException.class)
	public void connectionNegativeTest() {
		JiraConfiguration config = new JiraConfiguration();
		config.setBaseUrl("172.16.100.253:8084");
		config.setUsername("administrator");
		GuardedString passwd = new GuardedString("training".toCharArray());
		config.setPassword(passwd);
		 
		jiraConnector.init(config);
		jiraConnector.test();
	}

}
