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
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.annotations.Test;

/**
 * @author surmanek
 *
 */
public class JiraAccountTests extends JiraTestHelper{
	
	private Uid accountUid;
	private static final String testUsername = "ľščťžýáíéäúôöüß$#@%^*(?)";
	private static final String updatedTestUsername = "XXľščťžýáíéäúôöüß$#@%^*(?)XX";
	private static final Set<Attribute> accountAttributes = new HashSet<Attribute>();
	
	@Test(priority = 1)
	public void initTest() {
		jiraConnector.init(getConfiguration());
		cleanUp();
	}
	
	@Test(priority = 2)
	public void accountSchemaTest(){
		Schema schema = jiraConnector.schema();
		OperationOptions options = new OperationOptions(new HashMap<String,Object>());
		
		Set<AttributeInfo> accountAttributesInfo = schema.findObjectClassInfo(ObjectClass.ACCOUNT_NAME).getAttributeInfo();
		Set<Attribute> accountAttributes = new HashSet<Attribute>();
		
		for(AttributeInfo attributeInfo : accountAttributesInfo){
			if(!attributeInfo.isMultiValued() && attributeInfo.isCreateable() && attributeInfo.isReadable()){
				if(attributeInfo.getName().equals("emailAddress")){
					accountAttributes.add(AttributeBuilder.build(attributeInfo.getName(),"test_user@mail.com"));
				} else if(attributeInfo.getName().equals("locale")){
					accountAttributes.add(AttributeBuilder.build(attributeInfo.getName(),"en_US"));	
				} else if(attributeInfo.getName().equals("timeZone")){
					accountAttributes.add(AttributeBuilder.build(attributeInfo.getName(),"Europe/Bratislava"));	
				} else if(attributeInfo.getType().equals(String.class)){
					accountAttributes.add(AttributeBuilder.build(attributeInfo.getName(), testUsername));
				} 
			}
		}
		
		Attribute passwdAttr = AttributeBuilder.build("__PASSWORD__", password);
		accountAttributes.add(passwdAttr);
		
		accountUid = jiraConnector.create(accountObjectClass, accountAttributes, options);
		
		AttributeFilter accountEqualsFilter;
		accountEqualsFilter = (EqualsFilter) FilterBuilder.equalTo(accountUid);
		
		final ArrayList<ConnectorObject> accountResults = new ArrayList<>();
		SearchResultsHandler handlerAccount = new SearchResultsHandler() {

			@Override
			public boolean handle(ConnectorObject connectorObject) {
				accountResults.add(connectorObject);
				return true;
			}

			@Override
			public void handleResult(SearchResult result) {
			}
		};
		
		jiraConnector.executeQuery(accountObjectClass, accountEqualsFilter, handlerAccount, options);
		//remove password from attributes set because it is mandatory but note returned after get request:
		accountAttributes.remove(passwdAttr);
		
		try {
			if(!accountResults.get(0).getAttributes().containsAll(accountAttributes)){
				throw new InvalidAttributeValueException("Attributes of created account and searched account DO NOT CORRESPOND.");
			}
			else LOG.ok("\n-------------------------------------------------------------------------"
					+ "\n\tPASSED: Attributes of created account and searched account CORRESPOND"
					+ "\n-------------------------------------------------------------------------");
		} finally {
			jiraConnector.delete(accountObjectClass, accountUid, options);
		}
 }
	
