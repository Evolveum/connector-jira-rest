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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author surmanek
 *
 */
public class ProjectObject extends JiraObjectsProcessing {

	private static final String ATTR_PROJECT_TYPE_KEY = "projectTypeKey";
	private static final String ATTR_LEAD = "lead";
	private static final String ATTR_TEMPLATE_KEY = "projectTemplateKey";
	private static final String ATTR_DESCRIPTION = "description";
	private static final String ATTR_URL = "url";
	private static final String ATTR_ASSIGNEE_TYPE = "assigneeType";
	private static final String ATTR_AVATAR_ID = "avatarId";
	private static final String ATTR_ISSUE_SECURITY_SCHEME = "issueSecurityScheme";
	private static final String ATTR_PERMISSION_SCHEME = "permissionScheme";
	private static final String ATTR_NOTIFICATION_SCHEME = "notificationScheme";
	private static final String ATTR_CATEGORY_ID = "categoryId";
	private static final String ATTR_ID = "id";
	private static final String ATTR_COMPONENTS = "components";
	private static final String ATTR_ISSUE_TYPES = "issueTypes";
	private static final String ATTR_VERSIONS = "versions";
	private static final String ATTR_ROLES = "roles";
	private static final String URI_ROLE = "role";
	private static final String ATTR_ROLES_DEVELOPERS = "roles.developers";

	private static final String ATTR_ROLES_ADMINISTRATORS = "roles.administrators";

	private static final String ATTR_ACTORS = "actors";
	private static final String ATTR_TYPE = "type";

	public ProjectObject(JiraConfiguration configuration) {
		super(configuration);
	}

