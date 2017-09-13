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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.testng.annotations.Test;

/**
 * @author surmanek
 *
 */
public class JiraConnectorPerformanceTests extends JiraTestHelper {

	private static final int COUNT = 10;
	private static final String UPDATED = "updated";

	@Test(priority = 1)
	public void initTest() {
		jiraConnector.init(getConfiguration());
		
		//create lists of needed values:
		createUsernamesList();
		createGroupNamesList();
		createProjectNamesList();
		//cleanup before testing:
		projectsCleanUp();
		groupsCleanUp();
		accountsCleanUp();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ACCOUNT OBJECT CLASS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private List<Uid> createdAccountIds = new ArrayList<>();
	private List<String> createdAccountNames = new ArrayList<>();
	private List<String> updatedAccountNames = new ArrayList<>();
	private static final String USERNAME = "TestUser";

	private void createUsernamesList() {
		for (int i = 0; i < COUNT; i++) {
			createdAccountNames.add(USERNAME + i);
			updatedAccountNames.add(UPDATED + USERNAME + i);
		}
	}

	@Test(priority = 2)
	public void createAccounts() {
		for (String name : createdAccountNames) {
			Set<Attribute> accountAttributes = new HashSet<Attribute>();
			accountAttributes.add(AttributeBuilder.build(Name.NAME, name));
			Attribute attrPassword = AttributeBuilder.build("__PASSWORD__", password);
			accountAttributes.add(attrPassword);
			accountAttributes.add(AttributeBuilder.build("emailAddress", name + "@mail.com"));
			accountAttributes.add(AttributeBuilder.build("displayName", name + " Performance"));

			// create user:
			createdAccountIds.add(jiraConnector.create(accountObjectClass, accountAttributes, options));
		}
	}

	@Test(priority = 2)
	public void updateAccounts() {
		password = new GuardedString("secret1".toCharArray());
		int index = 0;
		for (Uid uid : createdAccountIds) {
			String name = UPDATED + createdAccountNames.get(index);
			Set<Attribute> accountAttributes = new HashSet<Attribute>();
			accountAttributes.add(AttributeBuilder.build(Name.NAME, name));
			Attribute attrPassword = AttributeBuilder.build("__PASSWORD__", password);
			accountAttributes.add(attrPassword);
			accountAttributes.add(AttributeBuilder.build("emailAddress", name + "@mail.com"));
			accountAttributes.add(AttributeBuilder.build("displayName", name + " Performance"));

			// update user:
			jiraConnector.update(accountObjectClass, uid, accountAttributes, options);
			index++;
		}
	}

	@Test(priority = 3)
	public void listAccounts() {
		Map<String, Object> operationOptions = new HashMap<String, Object>();
		operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
		operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, 1);
		operationOptions.put(OperationOptions.OP_PAGE_SIZE, 100);
		OperationOptions options = new OperationOptions(operationOptions);
		// get all groups:
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, null, accountHandler, options);
		if (accountResults.size() >= COUNT) {
			LOG.ok("\n\tListed " + accountResults.size() + " groups.\n");
		} else {
			throw new ConnectorException("Not all accounts have been listed.");
		}
	}
	
	//add/remove user(s) to/from group(s):
 
	@Test(priority = 7) 
	public void addOneUserToGroups(){ 
		Set<Attribute> accountAttributes = new HashSet<Attribute>();
		accountAttributes.add(AttributeBuilder.build("groups", createdGroupNames)); 
		jiraConnector.addAttributeValues(accountObjectClass, createdAccountIds.get(0), accountAttributes, null); 
	}
	
	@Test(priority = 8) public void removeOneUserFromGroups(){ 
		Set<Attribute> accountAttributes = new HashSet<Attribute>();
		accountAttributes.add(AttributeBuilder.build("groups", createdGroupNames));
		jiraConnector.removeAttributeValues(accountObjectClass, createdAccountIds.get(0), accountAttributes, null); 
	}
	
	@Test(priority = 9) public void addUsersToOneGroup(){ 
		for (Uid uid : createdAccountIds){ 
			Set<Attribute> accountAttributes = new HashSet<Attribute>();
			accountAttributes.add(AttributeBuilder.build("groups", createdGroupNames.get(0)));
			jiraConnector.addAttributeValues(accountObjectClass, uid, accountAttributes, null); 
			} 
		}
	 
	@Test(priority = 10) public void removeUsersFromOneGroup(){ 
		for (Uid uid : createdAccountIds){ 
			Set<Attribute> accountAttributes = new HashSet<Attribute>(); 
			accountAttributes.add(AttributeBuilder.build("groups", createdGroupNames.get(0)));
			jiraConnector.removeAttributeValues(accountObjectClass, uid, accountAttributes, null); 
		} 
	}
	 
	@Test(priority = 11) public void addUsersToGroups(){ 
		for (Uid uid : createdAccountIds){ Set<Attribute> accountAttributes = new HashSet<Attribute>();
			accountAttributes.add(AttributeBuilder.build("groups", createdGroupNames)); 
			jiraConnector.addAttributeValues(accountObjectClass, uid, accountAttributes, null); 
		} 
	}
	 
	@Test(priority = 12) public void removeUsersFromGroups(){ 
		for (Uid uid : createdAccountIds){ 
			Set<Attribute> accountAttributes = new HashSet<Attribute>();
			accountAttributes.add(AttributeBuilder.build("groups", createdGroupNames)); 
			jiraConnector.removeAttributeValues(accountObjectClass, uid, accountAttributes, null); 
		} 
	}
	 
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~GROUP OBJECT CLASS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private List<Uid> createdGroupIds = new ArrayList<>();
	private List<String> createdGroupNames = new ArrayList<>();
	private static final String GROUPNAME = "TestGroup";

	private void createGroupNamesList() {
		for (int i = 0; i < COUNT; i++) {
			createdGroupNames.add(GROUPNAME + i);
		}
	}

	@Test(priority = 4)
	public void createGroups() {
		for (String name : createdGroupNames) {
			Set<Attribute> groupAttributes = new HashSet<Attribute>();
			groupAttributes.add(AttributeBuilder.build(Name.NAME, name));
			// create group:
			createdGroupIds.add(jiraConnector.create(groupObjectClass, groupAttributes, options));
		}
	}

	@Test(priority = 5)
	public void listGroups() {
		// get all groups:
		groupResults.clear();
		jiraConnector.executeQuery(groupObjectClass, null, groupHandler, options);
		if (groupResults.size() >= COUNT) {
			LOG.ok("\n\tListed " + groupResults.size() + " groups.\n");
		} else {
			throw new ConnectorException("Not all groups have been listed.");
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~PROJECT OBJECT CLASS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private List<Uid> createdProjectIds = new ArrayList<>();
	private List<String> createdProjectNames = new ArrayList<>();
	private List<String> createdProjectTypeKeys = new ArrayList<>();
	private static final String PROJECT_NAME = "TestProject";
	private static final String PROJECT_TYPE_KEY = "TSTPR";

	private void createProjectNamesList() {
		for (int i = 0; i < COUNT; i++) {
			createdProjectNames.add(PROJECT_NAME + i);
			createdProjectTypeKeys.add(PROJECT_TYPE_KEY + i);
		}
	}

	@Test(priority = 13)
	public void createProjects() {
		for (int i = 0; i < COUNT; i++) {
			String name = createdProjectNames.get(i);
			String typeKey = createdProjectTypeKeys.get(i);
			Set<Attribute> projectAttributes = new HashSet<Attribute>();
			projectAttributes.add(AttributeBuilder.build(Name.NAME, name));
			projectAttributes.add(AttributeBuilder.build("key", typeKey));
			projectAttributes.add(AttributeBuilder.build("projectTypeKey", "business"));
			projectAttributes.add(AttributeBuilder.build("lead", "administrator"));
			// create project:
			createdProjectIds.add(jiraConnector.create(projectObjectClass, projectAttributes, options));
		}
	}

	@Test(priority = 14)
	public void updateProjects() {
		password = new GuardedString("secret1".toCharArray());
		int index = 0;
		for (Uid uid : createdProjectIds) {
			String name = UPDATED + createdProjectNames.get(index);
			String typeKey = "UPDT" + createdProjectTypeKeys.get(index);
			Set<Attribute> projectAttributes = new HashSet<Attribute>();
			projectAttributes.add(AttributeBuilder.build(Name.NAME, name));
			projectAttributes.add(AttributeBuilder.build("key", typeKey));
			projectAttributes.add(AttributeBuilder.build("projectTypeKey", "software"));
			projectAttributes.add(AttributeBuilder.build("lead", UPDATED + createdAccountNames.get(index)));
			// update project:
			jiraConnector.update(projectObjectClass, uid, projectAttributes, options);
			index++;
		}
	}

	@Test(priority = 15)
	public void listProjects() {
		// get all projects:
		projectResults.clear();
		jiraConnector.executeQuery(projectObjectClass, null, projectHandler, options);
		if (projectResults.size() >= COUNT) {
			LOG.ok("\n\tListed " + projectResults.size() + " projects.\n");
		} else {
			throw new ConnectorException("Not all projects have been listed.");
		}
	}

	@Test(priority = 15)
	public void addOneActorToProjects() {
		Set<Attribute> projectAttributes = new HashSet<Attribute>();
		projectAttributes.add(AttributeBuilder.build("Developers.groups", createdGroupNames.get(0)));
		projectAttributes.add(AttributeBuilder.build("Administrators.groups", createdGroupNames.get(0)));
		projectAttributes.add(AttributeBuilder.build("Developers.users", updatedAccountNames.get(0)));
		projectAttributes.add(AttributeBuilder.build("Administrators.users", updatedAccountNames.get(0)));
		for (Uid uid : createdProjectIds) {
			jiraConnector.addAttributeValues(projectObjectClass, uid, projectAttributes, null);
		}
	}

	@Test(priority = 16)
	public void removeOneActorFromProjects() {
		Set<Attribute> projectAttributes = new HashSet<Attribute>();
		projectAttributes.add(AttributeBuilder.build("Developers.groups", createdGroupNames.get(0)));
		projectAttributes.add(AttributeBuilder.build("Administrators.groups", createdGroupNames.get(0)));
		projectAttributes.add(AttributeBuilder.build("Developers.users", updatedAccountNames.get(0)));
		projectAttributes.add(AttributeBuilder.build("Administrators.users", updatedAccountNames.get(0)));
		for (Uid uid : createdProjectIds) {
			jiraConnector.removeAttributeValues(projectObjectClass, uid, projectAttributes, null);
		}
	}

	@Test(priority = 17)
	public void addActorsToOneProject() {
		Set<Attribute> projectAttributes = new HashSet<Attribute>();
		projectAttributes.add(AttributeBuilder.build("Developers.groups", createdGroupNames));
		projectAttributes.add(AttributeBuilder.build("Administrators.groups", createdGroupNames));
		projectAttributes.add(AttributeBuilder.build("Developers.users", updatedAccountNames));
		projectAttributes.add(AttributeBuilder.build("Administrators.users", updatedAccountNames));
		jiraConnector.addAttributeValues(projectObjectClass, createdProjectIds.get(0), projectAttributes, null);
	}

	@Test(priority = 18)
	public void removeActorsFromOneProject() {
		Set<Attribute> projectAttributes = new HashSet<Attribute>();
		projectAttributes.add(AttributeBuilder.build("Developers.groups", createdGroupNames));
		projectAttributes.add(AttributeBuilder.build("Administrators.groups", createdGroupNames));
		projectAttributes.add(AttributeBuilder.build("Developers.users", updatedAccountNames));
		projectAttributes.add(AttributeBuilder.build("Administrators.users", updatedAccountNames));
		jiraConnector.removeAttributeValues(projectObjectClass, createdProjectIds.get(0), projectAttributes, null);
	}

	@Test(priority = 19)
	public void addActorsToProjects() {
		Set<Attribute> projectAttributes = new HashSet<Attribute>();
		projectAttributes.add(AttributeBuilder.build("Developers.groups", createdGroupNames.get(0)));
		projectAttributes.add(AttributeBuilder.build("Administrators.groups", createdGroupNames.get(1)));
		projectAttributes.add(AttributeBuilder.build("Developers.users", updatedAccountNames.get(0)));
		projectAttributes.add(AttributeBuilder.build("Administrators.users", updatedAccountNames.get(1)));
		for (Uid uid : createdProjectIds) {
			jiraConnector.addAttributeValues(projectObjectClass, uid, projectAttributes, null);
		}
	}

	@Test(priority = 20)
	public void removeActorsFromProjects() {
		Set<Attribute> projectAttributes = new HashSet<Attribute>();
		projectAttributes.add(AttributeBuilder.build("Developers.groups", createdGroupNames));
		projectAttributes.add(AttributeBuilder.build("Administrators.groups", createdGroupNames));
		projectAttributes.add(AttributeBuilder.build("Developers.users", updatedAccountNames));
		projectAttributes.add(AttributeBuilder.build("Administrators.users", updatedAccountNames));
		for (Uid uid : createdProjectIds) {
			jiraConnector.removeAttributeValues(projectObjectClass, uid, projectAttributes, null);
		}
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~DELETE OBJECTS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test(priority = 35)
	public void deleteAccounts() {
		for (Uid uid : createdAccountIds) {
			// for (int i=1; i<count; i++){
			// Uid uid = new Uid("TestUser"+i);
			// delete user:
			jiraConnector.delete(accountObjectClass, uid, options);
		}
	}

	@Test(priority = 30)
	public void deleteGroups() {
		for (Uid uid : createdGroupIds) {
			// for (int i=0; i<count; i++){
			// Uid uid = new Uid("TestGroup"+i);
			// delete group
			jiraConnector.delete(groupObjectClass, uid, options);
		}
	}

	@Test(priority = 25)
	public void deleteProjects() {
		for (Uid uid : createdProjectIds) {
			// for (int i=1; i<count; i++){
			// Uid uid = new Uid("TestUser"+i);
			// delete project:
			jiraConnector.delete(projectObjectClass, uid, options);
		}
	}

	@Test(priority = 50)
	public void disposeTest() {
		jiraConnector.dispose();
	}

	private void projectsCleanUp() {
		projectResults.clear();
		jiraConnector.executeQuery(projectObjectClass, null, projectHandler, options);

		for (int i = 0; i < projectResults.size(); i++) {
			ConnectorObject project = projectResults.get(i);
			String name = (String) project.getAttributeByName(Name.NAME).getValue().get(0);
			for (String createdName : createdProjectNames) {
				if (name.equals(createdName) || name.equals(UPDATED + createdName)) {
					// delete project used for create and update tests:
					String uid = (String) project.getAttributeByName(Uid.NAME).getValue().get(0);
					if (uid != null) {
						jiraConnector.delete(projectObjectClass, new Uid(uid), null);
					}
				}
			}
		}
	}
	private void accountsCleanUp() {
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, null, accountHandler, options);

		for (int i = 0; i < accountResults.size(); i++) {
			ConnectorObject account = accountResults.get(i);
			String name = (String) account.getAttributeByName(Name.NAME).getValue().get(0);
			for (int j=0; j<COUNT; j++) {
				String createdName = createdAccountNames.get(j);
				String updatedName = updatedAccountNames.get(j);
				if (name.equals(createdName) || name.equals(updatedName)) {
					// delete account used for create and update tests:
					String uid = (String) account.getAttributeByName(Uid.NAME).getValue().get(0);
					if (uid != null) {
						jiraConnector.delete(accountObjectClass, new Uid(uid), null);
					}
				}
			}
		}
	}
	private void groupsCleanUp() {
		groupResults.clear();
		jiraConnector.executeQuery(groupObjectClass, null, groupHandler, options);

		for (int i = 0; i < groupResults.size(); i++) {
			ConnectorObject group = groupResults.get(i);
			String name = (String) group.getAttributeByName(Name.NAME).getValue().get(0);
			for (String createdName : createdGroupNames) {
				if (name.equals(createdName)) {
					// delete group used for create and update tests:
					String uid = (String) group.getAttributeByName(Uid.NAME).getValue().get(0);
					if (uid != null) {
						jiraConnector.delete(groupObjectClass, new Uid(uid), null);
					}
				}
			}
		}
	}
}
