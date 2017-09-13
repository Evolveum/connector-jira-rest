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
package com.evolveum.polygon.connector.jira.rest;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author surmanek
 *
 */
public class GroupObject extends JiraObjectsProcessing {

	private static final String ATTR_USERS = "users";
	private static final String PARAM_GROUPNAME = "groupname";
	private static final String PARAM_QUERY = "query";

	public GroupObject(JiraConfiguration configuration) {
		super(configuration);
	}

	ObjectClassInfoBuilder buildGroupSchema() {

		ObjectClassInfoBuilder groupObjClassBuilder = new ObjectClassInfoBuilder();

		groupObjClassBuilder.setType(ObjectClass.GROUP_NAME);
		// users:
		AttributeInfoBuilder attrGroupUsersBuilder = new AttributeInfoBuilder(ATTR_USERS, String.class);
		attrGroupUsersBuilder.setMultiValued(true);
		attrGroupUsersBuilder.setUpdateable(false);
		attrGroupUsersBuilder.setCreateable(false);
		groupObjClassBuilder.addAttributeInfo(attrGroupUsersBuilder.build());
		// self:
		AttributeInfoBuilder attrGroupSelfBuilder = new AttributeInfoBuilder(ATTR_SELF, String.class);
		attrGroupSelfBuilder.setUpdateable(false);
		attrGroupSelfBuilder.setCreateable(false);
		groupObjClassBuilder.addAttributeInfo(attrGroupSelfBuilder.build());
		// expand:
		AttributeInfoBuilder attrGroupExpandBuilder = new AttributeInfoBuilder(ATTR_EXPAND, String.class);
		attrGroupExpandBuilder.setUpdateable(false);
		attrGroupExpandBuilder.setCreateable(false);
		groupObjClassBuilder.addAttributeInfo(attrGroupExpandBuilder.build());

		return groupObjClassBuilder;
	}

	Uid createGroup(Set<Attribute> attrs) {
		LOG.ok("createGroup, attributes: {0}", attrs);
		if (attrs == null || attrs.isEmpty()) {
			LOG.error("Create or Update Group operation failed: attributes not provided or empty.");
			throw new InvalidAttributeValueException("Missing mandatory attributes.");
		}
		URIBuilder uri = getURIBuilder();
		uri.setPath(URI_BASE_PATH + URI_GROUP_PATH);

		JSONObject json = new JSONObject();
		// Required attributes:
		// name:
		String name = getStringAttr(attrs, MIDPOINT_NAME);
		if (name == null || StringUtil.isBlank(name)) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Missing mandatory attribute ").append(MIDPOINT_NAME);
			LOG.error(exceptionMsg.toString());
			throw new InvalidAttributeValueException(exceptionMsg.toString());
		} else {
			json.put(ATTR_NAME, name);
		}

