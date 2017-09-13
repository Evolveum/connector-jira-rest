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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException;
import org.identityconnectors.framework.common.exceptions.PreconditionFailedException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author surmanek
 *
 */
public class JiraObjectsProcessing {

	static final Log LOG = Log.getLog(JiraConnector.class);
	static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
	JiraConfiguration configuration;

	static final String URI_BASE_PATH = "/rest/api/2";
	static final String URI_USER_PATH = "/user";
	static final String URI_SEARCH_PATH = "/search";
	static final String PARAM_USERNAME = "username";
	static final String PARAM_START_AT = "startAt";
	static final String PARAM_MAX_RESULTS = "maxResults";
	static final String ATTR_GROUPS = "groups";
	static final String ATTR_AVATAR_URLS = "avatarUrls";

	static final String USER_NAME = "USER";
	static final String PROJECT_NAME = "PROJECT";

	static final String URI_PASSWORD_PATH = "/password";
	static final String URI_TEMP_AVATAR_PATH = "/avatar/temporary";
	static final String URI_AVATAR_PATH = "/avatar";
	static final String URI_GROUP_PATH = "/group";
	static final String URI_GROUPS_PICKER_PATH = "/groups/picker";
	static final String URI_PROJECT_PATH = "/project";

	static final String PARAM_FILENAME = "filename";
	static final String PARAM_KEY = "key";
	static final String CONTENT_TYPE_JPEG_IMAGE = "image/jpeg";
	static final String UID = "key";

	// project:
	static final String ATTR_DEVELOPERS_GROUPS = "Developers.groups";
	static final String ATTR_DEVELOPERS_USERS = "Developers.users";
	static final String ATTR_ADMINISTRATORS_GROUPS = "Administrators.groups";
	static final String ATTR_ADMINISTRATORS_USERS = "Administrators.users";
	static final String ATTR_ACTOR_USER = "user";
	static final String ATTR_ACTOR_GROUP = "group";
	static final String ATTR_DEVELOPERS = "Developers";
	static final String ATTR_ADMINISTRATORS = "Administrators";

	// user+project:
	static final String ATTR_KEY = "key";
	static final String ATTR_AVATAR_BYTE_ARRRAY = "binaryAvatar";
	// user+group:
	static final String ATTR_SELF = "self";
	static final String ATTR_NAME = "name";
	static final String ATTR_EXPAND = "expand";

	static final String MIDPOINT_NAME = "__NAME__";
	static final boolean IS_MULTI_VALUE = true;
	static final boolean IS_SINGLE_VALUE = false;
	static final String EXTENDED_ATTR_NAME = "name";
	static final String EXTENDED_ATTR_ITEMS = "items";

	public JiraObjectsProcessing(JiraConfiguration configuration) {
		this.configuration = configuration;
	}