	@Test(priority = 3)
	public void createAccountTest(){
		//mandatory attributes:
		accountAttributes.add(AttributeBuilder.build(Name.NAME,testUsername));
		Attribute attrPassword = AttributeBuilder.build("__PASSWORD__", password);
		accountAttributes.add(attrPassword);
		accountAttributes.add(AttributeBuilder.build("emailAddress","mymail@mail.com"));
		accountAttributes.add(AttributeBuilder.build("displayName","ľščťžýáíé ľščťžýáíéäúôöüß$#@%^*(?)"));
	 
		//create user:
		accountUid = jiraConnector.create(accountObjectClass, accountAttributes, options);
		//query user:
		AttributeFilter equalsFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Uid.NAME, accountUid.getUidValue()));
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, equalsFilter, accountHandler, options);
		
		//remove password from attributes set because it is mandatory but note returned after get request:
		accountAttributes.remove(attrPassword);
		
		if(!accountResults.get(0).getAttributes().containsAll(accountAttributes)){
			throw new InvalidAttributeValueException("Attributes of created account and searched account DO NOT CORRESPOND.");
		}
		else LOG.ok("\n-------------------------------------------------------------------------"
				+ "\n\tPASSED: Attributes of created account and searched account CORRESPOND"
				+ "\n\t-------------------------------------------------------------------------");
	 }
	
	@Test(priority = 3, expectedExceptions = IllegalArgumentException.class)
	public void unknownAccountUidNegativeTest() {
		AttributeFilter equalsFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Uid.NAME, "XXXXXXXXX"));
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, equalsFilter, accountHandler, options);
	}
	
	@Test(priority = 4, expectedExceptions = IllegalArgumentException.class)
	public void createDuplicateAccountNegativeTest(){
		//create duplicate user:
		jiraConnector.create(accountObjectClass, accountAttributes, options);
	 }
	
	@Test(priority = 4)
	public void updateAccountTest(){
		
		//update created account:
		accountAttributes.clear();
		accountAttributes.add(AttributeBuilder.build(Name.NAME, updatedTestUsername));
		accountAttributes.add(AttributeBuilder.build("emailAddress","mymail1@mail.com"));
		accountAttributes.add(AttributeBuilder.build("displayName","ľščťžýáíé ľščťžýáíéäúôöüß$#@%^*(?)1"));
		password = new GuardedString("secret1".toCharArray());
		Attribute attrPassword = AttributeBuilder.build("__PASSWORD__", password);
		accountAttributes.add(attrPassword);
		
		jiraConnector.update(accountObjectClass, accountUid, accountAttributes, null);
		
		//query user:
		accountResults.clear();
		AttributeFilter equalsFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Uid.NAME, accountUid.getUidValue()));
		jiraConnector.executeQuery(accountObjectClass, equalsFilter, accountHandler, options);
		
		//remove password from attributes set because it is mandatory but note returned after get request:
		accountAttributes.remove(attrPassword);
		
		if(!accountResults.get(0).getAttributes().containsAll(accountAttributes)){
			throw new InvalidAttributeValueException("Attributes of created account and searched account DO NOT CORRESPOND.");
		}
		else LOG.ok("\n-------------------------------------------------------------------------"
				+ "\n\tPASSED: Attributes of created account and searched account CORRESPOND"
				+ "\n-------------------------------------------------------------------------");
	 }
	
	@Test(priority = 5)
	public void uidEqualsFilteringForAccountsTest() {
		// filtering:
		AttributeFilter accountEqualsFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Uid.NAME, accountUid.getUidValue() ));
		
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, accountEqualsFilter, accountHandler, options);

		if (!accountResults.get(0).getAttributes().containsAll(accountAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------\n\tPASSED: Attributes of created account and searched account CORRESPOND \n-------------------------------------------------------------------------");
	}
	
	@Test(priority = 5)
	public void nameEqualsFilteringForAccountsTest() {
		// filtering:
		accountResults.clear();
		AttributeFilter accountEqualsFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME, updatedTestUsername));
		
		jiraConnector.executeQuery(accountObjectClass, accountEqualsFilter, accountHandler, options);

		if (!accountResults.get(0).getAttributes().containsAll(accountAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------\n\tPASSED: Attributes of created account and searched account CORRESPOND \n-------------------------------------------------------------------------");
		
	}
	
	@Test(priority = 5)
	public void nameStartsWithFilteringForAccountsTest() {
		// filtering:
		AttributeFilter accountStartsWithFilter = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build(Name.NAME, "ľščťž"));
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, accountStartsWithFilter, accountHandler, options);

		if (!accountResults.get(0).getAttributes().containsAll(accountAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------\n\tPASSED: Attributes of created account and searched account CORRESPOND \n-------------------------------------------------------------------------");
		
	}
	
	@Test(priority = 5)
	public void displaynameStartsWithFilteringForAccountsTest() {
		// filtering:
		AttributeFilter accountStartsWithFilter = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("displayName","ľščťžýáíé ľšč"));
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, accountStartsWithFilter, accountHandler, options);

		if (!accountResults.get(0).getAttributes().containsAll(accountAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------\n\tPASSED: Attributes of created account and searched account CORRESPOND \n-------------------------------------------------------------------------");
		
	}
	
	@Test(priority = 5)
	public void emailStartsWithFilteringForAccountsTest() {
		// filtering:
		AttributeFilter accountStartsWithFilter = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("emailAddress","mym"));
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, accountStartsWithFilter, accountHandler, options);

		if (!accountResults.get(0).getAttributes().containsAll(accountAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------\n\tPASSED: Attributes of created account and searched account CORRESPOND |\n-------------------------------------------------------------------------");
		
	}
	
	
	@Test(priority = 5)
	public void nameContainsFilteringForAccountsTest() {
		// filtering:
		AttributeFilter accountStartsWithFilter = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build(Name.NAME, "ľščťž"));
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, accountStartsWithFilter, accountHandler, options);

		if (!accountResults.get(0).getAttributes().containsAll(accountAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------\n\tPASSED: Attributes of created account and searched account CORRESPOND \n-------------------------------------------------------------------------");
		
	}
	
	@Test(priority = 5)
	public void displaynameContainsFilteringForAccountsTest() {
		// filtering:
		AttributeFilter accountStartsWithFilter = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName","ľščťž"));
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, accountStartsWithFilter, accountHandler, options);

		if (!accountResults.get(0).getAttributes().containsAll(accountAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------\n\tPASSED: Attributes of created account and searched account CORRESPOND \n-------------------------------------------------------------------------");
		
	}
	
	@Test(priority = 5)
	public void emailContainsFilteringForAccountsTest() {
		// filtering:
		AttributeFilter accountStartsWithFilter = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("emailAddress","mym"));
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, accountStartsWithFilter, accountHandler, options);

		if (!accountResults.get(0).getAttributes().containsAll(accountAttributes)) {
			throw new InvalidAttributeValueException(
					"Attributes of created account and searched account DO NOT CORRESPOND.");
		} else
			LOG.ok("\n-------------------------------------------------------------------------\n\tPASSED: Attributes of created account and searched account CORRESPOND |\n-------------------------------------------------------------------------");
		
	}
	
	@Test(priority = 6)
	public void listingAccountsTest() {
		Map<String, Object> operationOptions = new HashMap<String, Object>();
		operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
		operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, 2);
		operationOptions.put(OperationOptions.OP_PAGE_SIZE, 13);
		OperationOptions options = new OperationOptions(operationOptions);
		// filtering:
		accountResults.clear();
		jiraConnector.executeQuery(accountObjectClass, null, accountHandler, options);

		LOG.ok("\n\n\tListed "+accountResults.size()+" accounts.\n");

	}

	
	@Test(priority = 15)
	public void deleteAccountTest() {
		if (accountUid!=null){
			jiraConnector.delete(accountObjectClass, accountUid, options);
		}
	}
	
	@Test(priority = 20)
	public void disposeTest() {
		 jiraConnector.dispose();
	}
	
	//cleanup before running tests:
	private void cleanUp(){
		accountResults.clear();
		
		jiraConnector.executeQuery(accountObjectClass, null, accountHandler, options);

		for (ConnectorObject user : accountResults){
			String name = (String)user.getAttributeByName(Name.NAME).getValue().get(0);
			if(name.equals(testUsername) || name.equals(updatedTestUsername)){
				//delete user used for create and update tests:
				String uid = (String)user.getAttributeByName(Uid.NAME).getValue().get(0);
				if (uid!=null){
					jiraConnector.delete(accountObjectClass, new Uid(uid), options);
				}
			}
		}
	}
}
