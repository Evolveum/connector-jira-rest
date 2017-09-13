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
import java.util.Set;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.annotations.Test;

/**
 * @author surmanek
 *
 */
public class JiraProjectTests extends JiraTestHelper {

	private static final Set<Attribute> projectAttributes = new HashSet<Attribute>();
	private static final String testProjectname = "ľščťžýáíéäúôöüß$#@%^*(?)<&>";
	private static final String updatedTestProjectname = "XXľščťžýáíéäúôöüß$#@%^*(?)<&>XX";
	private Uid projectUid;
	private Attribute attrLead = AttributeBuilder.build("lead", "administrator");

	@Test(priority = 1)
	public void initTest() {
		jiraConnector.init(getConfiguration());
		cleanUp();
	}

	@Test(priority = 2)
	public void projectSchemaTest() {
		Schema schema = jiraConnector.schema();
		OperationOptions options = new OperationOptions(new HashMap<String, Object>());

		Set<AttributeInfo> projectAttributesInfo = schema.findObjectClassInfo("PROJECT").getAttributeInfo();
		Set<Attribute> projectAttributes = new HashSet<Attribute>();

		for (AttributeInfo attributeInfo : projectAttributesInfo) {
			if (!attributeInfo.isMultiValued() && attributeInfo.isCreateable() && attributeInfo.isReadable()) {
				if (attributeInfo.getName().equals("key")) {
					projectAttributes.add(AttributeBuilder.build(attributeInfo.getName(), "TPROJ"));
				} else if (attributeInfo.getName().equals("lead")) {
					projectAttributes.add(AttributeBuilder.build(attributeInfo.getName(), "administrator"));
				} else if (attributeInfo.getName().equals("projectTypeKey")) {
					projectAttributes.add(AttributeBuilder.build(attributeInfo.getName(), "software"));
				} else if (attributeInfo.getName().equals("description")) {
					projectAttributes.add(AttributeBuilder.build(attributeInfo.getName(), "This is Test Project."));
				} else if (attributeInfo.getName().equals("url")) {
					projectAttributes.add(AttributeBuilder.build(attributeInfo.getName(), "http://test.com"));
				} else if (attributeInfo.getName().equals("assigneeType")) {
					projectAttributes.add(AttributeBuilder.build(attributeInfo.getName(), "UNASSIGNED"));
				} else if (attributeInfo.getType().equals(String.class)) {
					projectAttributes.add(AttributeBuilder.build(attributeInfo.getName(), testProjectname));
				}
			}
		}

		Uid projectUid = jiraConnector.create(projectObjectClass, projectAttributes, options);

		AttributeFilter projectEqualsFilter;
		projectEqualsFilter = (EqualsFilter) FilterBuilder.equalTo(projectUid);

		final ArrayList<ConnectorObject> projectResults = new ArrayList<>();
		SearchResultsHandler handlerProject = new SearchResultsHandler() {

			@Override
			public boolean handle(ConnectorObject connectorObject) {
				projectResults.add(connectorObject);
				return true;
			}

			@Override
			public void handleResult(SearchResult result) {
			}
		};

		jiraConnector.executeQuery(projectObjectClass, projectEqualsFilter, handlerProject, options);

		try {
			if (!projectResults.get(0).getAttributes().containsAll(projectAttributes)) {
				throw new InvalidAttributeValueException(
						"Attributes of created project and searched project DO NOT CORRESPOND.");
			} 
		} finally {
			jiraConnector.delete(projectObjectClass, projectUid, options);
		}
	}

