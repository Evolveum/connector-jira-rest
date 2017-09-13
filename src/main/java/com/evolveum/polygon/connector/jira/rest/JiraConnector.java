/**
 * Copyright (c) 2017 Evolveum
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

import java.util.List;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * @author surmanek
 *
 */
@ConnectorClass(displayNameKey = "JiraConnector.rest.display", configurationClass = JiraConfiguration.class)

public class JiraConnector implements TestOp, SchemaOp, Connector, CreateOp, DeleteOp, SearchOp<Filter>, UpdateOp,
		UpdateAttributeValuesOp {

	private static final Log LOG = Log.getLog(JiraConnector.class);
	private JiraConfiguration configuration;

	private static final String PROJECT_NAME = "PROJECT";
	private static final String ATTR_GROUPS = "groups";

	@Override
	public JiraConfiguration getConfiguration() {
		LOG.info("Fetch configuration");
		return this.configuration;
	}

	@Override
	public void init(Configuration configuration) {
		if (configuration == null) {
			LOG.error("Initialization of the configuration failed: Configuration is not provided.");
			throw new ConfigurationException(
					"Initialization of the configuration failed: Configuration is not provided.");
		}
		this.configuration = (JiraConfiguration) configuration;
		this.configuration.validate();
	}

	@Override
	public void dispose() {
		configuration = null;
	}

	@Override
	public void test() {
		LOG.info("Testing connection...");
		JiraObjectsProcessing objectProcessing = new JiraObjectsProcessing(configuration);
		objectProcessing.testConnetion(configuration.getUsername());
		LOG.ok("Testing finished successfully.");
	}

	@Override
	public Schema schema() {
		SchemaBuilder schemaBuilder = new SchemaBuilder(JiraConnector.class);

		// build user schema:
		AccountObject user = new AccountObject(configuration);
		ObjectClassInfoBuilder userBuilder = user.buildUserSchema();
		schemaBuilder.defineObjectClass(userBuilder.build());

		// build project schema:
		ProjectObject project = new ProjectObject(configuration);
		ObjectClassInfoBuilder projectBuilder = project.buildProjectSchema();
		schemaBuilder.defineObjectClass(projectBuilder.build());

		// build group schema:
		GroupObject group = new GroupObject(configuration);
		ObjectClassInfoBuilder groupBuilder = group.buildGroupSchema();
		schemaBuilder.defineObjectClass(groupBuilder.build());

		return schemaBuilder.build();
	}

	@Override
	public Uid addAttributeValues(ObjectClass objClass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~addAttrValues-Parameters~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		if (objClass == null) {
			LOG.error("Remove attributes value operation failed: Attribute Object Class is not provided.");
			throw new InvalidAttributeValueException(
					"Update operation failed: Attribute Object Class is not provided.");
		} else
			LOG.info("ObjectClasss: {0}", objClass.toString());
		if (uid == null) {
			LOG.error("Remove attributes value operation failed: Attribute UID is not provided.");
			throw new InvalidAttributeValueException("Update operation failed: Attribute UID is not provided.");
		} else
			LOG.info("Uid: {0}", uid.toString());
		if (attrs == null) {
			LOG.error("Remove attributes value operation failed: Attribute set is not provided.");
			throw new InvalidAttributeValueException(
					"Add attributes value operation failed: Attribute set is not provided.");
		} else
			LOG.info("Attributes: {0}", attrs.toString());
		if (options != null)
			LOG.info("Options: {0}", options.toString());
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

		for (Attribute attr : attrs) {
			// add users to Group:
			if (objClass.is(ObjectClass.ACCOUNT_NAME) && attr.getName().equals(ATTR_GROUPS)) {
				AccountObject user = new AccountObject(configuration);
				List<Object> groupNames = attr.getValue();
				for (Object groupName : groupNames) {
					user.addUserToGroup(uid, (String) groupName);
				}
			}
			// add user/group to project:
			if (objClass.is(PROJECT_NAME)) {
				ProjectObject project = new ProjectObject(configuration);
				project.addMembersToProject(uid, attr);
			}
		}
		return uid;
	}

	@Override
	public Uid removeAttributeValues(ObjectClass objClass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~removeAttrValues-Parameters~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		if (objClass == null) {
			LOG.error("Remove attributes value operation failed: Attribute Object Class is not provided.");
			throw new InvalidAttributeValueException(
					"Update operation failed: Attribute Object Class is not provided.");
		} else
			LOG.info("ObjectClasss: {0}", objClass.toString());
		if (uid == null) {
			LOG.error("Remove attributes value operation failed: Attribute UID is not provided.");
			throw new InvalidAttributeValueException("Update operation failed: Attribute UID is not provided.");
		} else
			LOG.info("Uid: {0}", uid.toString());
		if (attrs == null) {
			LOG.error("Remove attributes value operation failed: Attribute set is not provided.");
			throw new InvalidAttributeValueException(
					"Add attributes value operation failed: Attribute set is not provided.");
		} else
			LOG.info("Attributes: {0}", attrs.toString());
		if (options != null)
			LOG.info("Options: {0}", options.toString());
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

		for (Attribute attr : attrs) {
			// remove users from Group:
			if (objClass.is(ObjectClass.ACCOUNT_NAME) && attr.getName().equals(ATTR_GROUPS)) {
				AccountObject user = new AccountObject(configuration);
				List<Object> groupNames = attr.getValue();
				for (Object groupName : groupNames) {
					user.removeUserFromGroup(uid, (String) groupName);
				}
			}
			// add Administrators.users to Project role:
			if (objClass.is(PROJECT_NAME)) {
				ProjectObject project = new ProjectObject(configuration);
				project.removeMembersFromProject(uid, attr);
			}
		}
		return uid;
	}

	@Override
	public Uid update(ObjectClass objClass, Uid uid, Set<Attribute> attrs, OperationOptions options) {

		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Update-Parameters~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		if (objClass == null) {
			LOG.error("Update operation failed: Attribute Object Class is not provided.");
			throw new InvalidAttributeValueException(
					"Update operation failed: Attribute Object Class is not provided.");
		} else
			LOG.info("ObjectClasss: {0}", objClass.toString());
		if (uid == null) {
			LOG.error("Update operation failed: Attribute UID is not provided.");
			throw new InvalidAttributeValueException("Update operation failed: Attribute UID is not provided.");
		} else
			LOG.info("Uid: {0}", uid.toString());
		if (attrs == null) {
			LOG.error("Update operation failed: Attribute set is not provided.");
			throw new InvalidAttributeValueException("Update operation failed: Attribute set is not provided.");
		} else
			LOG.info("Attributes: {0}", attrs.toString());

		if (options != null)
			LOG.info("Options: {0}", options.toString());
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

		if (objClass.is(ObjectClass.ACCOUNT_NAME)) { // __ACCOUNT__
			AccountObject user = new AccountObject(configuration);
			return user.createOrUpdateUser(uid, attrs);
		}
		if (objClass.is(ObjectClass.GROUP_NAME)) { // __GROUP__
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Jira does not support update group. Requested group uid: ").append(uid.getUidValue());
			LOG.error(exceptionMsg.toString());
			throw new UnsupportedOperationException(exceptionMsg.toString());
		}
		if (objClass.is(PROJECT_NAME)) { // PROJECT
			ProjectObject project = new ProjectObject(configuration);
			return project.createOrUpdateProject(uid, attrs);
		}
		return null;
	}

	@Override
	public Uid create(ObjectClass objClass, Set<Attribute> attrs, OperationOptions options) {
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Create-Parameters~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		if (objClass == null) {
			LOG.error("Create operation failed: Attribute Object Class is not provided.");
			throw new InvalidAttributeValueException("Attribute Object Class is not provided.");
		} else
			LOG.info("ObjectClasss: {0}", objClass.toString());
		if (attrs == null) {
			LOG.error("Create operation failed: Attribute set is not provided.");
			throw new InvalidAttributeValueException("Create operation failed: Attribute set is not provided.");
		} else
			LOG.info("Attributes: {0}", attrs.toString());
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		// create user:
		if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
			AccountObject user = new AccountObject(configuration);
			return user.createOrUpdateUser(null, attrs);
		}
		// create group:
		if (objClass.is(ObjectClass.GROUP_NAME)) {
			GroupObject group = new GroupObject(configuration);
			return group.createGroup(attrs);
		}
		// create project:
		if (objClass.is(PROJECT_NAME)) {
			ProjectObject project = new ProjectObject(configuration);
			return project.createOrUpdateProject(null, attrs);
		} else {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Object Class").append(objClass).append("is not supported");
			LOG.error(exceptionMsg.toString());
			throw new UnsupportedOperationException(exceptionMsg.toString());
		}

	}

	@Override
	public FilterTranslator<Filter> createFilterTranslator(ObjectClass arg0, OperationOptions arg1) {
		return new FilterTranslator<Filter>() {
			@Override
			public List<Filter> translate(Filter filter) {
				return CollectionUtil.newList(filter);
			}
		};
	}

	@Override
	public void executeQuery(ObjectClass objClass, Filter query, ResultsHandler handler, OperationOptions options) {
		// search by attribute 'username', however return reuslt containing
		// value on username or name or e-mail
		// get by 'username', 'key', but jira uses username value for key
		// attribute

		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Execute Query-Parameters~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		if (objClass == null) {
			LOG.error("Get operation failed: Attribute Object Class is not provided.");
			throw new InvalidAttributeValueException("Attribute Object Class is not provided.");
		} else
			LOG.info("ObjectClasss: {0}", objClass.toString());

		if (handler == null) {
			LOG.error("Get operation failed: Attribute Result Handler is not provided.");
			throw new InvalidAttributeValueException("Attribute Result Handler is not provided.");
		} else
			LOG.info("Execute Query-Handler: {0}", handler.toString());

		if (options == null) {
			LOG.error("Get operation failed: Attribute Options is not provided.");
			throw new InvalidAttributeValueException("Attribute Options is not provided.");
		} else
			LOG.info("Options: {0}", options.toString());

		if (query != null)
			LOG.info("Filter: {0}", query.toString());
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

		// Users:
		if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
			AccountObject user = new AccountObject(configuration);
			user.readUsers(query, handler, options);
		}
		// Groups:
		if (objClass.is(ObjectClass.GROUP_NAME)) {
			GroupObject group = new GroupObject(configuration);
			group.readGroups(query, handler, options);
		}
		// Projects:
		if (objClass.is(PROJECT_NAME)) {
			ProjectObject project = new ProjectObject(configuration);
			project.readProjects(query, handler, options);
		}
	}

	@Override
	public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~Delete-Parameters~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		if (objClass == null) {
			LOG.error("Delete operation failed: Attribute Object Class is not provided.");
			throw new InvalidAttributeValueException(
					"Delete operation failed: Attribute Object Class is not provided.");
		} else
			LOG.info("ObjectClasss: {0}", objClass.toString());
		if (uid == null) {
			LOG.error("Delete operation failed: Attribute UID is not provided.");
			throw new InvalidAttributeValueException("Delete operation failed: Attribute UID is not provided.");
		} else
			LOG.info("Uid: {0}", uid.toString());
		LOG.info("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

		if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
			AccountObject user = new AccountObject(configuration);
			user.deleteUser(uid);
		}
		if (objClass.is(ObjectClass.GROUP_NAME)) {
			GroupObject group = new GroupObject(configuration);
			group.deleteGroup(uid);
		}
		if (objClass.is(PROJECT_NAME)) {
			ProjectObject project = new ProjectObject(configuration);
			project.deleteProject(uid);
		}
	}
}