	ObjectClassInfoBuilder buildProjectSchema() {
		// Project Schema:
		ObjectClassInfoBuilder projectObjClassBuilder = new ObjectClassInfoBuilder();
		projectObjClassBuilder.setType(PROJECT_NAME);
		// key:
		AttributeInfoBuilder attrProjectKeyBuilder = new AttributeInfoBuilder(ATTR_KEY, String.class);
		attrProjectKeyBuilder.setRequired(true);
		projectObjClassBuilder.addAttributeInfo(attrProjectKeyBuilder.build());
		// project type key:
		AttributeInfoBuilder attrProjectTypeKeyBuilder = new AttributeInfoBuilder(ATTR_PROJECT_TYPE_KEY, String.class);
		attrProjectTypeKeyBuilder.setRequired(true);
		projectObjClassBuilder.addAttributeInfo(attrProjectTypeKeyBuilder.build());
		// lead:
		AttributeInfoBuilder attrProjectLeadBuilder = new AttributeInfoBuilder(ATTR_LEAD, String.class);
		attrProjectLeadBuilder.setRequired(true);
		projectObjClassBuilder.addAttributeInfo(attrProjectLeadBuilder.build());
		// project template key:
		AttributeInfoBuilder attrProjectTemplateKeyBuilder = new AttributeInfoBuilder(ATTR_TEMPLATE_KEY, String.class);
		attrProjectTemplateKeyBuilder.setCreateable(false);
		attrProjectTemplateKeyBuilder.setUpdateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectTemplateKeyBuilder.build());
		// description:
		AttributeInfoBuilder attrProjectDescriptionBuilder = new AttributeInfoBuilder(ATTR_DESCRIPTION, String.class);
		projectObjClassBuilder.addAttributeInfo(attrProjectDescriptionBuilder.build());
		// url:
		AttributeInfoBuilder attrProjectUrlBuilder = new AttributeInfoBuilder(ATTR_URL, String.class);
		projectObjClassBuilder.addAttributeInfo(attrProjectUrlBuilder.build());
		// assignee type:
		AttributeInfoBuilder attrProjectAssigneeTypeBuilder = new AttributeInfoBuilder(ATTR_ASSIGNEE_TYPE,
				String.class);
		projectObjClassBuilder.addAttributeInfo(attrProjectAssigneeTypeBuilder.build());
		// avatar id:
		AttributeInfoBuilder attrProjectAvatarIdBuilder = new AttributeInfoBuilder(ATTR_AVATAR_ID, String.class);
		attrProjectAvatarIdBuilder.setCreateable(false);
		attrProjectAvatarIdBuilder.setUpdateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectAvatarIdBuilder.build());
		// avatar urls:
		AttributeInfoBuilder attrProjectAvatarUrlsBuilder = new AttributeInfoBuilder(ATTR_AVATAR_URLS, String.class);
		attrProjectAvatarUrlsBuilder.setUpdateable(false);
		attrProjectAvatarUrlsBuilder.setCreateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectAvatarUrlsBuilder.build());
		// issue security scheme:
		AttributeInfoBuilder attrProjectIssueSecSchemeBuilder = new AttributeInfoBuilder(ATTR_ISSUE_SECURITY_SCHEME,
				String.class);
		attrProjectIssueSecSchemeBuilder.setCreateable(false);
		attrProjectIssueSecSchemeBuilder.setUpdateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectIssueSecSchemeBuilder.build());
		// category id:
		AttributeInfoBuilder attrProjectCategoryIdBuilder = new AttributeInfoBuilder(ATTR_CATEGORY_ID, String.class);
		attrProjectCategoryIdBuilder.setCreateable(false);
		attrProjectCategoryIdBuilder.setUpdateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectCategoryIdBuilder.build());
		// expand:
		AttributeInfoBuilder attrProjectExpandBuilder = new AttributeInfoBuilder(ATTR_EXPAND, String.class);
		attrProjectExpandBuilder.setUpdateable(false);
		attrProjectExpandBuilder.setCreateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectExpandBuilder.build());
		// self:
		AttributeInfoBuilder attrProjectSelfBuilder = new AttributeInfoBuilder(ATTR_SELF, String.class);
		attrProjectSelfBuilder.setUpdateable(false);
		attrProjectSelfBuilder.setCreateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectSelfBuilder.build());
		// components:
		AttributeInfoBuilder attrProjectComponentsBuilder = new AttributeInfoBuilder(ATTR_COMPONENTS, String.class);
		attrProjectComponentsBuilder.setMultiValued(true);
		attrProjectComponentsBuilder.setCreateable(false);
		attrProjectComponentsBuilder.setUpdateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectComponentsBuilder.build());
		// issue type:
		AttributeInfoBuilder attrProjectIssueTypeBuilder = new AttributeInfoBuilder(ATTR_ISSUE_TYPES, String.class);
		attrProjectIssueTypeBuilder.setMultiValued(true);
		attrProjectIssueTypeBuilder.setCreateable(false);
		attrProjectIssueTypeBuilder.setUpdateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectIssueTypeBuilder.build());
		// versions:
		AttributeInfoBuilder attrProjectVersionsBuilder = new AttributeInfoBuilder(ATTR_VERSIONS, String.class);
		attrProjectVersionsBuilder.setMultiValued(true);
		attrProjectVersionsBuilder.setCreateable(false);
		attrProjectVersionsBuilder.setUpdateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectVersionsBuilder.build());
		// roles-developers:
		AttributeInfoBuilder attrProjectRolesDevelopersBuilder = new AttributeInfoBuilder(ATTR_ROLES_DEVELOPERS,
				String.class);
		attrProjectRolesDevelopersBuilder.setUpdateable(false);
		attrProjectRolesDevelopersBuilder.setCreateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectRolesDevelopersBuilder.build());
		// roles-developers:
		AttributeInfoBuilder attrProjectRolesAdministratorsBuilder = new AttributeInfoBuilder(ATTR_ROLES_ADMINISTRATORS,
				String.class);
		attrProjectRolesAdministratorsBuilder.setUpdateable(false);
		attrProjectRolesAdministratorsBuilder.setCreateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectRolesAdministratorsBuilder.build());
		// binary avatar:
		AttributeInfoBuilder attrProjectAvatarBinBuilder = new AttributeInfoBuilder(ATTR_AVATAR_BYTE_ARRRAY,
				byte[].class);
		projectObjClassBuilder.addAttributeInfo(attrProjectAvatarBinBuilder.build());
		// developer users:
		AttributeInfoBuilder attrProjectDeveloperUsersBuilder = new AttributeInfoBuilder(ATTR_DEVELOPERS_USERS,
				String.class);
		attrProjectDeveloperUsersBuilder.setMultiValued(true);
		attrProjectDeveloperUsersBuilder.setCreateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectDeveloperUsersBuilder.build());
		// developer groups:
		AttributeInfoBuilder attrProjectDeveloperGroupsBuilder = new AttributeInfoBuilder(ATTR_DEVELOPERS_GROUPS,
				String.class);
		attrProjectDeveloperGroupsBuilder.setMultiValued(true);
		attrProjectDeveloperGroupsBuilder.setCreateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectDeveloperGroupsBuilder.build());
		// administrator users:
		AttributeInfoBuilder attrProjectAdministratorUsersBuilder = new AttributeInfoBuilder(ATTR_ADMINISTRATORS_USERS,
				String.class);
		attrProjectAdministratorUsersBuilder.setMultiValued(true);
		attrProjectAdministratorUsersBuilder.setCreateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectAdministratorUsersBuilder.build());
		// administrator groups:
		AttributeInfoBuilder attrProjectAdministratorGroupsBuilder = new AttributeInfoBuilder(
				ATTR_ADMINISTRATORS_GROUPS, String.class);
		attrProjectAdministratorGroupsBuilder.setMultiValued(true);
		attrProjectAdministratorGroupsBuilder.setCreateable(false);
		projectObjClassBuilder.addAttributeInfo(attrProjectAdministratorGroupsBuilder.build());

		return projectObjClassBuilder;
	}

	// actorName = 'user' / 'group'
	// roleName = 'Administrators' / 'Developers'
	void addActorsToProjectRole(Uid projectUid, JSONArray userOrGroupIds, String actorName, String roleName) {
		/// POST api/2/project/<projectUid>/role/<roleId> {"name":["<userId>]"}
		/// or {"group":["<groupId>"]}
		JSONObject body = new JSONObject();
		String projectRoleUri = getProjectRolesUrl(projectUid, roleName);
		HttpPost postRequest;
		try {
			URIBuilder uri = new URIBuilder(projectRoleUri);
			postRequest = new HttpPost(uri.build());
			body.put(actorName, userOrGroupIds);
			callRequest(postRequest, body, true, CONTENT_TYPE_JSON);
			// LOGGER.info("\n\tadding {0} to project role {1} - result: {2}",
			// actorName, roleName, jo.toString());
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Adding actor to project role failed: Problem occuerrd during building URI: ")
					.append(projectRoleUri).append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString(), e);
		}
	}

	// Get url for update project roles according to roleName ('Developers' or
	// 'Administrators')
	String getProjectRolesUrl(Uid projectUid, String roleName) {
		HttpGet getRequest;
		URIBuilder getUri = getURIBuilder();
		try {
			getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + projectUid.getUidValue());
			getRequest = new HttpGet(getUri.build());
			JSONObject project = callRequest(getRequest, true, CONTENT_TYPE_JSON);

			JSONObject roles = project.getJSONObject(ATTR_ROLES);
			return roles.getString(roleName);
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Get project role url failed: problem occurred during executing URI: ").append(getUri)
					.append(", using uid attribute: ").append(projectUid).append("\n\t")
					.append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		}
	}

	private void removeActorsFromProjectRole(Uid projectUid, String userOrGroupNames, String actorName,
			String roleName) {
		// DELETE
		// /rest/api/2/project/<projectIdOrKey>/role/<roleId>?user=<userID>
		// or
		// DELETE
		// /rest/api/2/project/<projectIdOrKey>/role/<roleId>?group=<groupID>
		String projectRoleUri = getProjectRolesUrl(projectUid, roleName);
		HttpDelete request;
		try {
			URIBuilder uri = new URIBuilder(projectRoleUri);
			uri.addParameter(actorName, userOrGroupNames);
			request = new HttpDelete(uri.build());
			callRequest(request, false, CONTENT_TYPE_JSON);
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Removing actor from project role failed: Problem occuerrd during building URI: ")
					.append(projectRoleUri).append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString(), e);
		}
	}

	Uid createOrUpdateProject(Uid uid, Set<Attribute> attrs) {
		if (attrs == null || attrs.isEmpty()) {
			LOG.error("Create or Update Project Operation failed: attributes not provided or empty.");
			throw new InvalidAttributeValueException("Missing mandatory attributes.");
		}
		String responseUid = null;
		Boolean create = uid == null;
		JSONObject body = new JSONObject();
		// Required attributes:
		// name:
		String name = getStringAttr(attrs, MIDPOINT_NAME);
		if (create && (name == null || StringUtil.isBlank(name))) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Missing mandatory attribute ").append(MIDPOINT_NAME);
			LOG.error(exceptionMsg.toString());
			throw new InvalidAttributeValueException(exceptionMsg.toString());
		}
		if (name != null)
			body.put(ATTR_NAME, name);
		// key:
		String key = getStringAttr(attrs, ATTR_KEY);
		if (create && (key == null || StringUtil.isBlank(key))) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Missing mandatory attribute ").append(ATTR_KEY);
			LOG.error(exceptionMsg.toString());
			throw new InvalidAttributeValueException(exceptionMsg.toString());
		}
		if (key != null)
			body.put(ATTR_KEY, key);
		// project type key-create:
		String projectTypeKey = getStringAttr(attrs, ATTR_PROJECT_TYPE_KEY);
		if (create && (projectTypeKey == null || StringUtil.isBlank(projectTypeKey))) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Missing mandatory attribute ").append(ATTR_PROJECT_TYPE_KEY);
			LOG.error(exceptionMsg.toString());
			throw new InvalidAttributeValueException(exceptionMsg.toString());
		}
		if (create && projectTypeKey != null)
			body.put(ATTR_PROJECT_TYPE_KEY, projectTypeKey);
		// project type key-update:
		// LOG.info("\n\tupdate project type key: \n\t\t create: {0}, \n\t\t
		// projecttypekey: {1}", create, projectTypeKey);
		if (!create && projectTypeKey != null && !StringUtil.isBlank(projectTypeKey)) {
			LOG.info("\n\tUPDATED project type key!!!");
			updateProjectTypeKey(uid, projectTypeKey);
		}
		// lead:
		String lead = getStringAttr(attrs, ATTR_LEAD);
		if (create && (lead == null || StringUtil.isBlank(lead))) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Missing mandatory attribute ").append(ATTR_LEAD);
			LOG.error(exceptionMsg.toString());
			throw new InvalidAttributeValueException(exceptionMsg.toString());
		}
		if (lead != null)
			body.put(ATTR_LEAD, lead);
		// optional attributes:
		putFieldIfExists(attrs, ATTR_TEMPLATE_KEY, body);
		putFieldIfExists(attrs, ATTR_DESCRIPTION, body);
		putFieldIfExists(attrs, ATTR_URL, body);
		putFieldIfExists(attrs, ATTR_ASSIGNEE_TYPE, body); // available values:
															// PROJECT_LEAD,
															// UNASSIGNED
		putFieldIfExists(attrs, ATTR_AVATAR_ID, body);
		putFieldIfExists(attrs, ATTR_ISSUE_SECURITY_SCHEME, body);
		putFieldIfExists(attrs, ATTR_PERMISSION_SCHEME, body);
		putFieldIfExists(attrs, ATTR_NOTIFICATION_SCHEME, body);
		putFieldIfExists(attrs, ATTR_CATEGORY_ID, body);

		URIBuilder uri = getURIBuilder();
		try {
			HttpEntityEnclosingRequestBase request;

			if (create) {
				// create:
				uri.setPath(URI_BASE_PATH + URI_PROJECT_PATH);
				request = new HttpPost(uri.build());
			} else {
				// update:
				uri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + uid.getUidValue());
				request = new HttpPut(uri.build());
			}

			// execute request only when response body is not empty (not when
			// only avatar or password are updated):
			if (body.length() > 0) {
				// LOG.info("\n\tbody is not empty: {0} - {1}", body.length(),
				// body.toString());
				JSONObject jo = callRequest(request, body, true, CONTENT_TYPE_JSON);
				responseUid = jo.getString(ATTR_KEY);
			} else {
				// LOG.ok("json body: ", json.getString(passwd));
				JSONObject jo = callRequest(request, body, true, CONTENT_TYPE_JSON);
				responseUid = jo.get(ATTR_ID).toString();
			}

			// create or update user including attribute avatar:
			byte[] binaryAvatar = getByteArrayAttr(attrs, ATTR_AVATAR_BYTE_ARRRAY);
			if (binaryAvatar != null) {
				LOG.info("\n\tupdating avatar");
				createOrUpdateAvatar(binaryAvatar, responseUid, PROJECT_NAME);
				// update only avatar:
				if (attrs.size() == 1) {
					responseUid = uid.getUidValue();
				}
			}

			return new Uid(responseUid);

		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Create or Update Project Operation failed: Problem occuerrd during building URI: ")
					.append(getURIBuilder()).append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString(), e);
		}
	}

	void updateProjectTypeKey(Uid uid, String projectTypeKey) {

		URIBuilder uri = getURIBuilder();
		uri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + uid.getUidValue() + "/type/" + projectTypeKey);
		HttpEntityEnclosingRequestBase request;
		try {
			request = new HttpPut(uri.build());

			callRequest(request, false, CONTENT_TYPE_JSON);
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Update project type key failed: Problem occuerrd during building URI: ").append(uri)
					.append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString(), e);
		}
	}

	void readProjects(Filter query, ResultsHandler handler, OperationOptions options) {
		URIBuilder getUri = getURIBuilder();
		JSONArray objectsArray;
		// get all projects
		if (query == null) {
			objectsArray = getAllProjects(getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH));
			if (objectsArray != null) {
				handleProjectObjects(objectsArray, handler, options, false);
			}
		} else
		// get project by id:
		if (query instanceof EqualsFilter && ((EqualsFilter) query).getAttribute() instanceof Uid) {
			Uid uid = (Uid) ((EqualsFilter) query).getAttribute();
			if (uid != null) {
				JSONObject project = getProjectByUid(uid.getUidValue());
				if (project != null) {
					ConnectorObject connectorObject = convertProjectToConnectorObject(project, true);
					handler.handle(connectorObject);
				}
			} else
				throwNullAttrException(query);
		} else
		// search projects using ContainsFilter:
		if (query instanceof ContainsFilter) {
			// Name name = (Name) ((ContainsFilter) query).getAttribute();
			String attrValue = ((ContainsFilter) query).getAttribute().getValue().get(0).toString();
			String attrName = getAttrName(query);
			// LOGGER.info("\n\tname: {0}, vale: {1}, ", attrName,
			// attrValue);
			if (attrValue != null) {
				// getUri = getURIBuilder();
				JSONArray allProjectsArray = getAllProjects(getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH));
				objectsArray = substringFiltering(allProjectsArray, attrName, attrValue);
				if (objectsArray != null)
					handleProjectObjects(objectsArray, handler, options, false);
			} else
				throwNullAttrException(query);
		} else
		// search projects using ContainsAllValuesFilter:
		if (query instanceof ContainsAllValuesFilter) {
			List<Object> attrValuesList = ((ContainsAllValuesFilter) query).getAttribute().getValue();
			String attrName = ((ContainsAllValuesFilter) query).getAttribute().getName();
			JSONArray allProjects = getAllProjects(getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH));
			JSONArray suitableProjects = new JSONArray();
			List<String> actors = new ArrayList<>();
			String projectId;
			String administratorRoleId = getRoleIds().get(ATTR_ADMINISTRATORS);
			String developerRoleId = getRoleIds().get(ATTR_DEVELOPERS);
			for (int i = 0; i < allProjects.length(); i++) {
				projectId = (String) allProjects.getJSONObject(i).get(ATTR_ID);

				if (attrName.equals(ATTR_ADMINISTRATORS_USERS)) {
					// GET /rest/api/2/project/<project_id>/role/10002
					actors = getProjectActors(getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH) + "/" + projectId
							+ "/role/" + administratorRoleId, "user");
					if (actors.contains(attrValuesList.get(0))) { // just single
																	// value is
																	// expected
						suitableProjects.put(allProjects.get(i));
					}

				} else if (attrName.equals(ATTR_ADMINISTRATORS_GROUPS)) {
					// GET /rest/api/2/project/<project_id>/role/10002
					actors = getProjectActors(getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH) + "/" + projectId
							+ "/role/" + administratorRoleId, "group");
					if (actors.contains(attrValuesList.get(0))) { // just single
																	// value is
																	// expected
						suitableProjects.put(allProjects.get(i));
					}

				} else if (attrName.equals(ATTR_DEVELOPERS_USERS)) {
					// GET /rest/api/2/project/<project_id>/role/10100
					actors = getProjectActors(getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH) + "/" + projectId
							+ "/role/" + developerRoleId, "user");
					if (actors.contains(attrValuesList.get(0))) { // just single
																	// value is
																	// expected
						suitableProjects.put(allProjects.get(i));
					}

				} else if (attrName.equals(ATTR_DEVELOPERS_GROUPS)) {
					// GET /rest/api/2/project/<project_id>/role/10100"
					actors = getProjectActors(getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH) + "/" + projectId
							+ "/role/" + developerRoleId, "group");
					if (actors.contains(attrValuesList.get(0))) { // just single
																	// value is
																	// expected
						suitableProjects.put(allProjects.get(i));
					}
				}
			}
			//LOG.ok("\n\tSuitable projects: {0}", suitableProjects.toString());
			handleProjectObjects(suitableProjects, handler, options, true);

		} else {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("\n\tUnsuported query filter for Project Object Class'").append(query)
					.append("' or its attribute name '").append(((AttributeFilter) query).getAttribute().getName())
					.append("'.");
			LOG.error(exceptionMsg.toString());
			throw new NoSuchMethodError(exceptionMsg.toString());
		}

	}

	private HashMap<String, String> getRoleIds() {
		HashMap<String, String> roleIds = new HashMap<String, String>();
		URIBuilder getUri = getURIBuilder();
		getUri.setPath(URI_BASE_PATH + "/" + URI_ROLE);
		try {
			HttpGet request = new HttpGet(getUri.build());
			JSONArray allRoles = callRequest(request);
			for (int i = 0; i < allRoles.length(); i++) {
				String name = allRoles.getJSONObject(i).getString(ATTR_NAME);
				Integer id = allRoles.getJSONObject(i).getInt(ATTR_ID);
				if (name.equals(ATTR_ADMINISTRATORS)) {
					roleIds.put(ATTR_ADMINISTRATORS, id.toString());
				}
				if (name.equals(ATTR_DEVELOPERS)) {
					roleIds.put(ATTR_DEVELOPERS, id.toString());
				}
			}
			return roleIds;
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
					.append(" (getting all projects) \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		}
	}

	private JSONArray getAllProjects(URIBuilder getUri) {
		try {
			HttpGet request = new HttpGet(getUri.build());
			JSONArray objectsArray = callRequest(request);
			return objectsArray;
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
					.append(" (getting all projects) \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		}
	}

	private String getAttrName(Filter query) {
		if (((ContainsFilter) query).getAttribute() instanceof Name) {
			return "name";
		} else
			return ((ContainsFilter) query).getAttribute().getName();
	}

	// parse projects from multiple json:
	private boolean handleProjectObjects(JSONArray objectsArray, ResultsHandler handler, OperationOptions options, Boolean isRolesNeeded) {
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

			// LOGGER.info("\n\tConverting Project...");
			connectorObject = convertProjectToConnectorObject(object, isRolesNeeded);

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

	private ConnectorObject convertProjectToConnectorObject(JSONObject project, Boolean isRolesNeeded) {
		if (project == null) {
			String exceptionMsg = "Conversion Project to Connector Object failed: JSONObject representing Project object is not provided or is empty";
			LOG.error(exceptionMsg);
			throw new InvalidAttributeValueException(exceptionMsg);
		}

		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
		builder.setObjectClass(new ObjectClass(PROJECT_NAME));

		Uid uid = getUidIfExists(project, ATTR_ID, builder);
		getNameIfExists(project, ATTR_NAME, builder);
		getIfExists(project, ATTR_SELF, builder, IS_SINGLE_VALUE);
		getIfExists(project, ATTR_EXPAND, builder, IS_SINGLE_VALUE);
		getIfExists(project, ATTR_KEY, builder, IS_SINGLE_VALUE);
		getIfExists(project, ATTR_DESCRIPTION, builder, IS_SINGLE_VALUE);
		getIfExists(project, ATTR_URL, builder, IS_SINGLE_VALUE);
		getIfExists(project, ATTR_ASSIGNEE_TYPE, builder, IS_SINGLE_VALUE);
		getIfExists(project, ATTR_PROJECT_TYPE_KEY, builder, IS_SINGLE_VALUE);

		if (project.has(ATTR_AVATAR_URLS)) {
			JSONObject avatars = project.getJSONObject(ATTR_AVATAR_URLS);
			// LOGGER.info("\n\tavatars: {0}", avatars);
			ArrayList<String> avatarUrls = new ArrayList<String>();
			avatarUrls.add((String) avatars.getString("16x16"));
			avatarUrls.add((String) avatars.getString("24x24"));
			avatarUrls.add((String) avatars.getString("32x32"));
			avatarUrls.add((String) avatars.getString("48x48"));
			builder.addAttribute(ATTR_AVATAR_URLS, avatarUrls);

			byte[] avatarArray = getAvatar(avatarUrls.get(3), new ObjectClass(PROJECT_NAME));
			// LOGGER.info("\n\tgetting avatar: {0}",
			// avatarsArray[3].toString());
			builder.addAttribute(ATTR_AVATAR_BYTE_ARRRAY, avatarArray);
		}

		if (project.has(ATTR_LEAD)) {
			JSONObject attrLead = project.getJSONObject(ATTR_LEAD);
			builder.addAttribute(ATTR_LEAD, attrLead.get(EXTENDED_ATTR_NAME));
		}

		// get attribute roles and then project actors:
		JSONObject attrRoles = null;
		if (isRolesNeeded && !project.has(ATTR_ROLES)) { // JSON Object project does not contain
										// attribute Roles when getting all
										// projects
			if (uid != null) {
				JSONObject projectObj = getProjectByUid(uid.getUidValue());
				attrRoles = projectObj.getJSONObject(ATTR_ROLES);
			}
		}
		if (project.has(ATTR_ROLES)) { // JSON Object project contains attribute
										// Roles when getting the specific
										// project
			attrRoles = project.getJSONObject(ATTR_ROLES);
		}
		if (isRolesNeeded && attrRoles != null) {
			// add role URIs to attributes:
			String administratorsUrl = attrRoles.getString(ATTR_ADMINISTRATORS);
			String developersUrl = attrRoles.getString(ATTR_DEVELOPERS);
			builder.addAttribute(ATTR_ROLES_DEVELOPERS, developersUrl);
			builder.addAttribute(ATTR_ROLES_ADMINISTRATORS, administratorsUrl);
			// add project actors (users/groups):
			builder.addAttribute(ATTR_ADMINISTRATORS_GROUPS, getProjectActors(administratorsUrl, "group").toArray());
			builder.addAttribute(ATTR_ADMINISTRATORS_USERS, getProjectActors(administratorsUrl, "user").toArray());
			builder.addAttribute(ATTR_DEVELOPERS_GROUPS, getProjectActors(developersUrl, "group").toArray());
			builder.addAttribute(ATTR_DEVELOPERS_USERS, getProjectActors(developersUrl, "user").toArray()); // new
																											// String[0]
		}

		getIfExists(project, ATTR_ISSUE_TYPES, builder, IS_MULTI_VALUE);
		getIfExists(project, ATTR_VERSIONS, builder, IS_MULTI_VALUE);
		getIfExists(project, ATTR_COMPONENTS, builder, IS_MULTI_VALUE);

		ConnectorObject connectorObject = builder.build();
		// LOG.ok("\nConvert Project to connector object, project: {0},
		// \n\tconnector object: {1}", project.getString(ATTR_NAME),
		// connectorObject);
		return connectorObject;
	}

	private List<String> getProjectActors(String url, String actorName) {
		/// GET api/2/project/<projectUid>/role/<roleId>
		URIBuilder getUri;
		try {
			// String[] actors = null;
			List<String> actors = new ArrayList<String>();
			getUri = new URIBuilder(url);
			HttpGet request = new HttpGet(getUri.build());
			JSONObject jo = callRequest(request, true, CONTENT_TYPE_JSON);
			// LOGGER.info("\n\tJSON Object: {0}", jo.toString());
			// get array of actor names:
			ArrayList<String> actorNames = getMultiAttrItems(jo, ATTR_ACTORS, ATTR_NAME);
			// get array of actor types:
			ArrayList<String> actorTypes = getMultiAttrItems(jo, ATTR_ACTORS, ATTR_TYPE);

			// if JSON array 'actors' is empty then return empty Array List:
			if (actorNames == null || actorTypes == null) {
				return actors;// .toArray(new String[0]);
			}

			if (actorNames.size() == actorTypes.size()) {
				int size = actorNames.size();
				for (int i = 0; i < size; i++) {
					// add actor names to list according to actor type -
					// containing actorName ('user' or 'group')
					if (actorTypes.get(i).contains(actorName)) {
						actors.add(actorNames.get(i));
						// LOGGER.info("\n\tactor name: {0} - {1}", i,
						// actors.get(i));
					}
				}
			}
			return actors;// .toArray(new String[0]);
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Get project actors failed: problem occurred during executing URI: ").append(url)
					.append(", for actors ").append(actorName).append("\n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		}
	}

	private JSONObject getProjectByUid(String uid) {
		URIBuilder getUri = getURIBuilder();
		getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + uid);
		try {
			HttpGet request = new HttpGet(getUri.build());
			JSONObject project = callRequest(request, true, CONTENT_TYPE_JSON);
			return project;
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
					.append(", using uid attribute: ").append(uid).append("\n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		}
	}

	void deleteProject(Uid uid) {
		URIBuilder deleteUri = getURIBuilder().setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + uid.getUidValue());
		// LOG.ok("delete project, Name: {0}", uid);
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

	void removeMembersFromProject(Uid uid, Attribute attr) {
		if (attr.getName().equals(ATTR_ADMINISTRATORS_USERS)) {
			for (Object username : attr.getValue()) {
				String userId = getUserIdFromName((String) username);
				removeActorsFromProjectRole(uid, userId, ATTR_ACTOR_USER, ATTR_ADMINISTRATORS);
			}
		}
		// add Administrators.groups to Project role:
		if (attr.getName().equals(ATTR_ADMINISTRATORS_GROUPS)) {
			for (Object groupName : attr.getValue()) {
				removeActorsFromProjectRole(uid, (String) groupName, ATTR_ACTOR_GROUP, ATTR_ADMINISTRATORS);
			}
		}
		// add Developers.users to Project role:
		if (attr.getName().equals(ATTR_DEVELOPERS_USERS)) {
			for (Object username : attr.getValue()) {
				String userId = getUserIdFromName((String) username);
				removeActorsFromProjectRole(uid, userId, ATTR_ACTOR_USER, ATTR_DEVELOPERS);
			}

		}
		// add Developers.groups to Project role:
		if (attr.getName().equals(ATTR_DEVELOPERS_GROUPS)) {
			for (Object groupName : attr.getValue()) {
				removeActorsFromProjectRole(uid, (String) groupName, ATTR_ACTOR_GROUP, ATTR_DEVELOPERS);
			}
		}
	}

	void addMembersToProject(Uid uid, Attribute attr) {
		if (attr.getName().equals(ATTR_ADMINISTRATORS_USERS)) {
			JSONArray userIds = new JSONArray();
			for (Object username : attr.getValue()) {
				userIds.put(getUserIdFromName((String) username));
			}
			addActorsToProjectRole(uid, userIds, ATTR_ACTOR_USER, ATTR_ADMINISTRATORS);
		}
		// add Administrators.groups to Project role:
		if (attr.getName().equals(ATTR_ADMINISTRATORS_GROUPS)) {
			JSONArray groupIds = new JSONArray(attr.getValue());
			addActorsToProjectRole(uid, groupIds, ATTR_ACTOR_GROUP, ATTR_ADMINISTRATORS);
		}
		// add Developers.users to Project role:
		if (attr.getName().equals(ATTR_DEVELOPERS_USERS)) {
			JSONArray userIds = new JSONArray();
			for (Object username : attr.getValue()) {
				userIds.put(getUserIdFromName((String) username));
			}
			addActorsToProjectRole(uid, userIds, ATTR_ACTOR_USER, ATTR_DEVELOPERS);
		}
		// add Developers.groups to Project role:
		if (attr.getName().equals(ATTR_DEVELOPERS_GROUPS)) {
			JSONArray groupIds = new JSONArray(attr.getValue());
			addActorsToProjectRole(uid, groupIds, ATTR_ACTOR_GROUP, ATTR_DEVELOPERS);
		}

	}
}
