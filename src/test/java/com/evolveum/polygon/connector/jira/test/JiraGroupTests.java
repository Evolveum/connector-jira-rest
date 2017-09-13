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
import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.testng.annotations.Test;

/**
 * @author surmanek
 *
 */
public class JiraGroupTests extends JiraTestHelper {

	private static final Set<Attribute> groupAttributes = new HashSet<Attribute>();
	private static final String testGroupname = "ľščťžýáíéäúôöüß$#@%^*(?)<&>";
	private Uid groupUid;

	@Test(priority = 1)
	public void initTest() {
		jiraConnector.init(getConfiguration());
		cleanUp();
	}

	@Test(priority = 2)
	public void groupSchemaTest() {
		Schema schema = jiraConnector.schema();

		Set<AttributeInfo> groupAttributesInfo = schema.findObjectClassInfo(ObjectClass.GROUP_NAME).getAttributeInfo();
		Set<Attribute> groupAttributes = new HashSet<Attribute>();

		for (AttributeInfo attributeInfo : groupAttributesInfo) {
			if (!attributeInfo.isMultiValued() && attributeInfo.isCreateable() && attributeInfo.isReadable()) {
				if (attributeInfo.getType().equals(String.class)) {
					groupAttributes.add(AttributeBuilder.build(attributeInfo.getName(), testGroupname));
				}
			}
		}

		groupUid = jiraConnector.create(groupObjectClass, groupAttributes, options);

		AttributeFilter groupEqualsFilter = (EqualsFilter) FilterBuilder.equalTo(groupUid);
		groupResults.clear();
		jiraConnector.executeQuery(groupObjectClass, groupEqualsFilter, groupHandler, options);

		try {
			if (!groupResults.get(0).getAttributes().containsAll(groupAttributes)) {
				throw new InvalidAttributeValueException(
						"Attributes of created group and searched group DO NOT CORRESPOND.");
			} else
				LOG.ok("\n---------------------------------------------------------------------"
						+ "\n\tPASSED: Attributes of created group and searched group CORRESPOND"
						+ "\n---------------------------------------------------------------------");
		} finally {
			jiraConnector.delete(groupObjectClass, groupUid, options);
		}
	}

	@Test(priority = 3)
	public void createGroupTest() {
		// mandatory attribute:
		groupAttributes.add(AttributeBuilder.build(Name.NAME, testGroupname));

		// create group:
		groupUid = jiraConnector.create(groupObjectClass, groupAttributes, options);

		// query group:
		AttributeFilter equalsFilter = (EqualsFilter) FilterBuilder
				.equalTo(AttributeBuilder.build(Uid.NAME, groupUid.getUidValue()));
		groupResults.clear();
		jiraConnector.executeQuery(groupObjectClass, equalsFilter, groupHandler, options);

		if (!groupResults.get(0).getAttributes().containsAll(groupAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created group and searched group DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------"
					+ "\n\tPASSED: Attributes of created group and searched group CORRESPOND"
					+ "\n-------------------------------------------------------------------------");
	}

	@Test(priority = 3, expectedExceptions = IllegalArgumentException.class)
	public void unknownGroupUidNegativeTest() {
		AttributeFilter equalsFilter = (EqualsFilter) FilterBuilder
				.equalTo(AttributeBuilder.build(Uid.NAME, "XXXXXXXXX"));
		groupResults.clear();
		jiraConnector.executeQuery(groupObjectClass, equalsFilter, groupHandler, options);
	}

	@Test(priority = 4, expectedExceptions = IllegalArgumentException.class)
	public void createDuplicateGroupNegativeTest() {
		// create duplicate group:
		jiraConnector.create(groupObjectClass, groupAttributes, options);
	}

	@Test(priority = 5)
	public void uidEqualsFilteringForGroupsTest() {
		// filtering:
		AttributeFilter groupEqualsFilter = (EqualsFilter) FilterBuilder
				.equalTo(AttributeBuilder.build(Uid.NAME, groupUid.getUidValue()));

		groupResults.clear();
		jiraConnector.executeQuery(groupObjectClass, groupEqualsFilter, groupHandler, options);

		if (!groupResults.get(0).getAttributes().containsAll(groupAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------"
					+ "\n\tPASSED: Attributes of created account and searched account CORRESPOND "
					+ "\n-------------------------------------------------------------------------");
	}

	@Test(priority = 5)
	public void nameEqualsFilteringForGroupsTest() {
		// filtering:
		groupResults.clear();
		AttributeFilter groupEqualsFilter = (EqualsFilter) FilterBuilder
				.equalTo(AttributeBuilder.build(Name.NAME, "ľščťžýáíéäúôöüß$#@%^*(?)<&>"));

		jiraConnector.executeQuery(groupObjectClass, groupEqualsFilter, groupHandler, options);

		if (!groupResults.get(0).getAttributes().containsAll(groupAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------"
					+ "\n\tPASSED: Attributes of created account and searched account CORRESPOND"
					+ "\n-------------------------------------------------------------------------");

	}

	@Test(priority = 5)
	public void nameContainsFilteringForGroupsTest() {
		// filtering:
		AttributeFilter groupContainsFilter = (ContainsFilter) FilterBuilder
				.contains(AttributeBuilder.build(Name.NAME, "áíéäúô"));
		groupResults.clear();
		jiraConnector.executeQuery(groupObjectClass, groupContainsFilter, groupHandler, options);

		if (!groupResults.get(0).getAttributes().containsAll(groupAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------"
					+ "\n\tPASSED: Attributes of created account and searched account CORRESPOND"
					+ "\n-------------------------------------------------------------------------");
	}

	@Test(priority = 6)
	public void listingGroupsTest() {
		// filtering:
		groupResults.clear();
		jiraConnector.executeQuery(groupObjectClass, null, groupHandler, options);

		LOG.ok("\n\n\tListed " + groupResults.size() + " groups.\n");

	}

	@Test(priority = 15)
	public void deleteGroupTest() {
		if (groupUid != null) {
			jiraConnector.delete(groupObjectClass, groupUid, null);
		}
	}

	@Test(priority = 20)
	public void disposeTest() {
		jiraConnector.dispose();
	}

	// cleanup before running tests:
	private void cleanUp() {
		groupResults.clear();
		jiraConnector.executeQuery(groupObjectClass, null, groupHandler, options);

		for (ConnectorObject group : groupResults) {
			String name = (String) group.getAttributeByName(Name.NAME).getValue().get(0);
			if (name.equals(testGroupname)) {
				// delete group used for create test:	
				jiraConnector.delete(groupObjectClass, new Uid(name), null);
			}
		}
	}
}