	@Test(priority = 3)
	public void createProjectTest() {
		// mandatory attributes:
		projectAttributes.add(AttributeBuilder.build(Name.NAME, testProjectname));
		projectAttributes.add(AttributeBuilder.build("key", "TEST"));
		projectAttributes.add(AttributeBuilder.build("projectTypeKey", "business"));

		projectAttributes.add(attrLead);

		// create project:
		projectUid = jiraConnector.create(projectObjectClass, projectAttributes, options);

		// query user:
		AttributeFilter equalsFilter = (EqualsFilter) FilterBuilder
				.equalTo(AttributeBuilder.build(Uid.NAME, projectUid.getUidValue()));
		projectResults.clear();
		jiraConnector.executeQuery(projectObjectClass, equalsFilter, projectHandler, options);

		if (!projectResults.get(0).getAttributes().containsAll(projectAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created project and searched project DO NOT CORRESPOND.");
		} 
	}

	@Test(priority = 3, expectedExceptions = IllegalArgumentException.class)
	public void unknownProjectUidNegativeTest() {
		AttributeFilter equalsFilter = (EqualsFilter) FilterBuilder
				.equalTo(AttributeBuilder.build(Uid.NAME, "XXXXXXXXX"));
		projectResults.clear();
		jiraConnector.executeQuery(projectObjectClass, equalsFilter, projectHandler, options);
	}

	@Test(priority = 4, expectedExceptions = IllegalArgumentException.class)
	public void createDuplicateProjectNegativeTest() {
		// create duplicate user:
		jiraConnector.create(projectObjectClass, projectAttributes, options);
	}

	@Test(priority = 4)
	public void updateProjectTest() {
		// update created project:
		projectAttributes.clear();
		projectAttributes.add(AttributeBuilder.build(Name.NAME, updatedTestProjectname));
		projectAttributes.add(AttributeBuilder.build("key", "TEST1"));
		projectAttributes.add(AttributeBuilder.build("projectTypeKey", "software"));

		jiraConnector.update(projectObjectClass, projectUid, projectAttributes, null);

		// query project:
		projectResults.clear();
		AttributeFilter equalsFilter = (EqualsFilter) FilterBuilder
				.equalTo(AttributeBuilder.build(Uid.NAME, projectUid.getUidValue()));
		jiraConnector.executeQuery(projectObjectClass, equalsFilter, projectHandler, options);

		if (!projectResults.get(0).getAttributes().containsAll(projectAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created project and searched project DO NOT CORRESPOND.");
		} 
	}

	@Test(priority = 5)
	public void uidEqualsFilteringForProjectsTest() {
		// filtering:
		AttributeFilter projectEqualsFilter = (EqualsFilter) FilterBuilder
				.equalTo(AttributeBuilder.build(Uid.NAME, projectUid.getUidValue()));

		projectResults.clear();
		jiraConnector.executeQuery(projectObjectClass, projectEqualsFilter, projectHandler, options);

		if (!projectResults.get(0).getAttributes().containsAll(projectAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} 
	}

	@Test(priority = 5)
	public void nameContainsFilteringForProjectsTest() {
		// filtering:
		AttributeFilter projectContainsFilter = (ContainsFilter) FilterBuilder
				.contains(AttributeBuilder.build(Name.NAME, "ľščťžý"));
		projectResults.clear();
		jiraConnector.executeQuery(projectObjectClass, projectContainsFilter, projectHandler, options);
		if (projectAttributes.contains(attrLead)) {
			projectAttributes.remove(attrLead);
		}

		if (!projectResults.get(0).getAttributes().containsAll(projectAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} 

	}

	@Test(priority = 5)
	public void keyContainsFilteringForProjectsTest() {
		// filtering:
		AttributeFilter projectContainsFilter = (ContainsFilter) FilterBuilder
				.contains(AttributeBuilder.build("key", "TES"));
		projectResults.clear();
		jiraConnector.executeQuery(projectObjectClass, projectContainsFilter, projectHandler, options);
		if (projectAttributes.contains(attrLead)) {
			projectAttributes.remove(attrLead);
		}

		if (!projectResults.get(0).getAttributes().containsAll(projectAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} 

	}

	@Test(priority = 5)
	public void projectTypeKeyContainsFilteringForProjectsTest() {
		// filtering:
		AttributeFilter projectContainsFilter = (ContainsFilter) FilterBuilder
				.contains(AttributeBuilder.build("projectTypeKey", "soft"));
		projectResults.clear();
		jiraConnector.executeQuery(projectObjectClass, projectContainsFilter, projectHandler, options);
		if (projectAttributes.contains(attrLead)) {
			projectAttributes.remove(attrLead);
		}
		Boolean contains = false;
		for (ConnectorObject project : projectResults){
			String name = (String)project.getAttributeByName(Name.NAME).getValue().get(0);
			if (name.equals(updatedTestProjectname)){
				contains = true;
				if (!project.getAttributes().containsAll(projectAttributes)) {
					throw new InvalidAttributeValueException("Attributes of created account and searched account DO NOT CORRESPOND.");
				} 
			}
		}
		if (!contains){
			throw new ConnectorException("Expected project was not returned by filtering.");
		}
	}

	@Test(priority = 6)
	public void listingProjectsTest() {
		// filtering:
		projectResults.clear();
		jiraConnector.executeQuery(projectObjectClass, null, projectHandler, options);

		LOG.ok("\n\n\tListed " + projectResults.size() + " projects.\n");
	}

	@Test(priority = 15)
	public void deleteProjectTest() {
		if (projectUid != null) {
			jiraConnector.delete(projectObjectClass, projectUid, null);
		}
	}

	@Test(priority = 20)
	public void disposeTest() {
		jiraConnector.dispose();
	}
	
	//cleanup before running tests:
		private void cleanUp(){
			projectResults.clear();
			
			jiraConnector.executeQuery(projectObjectClass, null, projectHandler, options);

			for (ConnectorObject project : projectResults){
				String name = (String)project.getAttributeByName(Name.NAME).getValue().get(0);
				if(name.equals(testProjectname) || name.equals(updatedTestProjectname)){
					//delete project used for create and update tests:
					String uid = (String)project.getAttributeByName(Uid.NAME).getValue().get(0);
					if (uid!=null){
						jiraConnector.delete(projectObjectClass, new Uid(uid), null);
					}
				}
			}
		}

}