	JSONObject callRequest(HttpEntityEnclosingRequestBase request, JSONObject jo, Boolean parseResult,
			String contentType) {
		// don't log request here - password field !!!
		if (contentType != null)
			request.addHeader("Content-Type", contentType);
		request.addHeader("Authorization", "Basic " + authEncoding());
		HttpEntity entity;
		try {
			entity = new ByteArrayEntity(jo.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			String exceptionMsg = "Creating request entity failed: problem occured during entity encoding.";
			LOG.error(exceptionMsg);
			throw new ConnectorIOException(exceptionMsg);
		}
		request.setEntity(entity);
		try (CloseableHttpResponse response = execute(request)){
			
			
			
			//LOG.ok("Request: {0}", request.toString());
			//response = execute(request);
			LOG.ok("Response: {0}", response);
			processResponseErrors(response);

			if (!parseResult) {
				return null;
			}
			
			String result = EntityUtils.toString(response.getEntity());

			LOG.ok("Response body: {0}", result);
			return new JSONObject(result);
		} catch (IOException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Request failed: problem occured during execute request with uri: ")
					.append(request.getURI()).append(": \n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorIOException(exceptionMsg.toString(), e);
		}
	}

	JSONObject callRequest(HttpRequestBase request, Boolean parseResult, String contentType) {
		// don't log request here - password field !!!
		//CloseableHttpResponse response = null;
		LOG.ok("request URI: {0}", request.getURI());
		request.addHeader("Content-Type", contentType);
		request.addHeader("Authorization", "Basic " + authEncoding());
		try (CloseableHttpResponse response = execute(request)){
			
			LOG.ok("Response: {0}", response);
			processResponseErrors(response);

			if (!parseResult) {
				//closeResponse(response);
				return null;
			}
			// DO NOT USE getEntity() TWICE!!!
			String result = EntityUtils.toString(response.getEntity());
			//closeResponse(response);
			LOG.ok("Response body: {0}", result);
			return new JSONObject(result);
		} catch (IOException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Request failed: problem occured during execute request with uri: ")
					.append(request.getURI()).append(": \n\t").append(e.getLocalizedMessage());
			//closeResponse(response);
			LOG.error(exceptionMsg.toString());
			throw new ConnectorIOException(exceptionMsg.toString(), e);
		}
	}

	JSONArray callRequest(HttpRequestBase request) {
		//CloseableHttpResponse response = null;
		LOG.ok("request URI: {0}", request.getURI());
		request.addHeader("Content-Type", CONTENT_TYPE_JSON);
		request.addHeader("Authorization", "Basic " + authEncoding());
		try (CloseableHttpResponse response = execute(request)) {
			
			//response = execute(request);
			LOG.ok("Response: {0}", response);
			processResponseErrors(response);
			// DO NOT USE getEntity() TWICE!!!
			String result = EntityUtils.toString(response.getEntity());
			//closeResponse(response);
			LOG.ok("Response body: {0}", result);
			return new JSONArray(result);
		} catch (IOException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Request failed: problem occured during execute request with URI: ")
					.append(request.getURI()).append(": \n\t").append(e.getLocalizedMessage());
			//closeResponse(response);
			LOG.error(exceptionMsg.toString());
			throw new ConnectorIOException(exceptionMsg.toString(), e);
		}
	}

	String authEncoding() {
		String username = configuration.getUsername();
		String password = configuration.getStringPassword();
		if (username == null || username.equals("")) {
			LOG.error("Authentication failed: Username is not provided.");
			throw new InvalidCredentialException("Authentication failed: Username is not provided.");
		}
		if (password == null || password.equals("")) {
			LOG.error("Authentication failed: Password is not provided.");
			throw new InvalidPasswordException("Authentication failed: Password is not provided.");
		}
		StringBuilder nameAndPasswd = new StringBuilder();
		nameAndPasswd.append(username).append(":").append(password);
		// String nameAndPasswd = "administrator:training"
		String encoding = Base64.encodeBase64String(nameAndPasswd.toString().getBytes());
		return encoding;
	}

	CloseableHttpResponse execute(HttpUriRequest request) {
		CloseableHttpClient client = HttpClientBuilder.create().build();
		try {
			CloseableHttpResponse response = (CloseableHttpResponse) client.execute(request);
			// print response code:
			LOG.ok("response code: {0}", String.valueOf(response.getStatusLine().getStatusCode()));
			// client.close();
			// DO NOT CLOSE response HERE !!!
			return response;
		} catch (IOException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Execution of the request failed: problem occured during HTTP client execution: \n\t")
					.append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorIOException(exceptionMsg.toString());
		}
	}

	/**
	 * Checks HTTP response for errors. If the response is an error then the
	 * method throws the ConnId exception that is the most appropriate match for
	 * the error.
	 */
	void processResponseErrors(CloseableHttpResponse response) {
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode >= 200 && statusCode <= 299) {
			return;
		}
		String responseBody = null;
		try {
			responseBody = EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			LOG.warn("cannot read response body: " + e, e);
		}

		StringBuilder message = new StringBuilder();
		message.append("HTTP error ").append(statusCode).append(" ").append(response.getStatusLine().getReasonPhrase())
				.append(" : ").append(responseBody);
		if (statusCode == 401 || statusCode == 403) {
			StringBuilder anauthorizedMessage = new StringBuilder(); // response
																		// body
																		// of
																		// status
																		// code
																		// 401
																		// contains
																		// binary
																		// data.
			anauthorizedMessage.append("HTTP error ").append(statusCode).append(" ")
					.append(response.getStatusLine().getReasonPhrase())
					.append(" : Provided credentials are incorrect.");
			closeResponse(response);
			LOG.error("{0}", anauthorizedMessage.toString());
			throw new InvalidCredentialException(anauthorizedMessage.toString());
		}
		LOG.error("{0}", message.toString());
		if ((statusCode == 400 || statusCode == 404) && message.toString().contains("already")) {
			closeResponse(response);
			LOG.error(message.toString());
			throw new AlreadyExistsException(message.toString());
		}
		if (statusCode == 400 || statusCode == 405 || statusCode == 406) {
			closeResponse(response);
			LOG.error(message.toString());
			throw new ConnectorIOException(message.toString());
		}
		if (statusCode == 402 || statusCode == 407) {
			closeResponse(response);
			LOG.error(message.toString());
			throw new PermissionDeniedException(message.toString());
		}
		if (statusCode == 404 || statusCode == 410) {
			closeResponse(response);
			LOG.error(message.toString());
			throw new UnknownUidException(message.toString());
		}
		if (statusCode == 408) {
			closeResponse(response);
			LOG.error(message.toString());
			throw new OperationTimeoutException(message.toString());
		}
		if (statusCode == 412) {
			closeResponse(response);
			LOG.error(message.toString());
			throw new PreconditionFailedException(message.toString());
		}
		if (statusCode == 418) {
			closeResponse(response);
			LOG.error(message.toString());
			throw new UnsupportedOperationException("Sorry, no cofee: " + message.toString());
		}

		closeResponse(response);
		LOG.error(message.toString());
		throw new ConnectorException(message.toString());
	}

	void closeResponse(CloseableHttpResponse response) {
		// to avoid pool waiting
		if (response == null)
			return;
		try {
			response.close();
		} catch (IOException e) {
			LOG.warn(e, "Failed to close response: " + response);
		}
	}
	
	void closeClient(CloseableHttpClient client){
		if (client == null){
			return;
		}
		try {
			client.close();
		} catch (IOException e) {
			LOG.warn(e, "Failed to close client.");
		}
	}

	// filter json objects by substring:
	JSONArray substringFiltering(JSONArray inputJsonArray, String attrName, String subValue) {
		JSONArray jsonArrayOut = new JSONArray();
		// String attrName = attribute.getName().toString();
		// LOGGER.info("\n\tSubstring filtering: {0} ({1})", attrName,
		// subValue);
		for (int i = 0; i < inputJsonArray.length(); i++) {
			JSONObject jsonObject = inputJsonArray.getJSONObject(i);
			if (!jsonObject.has(attrName)) {
				LOG.warn("\n\tProcessing JSON Object does not contain attribute {0}.", attrName);
				return null;
			}
			if (jsonObject.has(attrName) && (jsonObject.get(attrName)).toString().contains(subValue)) {
				// LOG.ok("value: {0}, subValue: {1} - MATCH: {2}",
				// jsonObject.get(attrName).toString(), subValue, "YES");
				jsonArrayOut.put(jsonObject);
			}
			// else LOG.ok("value: {0}, subValue: {1} - MATCH: {2}",
			// jsonObject.getString(attrName), subValue, "NO");
		}
		return jsonArrayOut;
	}

	JSONArray startsWithFiltering(Filter query, Attribute attr, OperationOptions options, String objClass) {
		String attrValue = attr.getValue().get(0).toString();
		HttpGet request;
		URIBuilder getUri = null;
		if (attrValue != null) {
			try {
				getUri = getURIBuilder();
				getUri.setPath(URI_BASE_PATH + URI_USER_PATH + URI_SEARCH_PATH);
				getUri.addParameter(PARAM_USERNAME, attrValue);
				if (options.getPagedResultsOffset() != null || options.getPageSize() != null) {
					int pageNumber = options.getPagedResultsOffset();
					int usersPerPage = options.getPageSize();
					int startAt = (pageNumber * usersPerPage) - usersPerPage;
					//LOG.info("\n\tpage: {0}, users per page {1}, start at: {2}", pageNumber, usersPerPage, startAt);
					getUri.addParameter(PARAM_MAX_RESULTS, String.valueOf(usersPerPage));
					getUri.addParameter(PARAM_START_AT, String.valueOf(startAt));
				}
				;
				request = new HttpGet(getUri.build());

				JSONArray objectsArray = new JSONArray();
				if (objClass.equals(ObjectClass.GROUP_NAME)) {
					JSONObject object = callRequest(request, true, CONTENT_TYPE_JSON);
					objectsArray = object.getJSONArray(ATTR_GROUPS);
				}
				if (objClass.equals(ObjectClass.ACCOUNT_NAME) || objClass.equals(PROJECT_NAME)) {
					objectsArray = callRequest(request);
				}
				// handleObjects(objectsArray, handler, options, objClass);
				return objectsArray;
			} catch (URISyntaxException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Get operation failed: problem occurred during executing URI: ").append(getUri)
						.append(", using attribute: ").append(attrValue).append("\n\t").append(e.getLocalizedMessage());
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			}
		} else
			throwNullAttrException(query);
		return null;
	}

	// method called when attribute of query filter is null:
	void throwNullAttrException(Filter query) {
		StringBuilder exceptionMsg = new StringBuilder();
		exceptionMsg
				.append("Get operation failed: problem occurred because of not provided attribute of query filter: ")
				.append(query);
		LOG.error(exceptionMsg.toString());
		throw new InvalidAttributeValueException(exceptionMsg.toString());
	}

	// create uri from base host:
	URIBuilder getURIBuilder() {
		String baseHost = configuration.getBaseUrl();
		URIBuilder uri = new URIBuilder();
		uri.setScheme("http");
		uri.setHost(baseHost);
		uri.setPath(URI_BASE_PATH);
		return uri;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// avatar processing:
	void createOrUpdateAvatar(byte[] avatar, String uid, String objectName) {
		// crop image to square and resize it to needed size:
		byte[] resizedImage = resizeAndCropImage(avatar, 48, 48);
		String username = null, key = null;

		CloseableHttpClient client = HttpClientBuilder.create().build();

		boolean needsCropping = false;
		int cropperWidth, cropperOffsetX, cropperOffsetY;
		JSONObject cropBody = new JSONObject();
		URIBuilder uri = getURIBuilder();
		JSONObject responseObject = new JSONObject();
		String avatarId = null;

		// delete old avatar before the new one will be successfully set:
		deleteAvatar(uid, objectName);

		// uri for uploading user avatar:
		if (objectName.equals(USER_NAME)) { // USER
			username = getUsernameFromUid(uid);
			uri.setPath(URI_BASE_PATH + URI_USER_PATH + URI_TEMP_AVATAR_PATH);
			uri.addParameter(PARAM_USERNAME, username);
			uri.addParameter(PARAM_FILENAME, username + ".jpeg");
		}

		// uri for uploading project avatar
		if (objectName.equals(PROJECT_NAME)) { // PROJECT
			key = getProjectKeyFromUid(uid);
			uri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + key + URI_TEMP_AVATAR_PATH);
			uri.addParameter(PARAM_FILENAME, key + ".jpeg");
			// LOGGER.info("\n\tupload project avatar: {0}", uri.toString());
		}

		HttpEntityEnclosingRequestBase postRequest;
		HttpEntityEnclosingRequestBase putRequest;
		CloseableHttpResponse response = null;
		// 1st step: upload avatar:
		try {
			postRequest = new HttpPost(uri.build());
			if (resizedImage != null) {
				postRequest.setHeader("X-Atlassian-Token", "no-check");
				postRequest.setHeader("Authorization", "Basic YWRtaW5pc3RyYXRvcjp0cmFpbmluZw==");
				postRequest.setHeader("Content-Type", CONTENT_TYPE_JPEG_IMAGE);
				postRequest.setEntity(new ByteArrayEntity(resizedImage));

				response = (CloseableHttpResponse) client.execute(postRequest);
				// print response code:
				//LOG.ok("response code: {0}", String.valueOf(response.getStatusLine().getStatusCode()));
				// client.close();
				// DO NOT CLOSE response HERE !!!
				processResponseErrors(response);
				String result = EntityUtils.toString(response.getEntity());

				closeResponse(response);
				responseObject = new JSONObject(result);

				if (responseObject.has("needsCropping")) {
					needsCropping = "true".equals(responseObject.get("needsCropping").toString());
					// LOGGER.info("\n\tneedsCropping-{0}", needsCropping);
				}
				if (responseObject.has("cropperWidth")) {
					cropperWidth = (int) responseObject.get("cropperWidth");
					// LOGGER.info("\n\tcropperWidth-{0}", cropperWidth);
					cropBody.put("cropperWidth", cropperWidth);
				}
				if (responseObject.has("cropperOffsetX")) {
					cropperOffsetX = (int) responseObject.get("cropperOffsetX");
					// LOGGER.info("\n\tcropperOffsetX-{0}",cropperOffsetX);
					cropBody.put("cropperOffsetX", cropperOffsetX);
				}
				if (responseObject.has("cropperOffsetY")) {
					cropperOffsetY = (int) responseObject.get("cropperOffsetY");
					// LOGGER.info("\n\tcropperOffsetY-{0}", cropperOffsetY);
					cropBody.put("cropperOffsetY", cropperOffsetY);
				}
				if (responseObject.has("id")) {
					avatarId = responseObject.getString("id");
					// LOGGER.info("\n\tid-{0}", cropperOffsetY);
				}
			}
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Uploading of the avatar failed: problem occurred during building URI: ").append(uri)
					.append("\n\t").append(e.getLocalizedMessage());
			closeResponse(response);
			closeClient(client);
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		} catch (IOException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Uploading of the avatar failed: problem occured during request execution: \n\t")
					.append(e.getLocalizedMessage());
			closeResponse(response);
			closeClient(client);
			LOG.error(exceptionMsg.toString());
			throw new ConnectorIOException(exceptionMsg.toString());
		}
		// 2nd step: crop avatar:
		if (needsCropping) {
			uri.removeQuery();
			if (objectName.equals(USER_NAME)) { // USER
				uri.setPath(URI_BASE_PATH + URI_USER_PATH + URI_AVATAR_PATH);
				uri.addParameter(PARAM_USERNAME, username);
			}
			if (objectName.equals(PROJECT_NAME)) { // PROJECT
				uri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + key + URI_AVATAR_PATH);
				LOG.info("\n\tcrop project avatar: {0}", uri.toString());
			}
			try {
				postRequest = new HttpPost(uri.build());
				postRequest.setHeader("X-Atlassian-Token", "no-check");
				postRequest.setHeader("Content-Type", CONTENT_TYPE_JSON);

				HttpEntity entity = new ByteArrayEntity(cropBody.toString().getBytes("UTF-8"));
				postRequest.setEntity(entity);

				response = (CloseableHttpResponse) client.execute(postRequest);
				// print response code:
				LOG.ok("response code: {0}", String.valueOf(response.getStatusLine().getStatusCode()));
				// client.close();
				// DO NOT CLOSE response HERE !!!
				processResponseErrors(response);
				String result = EntityUtils.toString(response.getEntity());

				closeResponse(response);
				LOG.ok("response body: {0}", result);
				responseObject = new JSONObject(result);

				avatarId = responseObject.getString("id");
				// LOGGER.info("\n\t2nd step: {0}", responseObject.toString());

			} catch (URISyntaxException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Cropping of the avatar failed: problem occurred during building URI: ").append(uri)
						.append("\n\t").append(e.getLocalizedMessage());
				closeResponse(response);
				closeClient(client);
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			} catch (UnsupportedEncodingException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg
						.append("Cropping of the avatar failed: problem occurred during encoding request body for URI: ")
						.append(uri).append("\n\t").append(e.getLocalizedMessage());
				closeResponse(response);
				closeClient(client);
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			} catch (IOException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Cropping of the avatar failed: problem occured during request execution: \n\t")
						.append(e.getLocalizedMessage());
				closeResponse(response);
				closeClient(client);
				LOG.error(exceptionMsg.toString());
				throw new ConnectorIOException(exceptionMsg.toString());
			}

		}
		// 3rd step: confirm avatar:
		
		try {
			uri.removeQuery();
			if (objectName.equals(USER_NAME)) { // USER
				uri.setPath(URI_BASE_PATH + URI_USER_PATH + URI_AVATAR_PATH);
				uri.addParameter(PARAM_USERNAME, username);
			}
			if (objectName.equals(PROJECT_NAME)) { // PROJECT
				uri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + key + URI_AVATAR_PATH);
				LOG.info("\n\tconfirm project avatar: {0}", uri.toString());
			}

			putRequest = new HttpPut(uri.build());
			putRequest.setHeader("X-Atlassian-Token", "no-check");
			putRequest.setHeader("Content-Type", CONTENT_TYPE_JSON);
			JSONObject confirmBody = new JSONObject();
			confirmBody.put("id", avatarId);

			HttpEntity entity = new ByteArrayEntity(confirmBody.toString().getBytes("UTF-8"));
			putRequest.setEntity(entity);

			response = (CloseableHttpResponse) client.execute(putRequest);

			LOG.ok("response code: {0}", String.valueOf(response.getStatusLine().getStatusCode()));
			// client.close();
			// DO NOT CLOSE response HERE !!!
			processResponseErrors(response);

			closeResponse(response);

		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Confirmation of the avatar failed: problem occurred during building URI: ").append(uri)
					.append("\n\t").append(e.getLocalizedMessage());
			closeResponse(response);
			closeClient(client);
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		} catch (UnsupportedEncodingException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg
					.append("Confirmation of the avatar failed: problem occurred during encoding request body for URI: ")
					.append(uri).append("\n\t").append(e.getLocalizedMessage());
			closeResponse(response);
			closeClient(client);
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		} catch (IOException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Confirmation of the avatar failed: problem occured during request execution: \n\t")
					.append(e.getLocalizedMessage());
			closeResponse(response);
			closeClient(client);
			LOG.error(exceptionMsg.toString());
			throw new ConnectorIOException(exceptionMsg.toString());
		}
	}

	void deleteAvatar(String uid, String objectName) {
		// user:
		// http://example.com:8080/jira/rest/api/2/user/avatar/<id>?username=<username>
		// project:
		// http://example.com:8080/jira/rest/api/2/project/<key>/avatar/<id>
		URIBuilder deleteUri = getURIBuilder();
		CloseableHttpResponse response = null;
		if (uid != null) {
			try {
				deleteUri = getURIBuilder();

				String avatarId = getAvatarId(uid, objectName);
				if (avatarId == null) {
					LOG.warn("Deleting of the avatar ignored: Requested avatar is probably default system avatar.");
					return;
				}

				if (objectName.equals(PROJECT_NAME)) {
					String key = getProjectKeyFromUid(uid);
					deleteUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + key + URI_AVATAR_PATH + "/" + avatarId);
				}
				if (objectName.equals(USER_NAME)) {
					String username = getUsernameFromUid(uid);
					deleteUri.setPath(URI_BASE_PATH + URI_USER_PATH + URI_AVATAR_PATH + "/" + avatarId);
					deleteUri.addParameter(PARAM_USERNAME, username);
				}

				HttpDelete request = new HttpDelete(deleteUri.build());
				request.addHeader("Authorization", "Basic " + authEncoding());
				response = execute(request);
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 401) {
					LOG.warn("Deleting of the avatar was ignored: Avatar with id {0} is probably system avatar.",
							avatarId);
					return;
				} else {
					processResponseErrors(response);
				}
				closeResponse(response);

			} catch (URISyntaxException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Deletion of the avatar failed: problem occurred during executing URI: ")
						.append(deleteUri).append(", using uid attribute: ").append(uid).append("\n\t")
						.append(e.getLocalizedMessage());
				closeResponse(response);
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			} 
		}
	}

	String getAvatarId(String uid, String objectName) {
		URIBuilder getUri = getURIBuilder();
		if (uid != null) {
			try {
				getUri = getURIBuilder();
				JSONObject object = null;

				if (objectName.equals(PROJECT_NAME)) {
					getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + uid);
				}
				if (objectName.equals(USER_NAME)) {
					getUri.setPath(URI_BASE_PATH + URI_USER_PATH);
					getUri.addParameter(PARAM_KEY, uid);
				}

				HttpGet request = new HttpGet(getUri.build());
				object = callRequest(request, true, CONTENT_TYPE_JSON);
				// LOGGER.info("project key: {0}", object.getString(ATTR_KEY));
				JSONObject avatarUrls = object.getJSONObject(ATTR_AVATAR_URLS);
				String avatarUrl = avatarUrls.getString("48x48");
				URIBuilder avatarUri = new URIBuilder(avatarUrl);
				List<NameValuePair> queryParameters = avatarUri.getQueryParams();

				for (NameValuePair pair : queryParameters) {
					String name = pair.getName();
					if (name.equals("avatarId")) {
						return pair.getValue();
					}
				}

			} catch (URISyntaxException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Getting avatar uid failed: problem occurred during executing URI: ").append(getUri)
						.append("\n\t").append(e.getLocalizedMessage());
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			}
		}
		return null;
	}

	byte[] resizeAndCropImage(byte[] image, int width, int height) {
		ByteArrayInputStream bis = new ByteArrayInputStream(image);
		BufferedImage buffImage;
		try {
			buffImage = ImageIO.read(bis);
			if (buffImage == null) {
				String exceptionMsg = "\n\tBuffering of the avatar for resize failed!";
				LOG.error(exceptionMsg);
				throw new ConnectorIOException(exceptionMsg);
			}
			// crop image if needed:
			int originalWidth = buffImage.getWidth();
			int originalHeight = buffImage.getHeight();
			BufferedImage croppedBuffImage;
			if (originalWidth > originalHeight) {
				croppedBuffImage = buffImage.getSubimage((originalWidth / 2 - originalHeight / 2), 0, originalHeight,
						originalHeight);
			} else if (originalHeight > originalWidth) {
				croppedBuffImage = buffImage.getSubimage(0, (originalHeight / 2 - originalWidth / 2), originalWidth,
						originalWidth);
			} else {
				croppedBuffImage = buffImage;
			}

			// LOGGER.info("\n\tConverting image format and size...");
			// resize image:
			BufferedImage resizedBuffImage = new BufferedImage(width, height, 5);
			Graphics2D imgGraphics = resizedBuffImage.createGraphics();
			imgGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			imgGraphics.drawImage(croppedBuffImage, 0, 0, width, height, null);
			imgGraphics.dispose();

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			// LOGGER.info("\n\tWritting output image...");
			if (ImageIO.write(resizedBuffImage, "jpeg", bos) == false) {
				LOG.error("\n\tConverting image format and size faild.");
				return null;
			} else {
				// LOGGER.info("\n\tConverting finished successfully.");
				byte[] resizedImage = bos.toByteArray();
				// ImageIO.write(resizedImage, "png", new File("D:\\out.png"));
				return resizedImage;

			}
		} catch (IOException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg
					.append("Converting avatar image format failed: problem occured during converting format and writing it to byte array stream: \n\t")
					.append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorIOException(exceptionMsg.toString());
		}
	}

	byte[] getAvatar(String avatarUrl, ObjectClass objClass) {
		byte[] result = null;
		try {
			URIBuilder getUri = new URIBuilder(avatarUrl);
			HttpGet request = new HttpGet(getUri.build());
			request.addHeader("Authorization", "Basic " + authEncoding());
			request.addHeader("User-Agent",
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");

			CloseableHttpResponse response = execute(request);
			processResponseErrors(response);

			HttpEntity entity = response.getEntity();
			result = EntityUtils.toByteArray(entity);
			String imageType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(result));
			// LOGGER.info("\n\ttype: {0}", contentType);

			// SVG avatar:
			if (imageType.contains("xml")) {
				// LOGGER.info("\n\tSVG avatar");
				TranscoderInput inputSVGimage = new TranscoderInput(avatarUrl);
				OutputStream outputJPEGstream = new ByteArrayOutputStream();
				TranscoderOutput outputJPEGimage = new TranscoderOutput(outputJPEGstream);
				JPEGTranscoder SVGtoJPEGconverter = new JPEGTranscoder();
				SVGtoJPEGconverter.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(1));
				try {
					SVGtoJPEGconverter.transcode(inputSVGimage, outputJPEGimage);
					result = ((ByteArrayOutputStream) outputJPEGstream).toByteArray();
					// ByteArrayInputStream bis = new
					// ByteArrayInputStream(result);
					// BufferedImage img = ImageIO.read(bis);
					// ImageIO.write(img, "jpeg", new File("D:\\outJPEG.jpeg"));
					outputJPEGstream.flush();
					outputJPEGstream.close();
					closeResponse(response);
					return result;
				} catch (TranscoderException e) {
					StringBuilder exceptionMsg = new StringBuilder();
					exceptionMsg.append("Converting from SVG format to JPEG foramt failed").append("\n\t")
							.append(e.getLocalizedMessage());
					closeResponse(response);
					LOG.error(exceptionMsg.toString());
					throw new ConnectorException(exceptionMsg.toString());
				}
			} else {
				return result;
			}
		} catch (IOException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Getting avatar failed: problem occured during determining of image format:")
					.append("\n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorIOException(exceptionMsg.toString());
		} catch (URISyntaxException e1) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Getting avatar failed: Problem occured during bilding URI: ").append(avatarUrl)
					.append("\n\t").append(e1.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		}
		// return result;
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	String getUsernameFromUid(String uid) {
		URIBuilder getUri = getURIBuilder();
		if (uid != null) {
			try {
				getUri = getURIBuilder();
				getUri.setPath(URI_BASE_PATH + URI_USER_PATH);
				getUri.addParameter(UID, uid);
				HttpGet request = new HttpGet(getUri.build());
				JSONObject user = callRequest(request, true, CONTENT_TYPE_JSON);
				// LOGGER.info("username: {0}", user.getString(ATTR_NAME));
				return user.getString(ATTR_NAME);
			} catch (URISyntaxException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Get username form user ID failed: problem occurred during executing URI: ")
						.append(getUri).append(", using uid attribute: ").append(uid).append("\n\t")
						.append(e.getLocalizedMessage());
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			}
		}
		return null;
	}

	String getUserIdFromName(String username) {
		URIBuilder getUri = getURIBuilder();
		if (username != null) {
			try {
				getUri = getURIBuilder();
				getUri.setPath(URI_BASE_PATH + URI_USER_PATH);
				getUri.addParameter(PARAM_USERNAME, username);
				HttpGet request = new HttpGet(getUri.build());
				JSONObject user = callRequest(request, true, CONTENT_TYPE_JSON);
				LOG.info("username: {0}", user.getString(ATTR_KEY));
				return user.getString(ATTR_KEY);
			} catch (URISyntaxException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Get user ID form user username failed: problem occurred during executing URI: ")
						.append(getUri).append("\n\t").append(e.getLocalizedMessage());
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			}
		}
		return null;
	}

	String getProjectKeyFromUid(String uid) {
		URIBuilder getUri = getURIBuilder();
		if (uid != null) {
			try {
				getUri = getURIBuilder();
				getUri.setPath(URI_BASE_PATH + URI_PROJECT_PATH + "/" + uid);
				HttpGet request = new HttpGet(getUri.build());
				JSONObject project = callRequest(request, true, CONTENT_TYPE_JSON);
				LOG.info("project key: {0}", project.getString(ATTR_KEY));
				return project.getString(ATTR_KEY);
			} catch (URISyntaxException e) {
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("Get project key form uid failed: problem occurred during executing URI: ")
						.append(getUri).append("\n\t").append(e.getLocalizedMessage());
				LOG.error(exceptionMsg.toString());
				throw new ConnectorException(exceptionMsg.toString());
			}
		}
		return null;
	}

	<T> T addAttr(ConnectorObjectBuilder builder, String attrName, T attrVal) {
		if (attrVal != null) {
			builder.addAttribute(attrName, attrVal);
		}
		return attrVal;
	}

	String getStringAttr(Set<Attribute> attributes, String attrName) throws InvalidAttributeValueException {
		return getAttr(attributes, attrName, String.class);
	}

	GuardedString getGuardedStringAttr(Set<Attribute> attributes, String attrName)
			throws InvalidAttributeValueException {
		return getAttr(attributes, attrName, GuardedString.class);
	}

	<T> T getAttr(Set<Attribute> attributes, String attrName, Class<T> type)
			throws InvalidAttributeValueException {
		return getAttr(attributes, attrName, type, null);
	}

	byte[] getByteArrayAttr(Set<Attribute> attributes, String attrName)
			throws InvalidAttributeValueException {
		return getAttr(attributes, attrName, byte[].class);
	}

	@SuppressWarnings("unchecked")
	private <T> T getAttr(Set<Attribute> attributes, String attrName, Class<T> type, T defaultVal)
			throws InvalidAttributeValueException {
		for (Attribute attr : attributes) {
			if (attrName.equals(attr.getName())) {
				List<Object> vals = attr.getValue();
				if (vals == null || vals.isEmpty()) {
					// set empty value
					return null;
				}
				if (vals.size() == 1) {
					Object val = vals.get(0);
					if (val == null) {
						// set empty value
						return null;
					}
					if (type.isAssignableFrom(val.getClass())) {
						return (T) val;
					}
					StringBuilder exceptionMsg = new StringBuilder();
					exceptionMsg.append("Unsupported type ").append(val.getClass()).append(" for attribute ")
							.append(attrName).append(", value: ").append(val);
					LOG.error(exceptionMsg.toString());
					throw new InvalidAttributeValueException(exceptionMsg.toString());
				}
				StringBuilder exceptionMsg = new StringBuilder();
				exceptionMsg.append("More than one value for attribute ").append(attrName).append(", values: ")
						.append(vals);
				LOG.error(exceptionMsg.toString());
				throw new InvalidAttributeValueException(exceptionMsg.toString());
			}
		}
		// set default value when attrName not in changed attributes
		return defaultVal;
	}

	void getIfExists(JSONObject jsonObj, String attr, ConnectorObjectBuilder builder, boolean isMultiValue) {
		if (jsonObj.has(attr) && jsonObj.get(attr) != null && !JSONObject.NULL.equals(jsonObj.get(attr))) {
			if (isMultiValue) {
				JSONArray attrJSONArray = jsonObj.getJSONArray(attr);
				if (attrJSONArray != null) {
					int size = attrJSONArray.length();
					ArrayList<String> attrStringArray = new ArrayList<String>();
					for (int i = 0; i < size; i++) {
						attrStringArray.add(attrJSONArray.get(i).toString());
					}
					builder.addAttribute(attr, attrStringArray.toArray());
				}
			} else
				addAttr(builder, attr, jsonObj.get(attr));
		}
	}

	Uid getUidIfExists(JSONObject jsonObj, String attr, ConnectorObjectBuilder builder) {
		if (jsonObj.has(attr) && jsonObj.get(attr) != null && !JSONObject.NULL.equals(jsonObj.get(attr))) {
			Uid uid = new Uid(jsonObj.getString(attr));
			builder.setUid(uid);
			return uid;
		} else {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Primary identifier '").append(attr).append("' is missing.");
			LOG.error(exceptionMsg.toString());
			throw new ConfigurationException(exceptionMsg.toString());
		}
	}

	void getNameIfExists(JSONObject jsonObj, String attr, ConnectorObjectBuilder builder) {
		if (jsonObj.has(attr) && jsonObj.get(attr) != null && !JSONObject.NULL.equals(jsonObj.get(attr))) {
			builder.setName(new Name(jsonObj.getString(attr)));
		} else {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Secondary identifier '").append(attr).append("' is missing.");
			LOG.error(exceptionMsg.toString());
			throw new ConfigurationException(exceptionMsg.toString());
		}
	}

	// processing of multivalue attribute containing JSONArray attribute
	// (applicable for e.g. extended attributes of users/groups/projects)
	// get string array of attributes 'name' from multivalue attribute 'item':
	ArrayList<String> getMultiAttrItems(JSONObject object, String attrName, String subAttrName) {

		JSONObject itemJSONObject = null;
		JSONArray itemsJSONArray = (JSONArray) object.get(attrName);
		int size = itemsJSONArray.length();
		if (size > 0) {
			ArrayList<String> itemsArray = new ArrayList<String>();
			// String[] itemsArray = new String[size];
			// JSONArray itemsJSONArray = roles.keySet().toArray(new
			// String[roles.keySet().size()]);
			// LOG.info("\n\tItems Count: {0} of atribute {1}: ' {2} '",
			// size, attrName, itemsJSONArray);

			for (int i = 0; i < itemsJSONArray.length(); i++) {
				// LOG.ok("\n\tProcessing {0}/{1} {2}: {3}", i + 1,
				// itemsJSONArray.length(), attrName,
				// itemsJSONArray.getJSONObject(i).get(subAttrName));

				itemJSONObject = itemsJSONArray.getJSONObject(i);
				itemsArray.add(itemJSONObject.getString(subAttrName));
				// LOG.info("\n\tGroup {0}: {1}", i+1, itemsArray[i]);
			}
			return itemsArray;
		}

		return null;
	}

	void putFieldIfExists(Set<Attribute> attributes, String fieldName, JSONObject jo) {
		String fieldValue = getStringAttr(attributes, fieldName);
		if (fieldValue != null) {
			jo.put(fieldName, fieldValue);
		}
	}

	void testConnetion(String name) {
		HttpGet request;
		URIBuilder getUri = null;
		try {
			getUri = getURIBuilder();
			getUri.setPath(URI_BASE_PATH + URI_USER_PATH);
			getUri.addParameter(PARAM_USERNAME, name);
			request = new HttpGet(getUri.build());
			callRequest(request, true, CONTENT_TYPE_JSON);
		} catch (URISyntaxException e) {
			StringBuilder exceptionMsg = new StringBuilder();
			exceptionMsg.append("Test failed: problem occurred during executing URI: ").append(getUri).append(getUri)
					.append(", using name attribute: ").append(name).append("\n\t").append(e.getLocalizedMessage());
			LOG.error(exceptionMsg.toString());
			throw new ConnectorException(exceptionMsg.toString());
		}
	}
}
