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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.http.ParseException;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.json.JSONArray;
import org.testng.annotations.Test;

import com.evolveum.polygon.connector.jira.rest.JiraConfiguration;
import com.evolveum.polygon.connector.jira.rest.JiraConnector;
import com.evolveum.polygon.connector.jira.rest.ProjectObject;

import org.identityconnectors.framework.common.objects.Name;

/**
 * @author surmanek
 *
 */
public class JiraConnectorSimpleTests {
	
	private static final Log LOG = Log.getLog(JiraConnector.class);
	
	
	public static SearchResultsHandler handler = new SearchResultsHandler() {

		@Override
		public boolean handle(ConnectorObject connectorObject) {
			results.add(connectorObject);
			return true;
		}

		@Override
		public void handleResult(SearchResult result) {
			LOG.info("im handling {0}", result.getRemainingPagedResults());
		}
	};
	private static final ArrayList<ConnectorObject> results = new ArrayList<>();


	 
	 @Test
	 public void test() throws URISyntaxException, ParseException, IOException { 
		JiraConfiguration config = new JiraConfiguration();
		config.setBaseUrl("172.16.1.253:8084");
		config.setUsername("administrator");
		GuardedString passwd = new GuardedString("training".toCharArray());
		config.setPassword(passwd);
		 
		JiraConnector conn = new JiraConnector();
		//LOG.info("schema: {0}", conn.schema());
		conn.init(config);
		
		conn.test();
		
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~OPTIONS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		Map<String, Object> operationOptions = new HashMap<String, Object>();
		operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
		operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, 1);
		operationOptions.put(OperationOptions.OP_PAGE_SIZE, 7);
		OperationOptions options = new OperationOptions(operationOptions);
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ACCOUNT OBJECT CLASS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		ObjectClass accountObjClass = new ObjectClass("__ACCOUNT__");
		//mandatory attributes:
		Set<Attribute> userAttrs = new HashSet<Attribute>();
		userAttrs.add(AttributeBuilder.build("__NAME__","superMario"));
		GuardedString password = new GuardedString("secret123".toCharArray());
		userAttrs.add(AttributeBuilder.build("__PASSWORD__", password));
		userAttrs.add(AttributeBuilder.build("emailAddress","super.mario@evo.com"));
		userAttrs.add(AttributeBuilder.build("displayName","Super Mario"));
		//avatar: 
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File("D:\\mushroom.png"));
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ImageIO.write(img, "png", baos);
		    //userAttrs.add(AttributeBuilder.build("binaryAvatar", baos.toByteArray()));
		} catch (IOException e) { e.printStackTrace(); }
		//group:
		//userAttrs.add(AttributeBuilder.build("groups","superMarioGroup"));
		
		//filters:
		//possible attributes for EQUALS filtering: name, uid
		AttributeFilter equalsUserFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("__UID__", "testGroup"));
		//LOG.info("\n\tEqualsFilter --> attr name: {0}, attr value: {1}", equalsUserFilter.getName(), equalsUserFilter.getAttribute().getValue().toString());
		//possible attributes for CONTAINS filtering: all
		AttributeFilter containsUserFilter = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "EX"));
		//LOG.info("\n\tContainsFilter --> attr name: {0}, attr value: {1}", containsUserFilter.getName(), containsUserFilter.getAttribute().getValue().toString());
		//possible attributes for STARTSWITH filtering: all
		AttributeFilter startsWithUserFilter = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("__NAME__","use")); //possible attribute name: username
		//LOG.info("\n\tStartsWithFilter --> attr name: {0}, attr value: {1}", startsWithUserFilter.getName(), startsWithUserFilter.getAttribute().getValue().toString());
		
		//Create:
		//LOG.info("\n\tCreated account: {0}", conn.create(accountObjClass, userAttrs, null).toString());
		
		//Read:
		//LOG.info("\n\tRead account:"); conn.executeQuery(accountObjClass, null, handler, options);
		
		//Update:
		//LOG.info("Updated account with uid: {0}", conn.update(accountObjClass, new Uid("supermario"), userAttrs, null).toString());
		
		//Delete:
		//LOG.info("\n\tDeleted account:"); conn.delete(accountObjClass, new Uid("supermario"), null);
		
		//Add actor to group:
		//conn.addAttributeValues(userObjClass, new Uid("supermario"), userAttrs, null);
		//Add actor to group:
		//conn.removeAttributeValues(userObjClass, new Uid("supermario"), userAttrs, null);
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~GROUP OBJECT CLASS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		ObjectClass groupObjClass = new ObjectClass("__GROUP__");
		//mandatory attributes:
		Set<Attribute> groupAttrs = new HashSet<Attribute>();
		groupAttrs.add(AttributeBuilder.build("__NAME__", "superMarioGroup"));
		//filters:
		//possible attributes for EQUALS filtering: name, uid
		AttributeFilter equalsGroupFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("__UID__", "testGroup"));
		//LOG.info("\n\tEqualsFilter --> attr name: {0}, attr value: {1}", equalsGroupFilter.getName(), equalsGroupFilter.getAttribute().getValue().toString());
		//possible attributes for CONTAINS filtering: name
		AttributeFilter containsGroupFilter = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("__NAME__", "EX")); 
		//LOG.info("\n\tContainsFilter --> attr name: {0}, attr value: {1}", containsGroupFilter.getName(), containsGroupFilter.getAttribute().getValue().toString());
		
		//Create:
		//LOG.info("\n\tCreated group: {0}", conn.create(groupObjClass, groupAttrs, null).toString());
		
		//Read:
		//LOG.info("\n\tRead group:"); conn.executeQuery(groupObjClass, null, handler, options);
		
		//Update is not permitted
		
		//Delete:
		//LOG.info("\n\tDeleted group:"); conn.delete(groupObjClass, new Uid("testgroup"), null);
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~PROJECT OBJECT CLASS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		ObjectClass projectObjClass = new ObjectClass("PROJECT");
		//mandatory attributes:
		Set<Attribute> projectAttrs = new HashSet<Attribute>();
		projectAttrs.add(AttributeBuilder.build("__NAME__","super_mega_project"));
		projectAttrs.add(AttributeBuilder.build("key","SUMEC"));
		projectAttrs.add(AttributeBuilder.build("projectTypeKey","business"));
		projectAttrs.add(AttributeBuilder.build("lead","charlie223"));
		//avatar:
		try {
		    img = ImageIO.read(new File("D:\\project2.jpg"));
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ImageIO.write(img, "jpg", baos);
		    //projectAttrs.add(AttributeBuilder.build("binaryAvatar", baos.toByteArray()));
		} catch (IOException e) { e.printStackTrace(); }
		
		//actors:
		String[] users = new String[] {"administrator", "charlie223", "lukasskublik", "testuser"};
		String[] groups = new String[] {"testGroup", "NewGroup"};
		//projectAttrs.add(AttributeBuilder.build("Developers.groups", groups));
		//projectAttrs.add(AttributeBuilder.build("Administrators.groups", groups));
		//projectAttrs.add(AttributeBuilder.build("Developers.users", users));
		//projectAttrs.add(AttributeBuilder.build("Administrators.users", users));
		
		
		//filters:
		//possible attributes for EQUALS filtering: uid
		AttributeFilter equalsProjectFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("__UID__", "10007"));
		//possible attributes for CONTAINS filtering: all attributes
		AttributeFilter containsProjectFilter = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("key", "EX"));
		//LOG.info("\n\tContainsFilter --> attr name: {0}, attr value: {1}", containsProjectFilter.getName(), containsProjectFilter.getAttribute().getValue().toString());
		//possible attributes for CONTAINS-ALL-VALUES filtering: Administrators.users, Administrators.groups, Developers.users, Developers.groups
		ArrayList<String> actors = new ArrayList<>();
		actors.add("exampleUser");
		AttributeFilter containsAllValuesProjectFilter = (ContainsAllValuesFilter) FilterBuilder.containsAllValues(AttributeBuilder.build("Developers.users", actors));
		//LOG.info("\n\tContainsAllValueFilter --> attr name: {0}, attr value: {1}", containsAllValuesProjectFilter.getName(), containsAllValuesProjectFilter.getAttribute().getValue().toString());
		
		//Create:
		//LOG.info("\n\tCreated project: {0}", conn.create(projectObjClass, projectAttrs, null).toString());
		
		//Read:
		//LOG.info("\n\tRead project:"); conn.executeQuery(projectObjClass, containsAllValuesProjectFilter, handler, options);
		
		//Update:
		//LOG.info("\n\tUpdated project: {0}", conn.update(projectObjClass, new Uid("10202"), projectAttrs, null).toString());
		
		//Delete:
		//LOG.info("\n\tDeleted project:"); conn.delete(projectObjClass, new Uid("10202"), null);
		
		//add user(s)/group(s) to project as member(s)/developer(s):
		//conn.addAttributeValues(projectObjClass, new Uid("10202"), projectAttrs, null);
		//remove user(s)/group(s) to project as member(s)/developer(s):
		//conn.removeAttributeValues(projectObjClass, new Uid("10202"), projectAttrs, null);
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		conn.dispose();
	 }
	 
}
