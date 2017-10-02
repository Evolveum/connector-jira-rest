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
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.security.GuardedString;
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
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author surmanek
 *
 */
public class AccountObject extends JiraObjectsProcessing {

	// user:
	private static final String ATTR_DISPLAY_NAME = "displayName";
	private static final String ATTR_PASSWORD = "password";
	private static final String ATTR_EMAILADDRESS = "emailAddress";
	private static final String ATTR_TIMEZONE = "timeZone";
	private static final String ATTR_APPLICAION_ROLES = "applicationRoles";
	private static final String ATTR_LOCALE = "locale";
	private static final String ATTR_ACTIVE = "active";
	private static final String PARAM_GROUPNAME = "groupname";
	private static final String PARAM_GET_ALL_USERS = ".";
	private static final String STATUS_ACTIVE = "true";

	private static final String MIDPOINT_PASSWORD = "__PASSWORD__";

	public AccountObject(JiraConfiguration configuration) {
		super(configuration);
	}

	ObjectClassInfoBuilder buildUserSchema() {
		// User Schema:
		ObjectClassInfoBuilder userObjClassBuilder = new ObjectClassInfoBuilder();
		// email:
		AttributeInfoBuilder attrMailBuilder = new AttributeInfoBuilder(ATTR_EMAILADDRESS, String.class);
		attrMailBuilder.setRequired(true);
		userObjClassBuilder.addAttributeInfo(attrMailBuilder.build());
		// display name:
		AttributeInfoBuilder attrDisplayNameBuilder = new AttributeInfoBuilder(ATTR_DISPLAY_NAME, String.class);
		attrDisplayNameBuilder.setRequired(true);
		userObjClassBuilder.addAttributeInfo(attrDisplayNameBuilder.build());
		// password:
		AttributeInfoBuilder attrPasswordBuilder = new AttributeInfoBuilder(ATTR_PASSWORD, GuardedString.class);
		userObjClassBuilder.addAttributeInfo(attrPasswordBuilder.build());
		// active:
		AttributeInfoBuilder attrActiveBuilder = new AttributeInfoBuilder(ATTR_ACTIVE, String.class);
		attrActiveBuilder.setCreateable(false);
		attrActiveBuilder.setUpdateable(false);
		userObjClassBuilder.addAttributeInfo(attrActiveBuilder.build());
		// avatarUrls:
		AttributeInfoBuilder attrAvatarUrlsBuilder = new AttributeInfoBuilder(ATTR_AVATAR_URLS, String.class);
		attrAvatarUrlsBuilder.setMultiValued(true);
		attrAvatarUrlsBuilder.setUpdateable(false);
		attrAvatarUrlsBuilder.setCreateable(false);
		userObjClassBuilder.addAttributeInfo(attrAvatarUrlsBuilder.build());
		// time zone:
		AttributeInfoBuilder attrTimeZoneBuilder = new AttributeInfoBuilder(ATTR_TIMEZONE, String.class);
		userObjClassBuilder.addAttributeInfo(attrTimeZoneBuilder.build());
		// locale:
		AttributeInfoBuilder attrLocaleBuilder = new AttributeInfoBuilder(ATTR_LOCALE, String.class);
		userObjClassBuilder.addAttributeInfo(attrLocaleBuilder.build());
		// groups:
		AttributeInfoBuilder attrGroupsBuilder = new AttributeInfoBuilder(ATTR_GROUPS, String.class);
		attrGroupsBuilder.setMultiValued(true);
		attrGroupsBuilder.setCreateable(false);
		attrGroupsBuilder.setUpdateable(false);
		userObjClassBuilder.addAttributeInfo(attrGroupsBuilder.build());
		// applicationRoles:
		AttributeInfoBuilder attrApplicationRolesBuilder = new AttributeInfoBuilder(ATTR_APPLICAION_ROLES,
				String.class);
		attrApplicationRolesBuilder.setMultiValued(true);
		attrApplicationRolesBuilder.setCreateable(false);
		attrApplicationRolesBuilder.setUpdateable(false);
		userObjClassBuilder.addAttributeInfo(attrApplicationRolesBuilder.build());
		// expand:
		AttributeInfoBuilder attrExpandBuilder = new AttributeInfoBuilder(ATTR_EXPAND, String.class);
		attrExpandBuilder.setUpdateable(false);
		attrExpandBuilder.setCreateable(false);
		userObjClassBuilder.addAttributeInfo(attrExpandBuilder.build());
		// self:
		AttributeInfoBuilder attrSelfBuilder = new AttributeInfoBuilder(ATTR_SELF, String.class);
		attrSelfBuilder.setUpdateable(false);
		attrSelfBuilder.setCreateable(false);
		userObjClassBuilder.addAttributeInfo(attrSelfBuilder.build());
		// binary avatar:
		AttributeInfoBuilder attrAvatarBinBuilder = new AttributeInfoBuilder(ATTR_AVATAR_BYTE_ARRRAY, byte[].class);
		userObjClassBuilder.addAttributeInfo(attrAvatarBinBuilder.build());

		userObjClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);

