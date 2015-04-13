package org.jenkinsci.plugins.deepin;

import hudson.model.Hudson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.acegisecurity.userdetails.UserDetails;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deepin.DeepinUser.DeepinUserResponce;
import org.scribe.model.Request;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class DeepinOAuthApiService {
    private String clientID;
    private String clientSecret;
    private String oauthCallback;
    
	private String apiRoot;
    private String UID_ENDPOINT;
    private String USER_ENDPOINT;
    private String OAUTH2_API;
    private String LOGOUT_API;
   
    public DeepinOAuthApiService(String host, String apiKey, String apiSecret) {
        apiRoot = host;
        UID_ENDPOINT 	= apiRoot + "/users/uid/";
        USER_ENDPOINT	= apiRoot + "/users/username/";
        OAUTH2_API 		= apiRoot + "/oauth2/";
        LOGOUT_API 		= apiRoot + "/oauth2/logout";
        
    	clientID = apiKey;
        clientSecret = apiSecret;       
        String rootUrl = Hudson.getInstance().getRootUrl();
        if (StringUtils.endsWith(rootUrl, "/")) {
            rootUrl = StringUtils.left(rootUrl, StringUtils.length(rootUrl) - 1);
        }
        oauthCallback = rootUrl + "/securityRealm/finishLogin";
    }
    
    public String syncLogout(String callback) {
    	return LOGOUT_API + "?callback=" + callback;
    }
    
    public String createOAutuorizeURL() {
    	return String.format("%s%s?response_type=%s&client_id=%s&scope=%s&redirect_uri=%s", 
    			OAUTH2_API, "authorize", "code", clientID, "base", oauthCallback);
    }
    
    //"access_token":"ZmQxYmYzYjAtYWJlMS00NTVkLThkNTYtYmFjZjE5ODEwOTAz",
    //"expires_in":3000,
    //"refresh_token":"Njk2NzMwMTItODdkYS00OGUyLWIzNGQtNmZlMGJiNjFiMjQ4",
    //"scope":"base,",
    //"uid":0
    
	public class DeepinToken {
		
		public class DeepinTokenResponse {
			public DeepinToken data;
		}
		
	    @SerializedName("access_token")
	    public String accessToken;
	    
	    @SerializedName("expires_in")
	    public Integer expiresIn;
	    
	    @SerializedName("refresh_token")
	    public String refreshToken;
	    
	    @SerializedName("scope")
	    public String scope;
	    
	    @SerializedName("uid")
	    public Integer uid;
    }
		
    public DeepinToken getTokenByAuthorizationCode(String code) {
        Request request = new Request(Verb.POST, OAUTH2_API + "token");
        request.addBodyParameter("grant_type", "authorization_code");
        request.addBodyParameter("code", code);
        request.addBodyParameter("redirect_uri", oauthCallback);
        request.addBodyParameter("client_id", clientID);
        request.addBodyParameter("client_secret", clientSecret);
        Response response = request.send();
        String json = response.getBody();
        Gson gson = new Gson();
        DeepinToken tokenResponse = gson.fromJson(json, DeepinToken.class);
        return tokenResponse;        
    }

    public DeepinUser getUserByToken(DeepinToken token) {
        Request request = new Request(Verb.GET, UID_ENDPOINT + String.format("%d", token.uid));
        request.addHeader("Access-Token",  token.accessToken);
        Response response = request.send();
        String json = response.getBody();
        Gson gson = new Gson();
        DeepinUserResponce userResponse = gson.fromJson(json, DeepinUserResponce.class);

        if (userResponse != null) {
            return userResponse.data;
        } else {
            return null;
        }
    }

    public UserDetails getUserByUsername(String username) {
        InputStreamReader reader = null;
        DeepinUserResponce userResponce = null;
        try {
            URL url = new URL(USER_ENDPOINT + username);
            reader = new InputStreamReader(url.openStream(), "UTF-8");
            Gson gson = new Gson();
            userResponce = gson.fromJson(reader, DeepinUserResponce.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(reader);
        }

        if (userResponce != null) {
            return userResponce.data;
        } else {
            return null;
        }
    }

}