		try {
			HttpEntityEnclosingRequestBase request;
			request = new HttpPost(uri.build());

			JSONObject jo = callRequest(request, json, true, CONTENT_TYPE_JSON);

			// Create UID from NAME, because JIRA does not provide UID contained
			// in JSONObject response:
			String responceName = jo.getString(ATTR_NAME);
			return new Uid(responceName);

		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Create Group Operation failed: problem occuerrd during building URI: ")
					.append(getURIBuilder()).append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString(), e);
		}
	}

	void readGroups(Filter query, ResultsHandler handler, OperationOptions options) {

		HttpGet request;
		URIBuilder getUri = getURIBuilder();
		JSONArray objectsArray;

		JSONObject object = new JSONObject();
		// get all groups:
		if (query == null) {
			try {
				// if want to get all groups use picker
				// getUri = getURIBuilder();
				getUri.setPath(URI_BASE_PATH + URI_GROUPS_PICKER_PATH);
				request = new HttpGet(getUri.build());
				object = callRequest(request, true, CONTENT_TYPE_JSON);
				objectsArray = object.getJSONArray(ATTR_GROUPS);
				handleGroupObjects(objectsArray, handler, options);
			} catch (URISyntaxException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
						.append(" (getting all groups) \n\t").append(e.getLocalizedMessage());
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			}
		} else
		// get group by groupname or groupid:
		if (query instanceof EqualsFilter) {
			Name name = null;
			Uid uid = null;
			String attrValue = "";
			if (((EqualsFilter) query).getAttribute() instanceof Name) {
				name = (Name) ((EqualsFilter) query).getAttribute();
				if (name != null) {
					attrValue = name.getNameValue();
				} else
					throwNullAttrException(query);
			}
			if (((EqualsFilter) query).getAttribute() instanceof Uid) {
				uid = (Uid) ((EqualsFilter) query).getAttribute();
				if (uid != null) {
					attrValue = uid.getUidValue();
				} else
					throwNullAttrException(query);
			}
			try {
				// getUri = getURIBuilder();
				getUri.setPath(URI_BASE_PATH + URI_GROUP_PATH);
				getUri.addParameter(PARAM_GROUPNAME, attrValue);
				getUri.addParameter(ATTR_EXPAND, ATTR_USERS);
				request = new HttpGet(getUri.build());
				JSONObject group = callRequest(request, true, CONTENT_TYPE_JSON);
				ConnectorObject connectorObject = convertGroupToConnectorObject(group);
				handler.handle(connectorObject);
			} catch (URISyntaxException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
						.append(", using name/uid attribute: '").append(attrValue).append("'\n\t")
						.append(e.getLocalizedMessage());
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			}
		} else
		// search groups using ContainsFilter:
		if (query instanceof ContainsFilter && ((ContainsFilter) query).getAttribute() instanceof Name) {
			Name name = (Name) ((ContainsFilter) query).getAttribute();
			if (name != null) {
				try {
					// getUri = getURIBuilder();
					getUri.setPath(URI_BASE_PATH + URI_GROUPS_PICKER_PATH);
					getUri.addParameter(PARAM_QUERY, name.getNameValue());
					request = new HttpGet(getUri.build());
					object = callRequest(request, true, CONTENT_TYPE_JSON);
					objectsArray = object.getJSONArray(ATTR_GROUPS);
					handleGroupObjects(objectsArray, handler, options);
				} catch (URISyntaxException e) {
					StringBuilder exceptionMsg = new StringBuilder();
					exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
							.append(", using name attribute: '").append(name).append("'\n\t")
							.append(e.getLocalizedMessage());
					LOG.error(exceptionMsg.toString());
					throw new ConnectorException(exceptionMsg.toString());
				}
			} else
				throwNullAttrException(query);
		} else {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("\n\tUnsuported query filter for Group Object Class '").append(query)
					.append("' or its attribute name '").append(((AttributeFilter) query).getAttribute().getName())
					.append("'.");
			LOG.error(exceptionMsg.toString());
			throw new NoSuchMethodError(exceptionMsg.toString());
		}

	}

	private boolean handleGroupObjects(JSONArray objectsArray, ResultsHandler handler, OperationOptions options)
			throws JSONException, URISyntaxException {
		/*
		LOG.ok("Number of objects: {0}, pageResultsOffset: {1}, pageSize: {2} ", objectsArray.length(),
				options == null ? "null" : options.getPagedResultsOffset(),
				options == null ? "null" : options.getPageSize());
		 */
		for (int i = 0; i < objectsArray.length(); i++) {
			// LOG.ok("executeQuery: processing {0}/{1} objects", i + 1,
			// objectsArray.length());

			JSONObject object = objectsArray.getJSONObject(i);
			ConnectorObject connectorObject = null;

			// LOGGER.info("\n\tConverting Group...");
			connectorObject = convertGroupToConnectorObject(object);

			boolean finish = false;
			if (connectorObject != null) {
				finish = !handler.handle(connectorObject);
			}
			if (finish) {
				return true;
			}
		}
		return false;
	}

	private ConnectorObject convertGroupToConnectorObject(JSONObject group) {
		if (group == null) {
			String exceptionMsg = "Conversion to Connector Object failed: JSONObject group is not provided or is empty";
			LOG.error(exceptionMsg);
			throw new InvalidAttributeValueException(exceptionMsg);
		}
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
		builder.setObjectClass(new ObjectClass(ObjectClass.GROUP_NAME));
		// if (group.has(ATTR_NAME)) {
		// builder.setName(new Name(group.getString(ATTR_NAME)));
		// builder.setUid(new Uid(group.getString(ATTR_NAME)));
		// }
		getNameIfExists(group, ATTR_NAME, builder);
		// Use NAME as UID, because JIRA does not provide UID contained in
		// JSONObject response for groups:
		builder.setUid(new Uid(group.getString(ATTR_NAME)));
		getIfExists(group, ATTR_SELF, builder, IS_SINGLE_VALUE);
		getIfExists(group, ATTR_EXPAND, builder, IS_SINGLE_VALUE);

		if (group.has(ATTR_USERS)) {
			JSONObject users = group.getJSONObject(ATTR_USERS);
			ArrayList<String> usersArray = getMultiAttrItems(users, EXTENDED_ATTR_ITEMS, EXTENDED_ATTR_NAME);
			if (usersArray != null) {
				builder.addAttribute(ATTR_USERS, usersArray.toArray());
			}
		}

		ConnectorObject connectorObject = builder.build();
		// LOG.ok("\nconvertUserToConnectorObject, group: {0},
		// \n\tconnectorObject: {1}", group.getString(ATTR_NAME),
		// connectorObject);
		return connectorObject;
	}

	void deleteGroup(Uid uid) {
		URIBuilder deleteUri = getURIBuilder().setPath(URI_BASE_PATH + URI_GROUP_PATH).addParameter(PARAM_GROUPNAME,
				uid.getUidValue());
		HttpDelete request;
		try {
			request = new HttpDelete(deleteUri.build());
			callRequest(request, false, CONTENT_TYPE_JSON);
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Delete operation failed: problem occurred during executing URI: ").append(deleteUri)
					.append("\n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		}

	}

}
