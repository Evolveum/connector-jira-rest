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
import java.util.ArrayList;
import java.util.HashMap;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.spi.SearchResultsHandler;

import com.evolveum.polygon.connector.jira.rest.JiraConfiguration;
import com.evolveum.polygon.connector.jira.rest.JiraConnector;

/**
 * @author surmanek
 *
 */
public class JiraTestHelper {
	
	private static final String baseUrl = "172.16.1.253:8084";
	private static final String adminUsername = "administrator";
	private static final String adminPassword = "training";
	
	//account variables:
	protected static final JiraConnector jiraConnector = new JiraConnector();
	protected static final OperationOptions options = new OperationOptions(new HashMap<String, Object>());
	protected static final Log LOG = Log.getLog(JiraConnector.class);
	protected static final ObjectClass accountObjectClass = ObjectClass.ACCOUNT;
	
	protected GuardedString password = new GuardedString("secret".toCharArray());
	protected final ArrayList<ConnectorObject> accountResults = new ArrayList<>();
	protected SearchResultsHandler accountHandler = new SearchResultsHandler() {
		@Override
		public boolean handle(ConnectorObject connectorObject) {
			accountResults.add(connectorObject);
			return true;
		}

		@Override
		public void handleResult(SearchResult result) {
		}
	};
	
	//group variables:
	protected static final ObjectClass groupObjectClass = ObjectClass.GROUP;
	protected final ArrayList<ConnectorObject> groupResults = new ArrayList<>();
	protected SearchResultsHandler groupHandler = new SearchResultsHandler() {
		@Override
		public boolean handle(ConnectorObject connectorObject) {
			groupResults.add(connectorObject);
			return true;
		}

		@Override
		public void handleResult(SearchResult result) {
		}
	};
	
	//project variables:
	protected static final ObjectClass projectObjectClass = new ObjectClass("PROJECT");
	protected final ArrayList<ConnectorObject> projectResults = new ArrayList<>();
	protected SearchResultsHandler projectHandler = new SearchResultsHandler() {
		@Override
		public boolean handle(ConnectorObject connectorObject) {
			projectResults.add(connectorObject);
			return true;
		}

		@Override
		public void handleResult(SearchResult result) {
		}
	};
	
	protected JiraConfiguration getConfiguration() {
		JiraConfiguration config = new JiraConfiguration();
		config.setBaseUrl(baseUrl);
		config.setUsername(adminUsername);
		GuardedString passwd = new GuardedString(adminPassword.toCharArray());
		config.setPassword(passwd);
		return config;
	}

}