		return userObjClassBuilder;
	}

	Uid addUserToGroup(Uid uid, String groupName) {
		// POST /rest/api/2/group/user?groupname=<groupname>
		// {"name":"<username>"}
		String username = getUsernameFromUid(uid.getUidValue());
		JSONObject body = new JSONObject();
		URIBuilder uri = getURIBuilder();
		uri.setPath(URI_BASE_PATH + URI_GROUP_PATH + URI_USER_PATH);
		HttpEntityEnclosingRequestBase request;
		uri.addParameter(PARAM_GROUPNAME, groupName);
		try {
			request = new HttpPost(uri.build());
			body.put(ATTR_NAME, username);
			callRequest(request, body, true, CONTENT_TYPE_JSON);
			//LOG.info("\n\tadding user to group - result: {0}", jo.toString());
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Adding user to group failed: Problem occuerrd during building URI: ").append(uri)
					.append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString(), e);
		}

		return null;
	}

	Uid removeUserFromGroup(Uid uid, String groupName) {
		// DELETE
		// /rest/api/2/group/user?groupname=<groupname>&username=<username>
		String username = getUsernameFromUid(uid.getUidValue());
		URIBuilder uri = getURIBuilder();
		uri.setPath(URI_BASE_PATH + URI_GROUP_PATH + URI_USER_PATH);
		HttpDelete request;
		uri.addParameter(PARAM_GROUPNAME, groupName);
		uri.addParameter(PARAM_USERNAME, username);
		try {
			request = new HttpDelete(uri.build());
			callRequest(request, false, CONTENT_TYPE_JSON);
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Removing user from group failed: Problem occuerrd during building URI: ")
					.append(uri).append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString(), e);
		}

		return null;
	}

	Uid createOrUpdateUser(Uid uid, Set<Attribute> attrs) {
		if (attrs == null || attrs.isEmpty()) {
			LOG.error("Create or Update User Operation failed: attributes not provided or empty.");
			throw new InvalidAttributeValueException("Missing mandatory attributes.");
		}
		String responseUid = null;
		URIBuilder uri = getURIBuilder();
		uri.setPath(URI_BASE_PATH + URI_USER_PATH);

		Boolean create = uid == null;
		JSONObject body = new JSONObject();
		// Required attributes:
		// name:
		String name = getStringAttr(attrs, Name.NAME);
		if (create && (name == null || StringUtil.isBlank(name))) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Missing mandatory attribute ").append(Name.NAME);
			LOG.error(exceptionMsg.toString());
			throw new InvalidAttributeValueException(exceptionMsg.toString());
		}
		if (name != null)
			body.put(ATTR_NAME, name);

		// display name:
		String displayName = getStringAttr(attrs, ATTR_DISPLAY_NAME);
		if (create && (displayName == null || StringUtil.isBlank(displayName))) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Missing mandatory attribute ").append(ATTR_DISPLAY_NAME);
			LOG.error(exceptionMsg.toString());
			throw new InvalidAttributeValueException(exceptionMsg.toString());
		}
		if (displayName != null)
			body.put(ATTR_DISPLAY_NAME, displayName);

		// password:
		GuardedString guardedPasswd = getGuardedStringAttr(attrs, MIDPOINT_PASSWORD);
		final StringBuilder passwd = new StringBuilder();
		if (guardedPasswd != null) {
			guardedPasswd.access(new GuardedString.Accessor() {
				@Override
				public void access(char[] clearChars) {
					passwd.append(new String(clearChars));
				}
			});
			// set password during creation of user:
			if (create && passwd.toString() != null)
				body.put(ATTR_PASSWORD, passwd.toString());
			// update password:
			if (!create && passwd.toString() != null) {
				updateUserPassword(uid, passwd.toString());
				responseUid = uid.getUidValue();
			}
		}
		// email address:
		String emailAddress = getStringAttr(attrs, ATTR_EMAILADDRESS);
		if (create && (emailAddress == null || StringUtil.isBlank(emailAddress))) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Missing mandatory attribute ").append(ATTR_EMAILADDRESS);
			LOG.error(exceptionMsg.toString());
			throw new InvalidAttributeValueException(exceptionMsg.toString());
		}
		if (emailAddress != null)
			body.put(ATTR_EMAILADDRESS, emailAddress);

		// Optional attributes:
		putFieldIfExists(attrs, ATTR_TIMEZONE, body);
		// putFieldIfExists(attrs, ATTR_APPLICAION_ROLES, body);
		putFieldIfExists(attrs, ATTR_LOCALE, body);
		// putFieldIfExists(attrs, ATTR_AVATAR_URLS, body);
		// putFieldIfExists(attrs, ATTR_GROUPS, body);
		// putFieldIfExists(attrs, ATTR_SELF, body);
		// putFieldIfExists(attrs, ATTR_ACTIVE, body);
		// putFieldIfExists(attrs, ATTR_EXPAND, body);
		// putFieldIfExists(attrs, ATTR_KEY, body);

		try {
			HttpEntityEnclosingRequestBase request;

			if (create) {
				// create:
				request = new HttpPost(uri.build());
			} else {
				// update:
				uri.addParameter(PARAM_KEY, uid.getUidValue());
				request = new HttpPut(uri.build());
			}
			// execute request only when response body is not empty (not when
			// only avatar or password are updated):
			if (body.length() > 0) {
				// LOG.info("\n\tbody is not empty: {0} - {1}", body.length(),
				// body.toString());
				JSONObject jo = callRequest(request, body, true, CONTENT_TYPE_JSON);
				responseUid = jo.getString(ATTR_KEY);
			}

			// create or update user including attribute avatar:
			byte[] binaryAvatar = getByteArrayAttr(attrs, ATTR_AVATAR_BYTE_ARRRAY);
			if (binaryAvatar != null) {
				// update only avatar:
				if (attrs.size() == 1) {
					responseUid = uid.getUidValue();
				}
				createOrUpdateAvatar(binaryAvatar, responseUid, USER_NAME);
			}
			return new Uid(responseUid);

		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Create or Update User Operation failed: Problem occuerrd during building URI: ")
					.append(uri).append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString(), e);
		}
	}

	private void updateUserPassword(Uid uid, String password) {
		JSONObject body = new JSONObject();
		URIBuilder uri = getURIBuilder();
		uri.setPath(URI_BASE_PATH + URI_USER_PATH + URI_PASSWORD_PATH);
		HttpEntityEnclosingRequestBase request;
		uri.addParameter(PARAM_KEY, uid.getUidValue());
		try {
			request = new HttpPut(uri.build());
			body.put(ATTR_PASSWORD, password);
			callRequest(request, body, false, CONTENT_TYPE_JSON);
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Update password failed: Problem occuerrd during building URI: ").append(uri)
					.append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString(), e);
		}

	}

	void readUsers(Filter query, ResultsHandler handler, OperationOptions options) {
		HttpGet request;
		URIBuilder getUri = getURIBuilder();
		Attribute attr;
		JSONArray objectsArray;
		// get all users:
		if (query == null) {
			objectsArray = getAllUsers(options);
			handleUserObjects(objectsArray, handler, options, false);
		} else

		// get user by username:
		if (query instanceof EqualsFilter && ((EqualsFilter) query).getAttribute() instanceof Name) {
			Name name = (Name) ((EqualsFilter) query).getAttribute();
			if (name != null) {
				try {
					// getUri = getURIBuilder();
					getUri.setPath(URI_BASE_PATH + URI_USER_PATH);
					getUri.addParameter(PARAM_USERNAME, name.getNameValue());
					getUri.addParameter(ATTR_EXPAND, ATTR_GROUPS);
					getUri.addParameter(ATTR_EXPAND, ATTR_APPLICAION_ROLES);
					request = new HttpGet(getUri.build());
					JSONObject user = callRequest(request, true, CONTENT_TYPE_JSON);
					ConnectorObject connectorObject = convertUserToConnectorObject(user, true);
					handler.handle(connectorObject);
				} catch (URISyntaxException e) {
					StringBuilder exceptionMsg = new StringBuilder();
					exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
							.append(", using name attribute: ").append(name).append("\n\t")
							.append(e.getLocalizedMessage());
					LOG.error(exceptionMsg.toString());
					throw new ConnectorException(exceptionMsg.toString());
				}
			} else
				throwNullAttrException(query);
		} else

		// get user by uid:
		if (query instanceof EqualsFilter && ((EqualsFilter) query).getAttribute() instanceof Uid) {
			Uid uid = (Uid) ((EqualsFilter) query).getAttribute();
			if (uid != null) {
				try {
					// getUri = getURIBuilder();
					getUri.setPath(URI_BASE_PATH + URI_USER_PATH);
					getUri.addParameter(UID, uid.getUidValue());
					getUri.addParameter(ATTR_EXPAND, ATTR_GROUPS);
					getUri.addParameter(ATTR_EXPAND, ATTR_APPLICAION_ROLES);
					request = new HttpGet(getUri.build());
					JSONObject user = callRequest(request, true, CONTENT_TYPE_JSON);
					ConnectorObject connectorObject = convertUserToConnectorObject(user, true);
					handler.handle(connectorObject);
				} catch (URISyntaxException e) {
					StringBuilder exceptionMsg = new StringBuilder();
					exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
							.append(", using uid attribute: ").append(uid).append("\n\t")
							.append(e.getLocalizedMessage());
					LOG.error(exceptionMsg.toString());
					throw new ConnectorException(exceptionMsg.toString());
				}
			} else
				throwNullAttrException(query);
		} else
		// search users using startsWith filter:
		if (query instanceof StartsWithFilter) {
			attr = ((StartsWithFilter) query).getAttribute();
			JSONArray filteredArray = startsWithFiltering(query, attr, options, ObjectClass.ACCOUNT_NAME);
			handleUserObjects(filteredArray, handler, options, true);
		} else
		// search users using containsFilter, but in fact it is
		// startsWithFilter because Jira does not allow ContainsFilter:
		if (query instanceof ContainsFilter) {
			attr = ((ContainsFilter) query).getAttribute();
			JSONArray filteredArray = startsWithFiltering(query, attr, options, ObjectClass.ACCOUNT_NAME);
			handleUserObjects(filteredArray, handler, options, true);
		} else {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("\n\tUnsuported query filter for Account Object Class '").append(query)
					.append("' or its attribute name '").append(((AttributeFilter) query).getAttribute().getName())
					.append("'.");
			LOG.error(exceptionMsg.toString());
			throw new NoSuchMethodError(exceptionMsg.toString());
		}
	}
	
	private JSONArray getAllUsers(OperationOptions options){
		HttpGet request;
		URIBuilder getUri = getURIBuilder();
		JSONArray users = new JSONArray(), allUsers = new JSONArray();
		Boolean isPaginationRequested = false;
		int usersPerPage = 50, startAt = 0;
		getUri.setPath(URI_BASE_PATH + URI_USER_PATH + URI_SEARCH_PATH);
		// if want to get all user use parameter '.'
		getUri.addParameter(PARAM_USERNAME, PARAM_GET_ALL_USERS);
		try {
			if (options.getPagedResultsOffset() != null || options.getPageSize() != null) {
				isPaginationRequested = true;
				int pageNumber = options.getPagedResultsOffset();
				usersPerPage = options.getPageSize();				
				startAt = (pageNumber * usersPerPage) - usersPerPage;
				// LOGGER.info("\n\tpage: {0}, users per page {1}, start at: {2}", pageNumber, usersPerPage, startAt);
			}

			getUri.addParameter(PARAM_MAX_RESULTS, String.valueOf(usersPerPage));
			getUri.addParameter(PARAM_START_AT, String.valueOf(startAt));
			request = new HttpGet(getUri.build());
			users = callRequest(request);
			
			allUsers = concatJSONArrays(users, allUsers);
			
			while( users.length()==50 && !isPaginationRequested){
				startAt = startAt+50;
				getUri.clearParameters();
				getUri.addParameter(PARAM_USERNAME, PARAM_GET_ALL_USERS);
				getUri.addParameter(PARAM_MAX_RESULTS, String.valueOf(usersPerPage));
				getUri.addParameter(PARAM_START_AT, String.valueOf(startAt));
				request = new HttpGet(getUri.build());
				users = callRequest(request);
				allUsers = concatJSONArrays(users, allUsers);
			}
			return allUsers;
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
					.append(", during getting all users.\n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		}
	}
	
	//put objects of array1 to the end of array2
	private JSONArray concatJSONArrays(JSONArray array1, JSONArray array2){
		for (Object obj : array1){
			array2.put(obj);
		}
		return array2;
	}

	// parse user object from multiple json:
	private boolean handleUserObjects(JSONArray objectsArray, ResultsHandler handler, OperationOptions options, Boolean isNonBasicParametersNeeded) {
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

			// LOGGER.info("\n\tConverting Account...");
			connectorObject = convertUserToConnectorObject(object, isNonBasicParametersNeeded);

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

	private ConnectorObject convertUserToConnectorObject(JSONObject user, Boolean isNonBasicParametersNeeded) {
		if (user == null) {
			String exceptionMsg = "Conversion to Connector Object failed: JSONObject representing account object is not provided or is empty"; 
			LOG.error(exceptionMsg);
			throw new InvalidAttributeValueException(exceptionMsg);
		}

		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

		getUidIfExists(user, ATTR_KEY, builder);
		getNameIfExists(user, ATTR_NAME, builder);
		// if (user.has(ATTR_KEY)) {
		// builder.setUid(new Uid(user.getString(ATTR_KEY)));
		// }
		// if (user.has(ATTR_NAME)) {
		// builder.setName(new Name(user.getString(ATTR_NAME)));
		// }

		getIfExists(user, ATTR_PASSWORD, builder, IS_SINGLE_VALUE);
		getIfExists(user, ATTR_DISPLAY_NAME, builder, IS_SINGLE_VALUE);
		getIfExists(user, ATTR_EMAILADDRESS, builder, IS_SINGLE_VALUE);
		getIfExists(user, ATTR_TIMEZONE, builder, IS_SINGLE_VALUE);
		getIfExists(user, ATTR_LOCALE, builder, IS_SINGLE_VALUE);
		getIfExists(user, ATTR_SELF, builder, IS_SINGLE_VALUE);
		getIfExists(user, ATTR_EXPAND, builder, IS_SINGLE_VALUE);

		if (user.has(ATTR_ACTIVE)) {
			boolean enable = STATUS_ACTIVE.equals(user.get(ATTR_ACTIVE).toString());
			addAttr(builder, OperationalAttributes.ENABLE_NAME, enable);
		}

		if (user.has(ATTR_AVATAR_URLS)) {
			JSONObject avatars = user.getJSONObject(ATTR_AVATAR_URLS);
			// LOGGER.info("\n\tavatars: {0}", avatars);
			ArrayList<String> avatarsArray = new ArrayList<String>();
			avatarsArray.add((String) avatars.getString("16x16"));
			avatarsArray.add((String) avatars.getString("24x24"));
			avatarsArray.add((String) avatars.getString("32x32"));
			avatarsArray.add((String) avatars.getString("48x48"));
			builder.addAttribute(ATTR_AVATAR_URLS, avatarsArray.toArray());
			
			if (isNonBasicParametersNeeded){
				byte[] avatarArray = getAvatar(avatarsArray.get(3), new ObjectClass(ObjectClass.ACCOUNT_NAME));
				builder.addAttribute(ATTR_AVATAR_BYTE_ARRRAY, avatarArray);
			}
		}

		if (user.has(ATTR_GROUPS)) {
			JSONObject groups = user.getJSONObject(ATTR_GROUPS);
			ArrayList<String> groupsArray = getMultiAttrItems(groups, EXTENDED_ATTR_ITEMS, EXTENDED_ATTR_NAME);

			if (groupsArray != null) {
				builder.addAttribute(ATTR_GROUPS, groupsArray);
			}
		}
		if (user.has(ATTR_APPLICAION_ROLES)) {
			JSONObject roles = user.getJSONObject(ATTR_APPLICAION_ROLES);
			ArrayList<String> rolesArray = getMultiAttrItems(roles, EXTENDED_ATTR_ITEMS, EXTENDED_ATTR_NAME);
			if (rolesArray != null) {
				builder.addAttribute(ATTR_APPLICAION_ROLES, rolesArray);
			}
		}

		ConnectorObject connectorObject = builder.build();
		// LOG.ok("\nconvertUserToConnectorObject, user: {0},
		// \n\tconnectorObject: {1}", user.getString(UID), connectorObject);
		return connectorObject;
	}

	void deleteUser(Uid uid) {
		URIBuilder deleteUri = getURIBuilder().setPath(URI_BASE_PATH + URI_USER_PATH).addParameter(PARAM_KEY,
				uid.getUidValue());
		// LOG.ok("delete user, Uid: {0}", uid);
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
