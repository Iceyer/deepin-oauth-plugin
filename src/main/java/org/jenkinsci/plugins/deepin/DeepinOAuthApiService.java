package org.jenkinsci.plugins.deepin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.acegisecurity.userdetails.UserDetails;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deepin.DeepinUser.DeepinUserResponce;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.google.gson.Gson;

public class DeepinOAuthApiService {

    private static final String API_ENDPOINT = "https://api.linuxdeepin.com/user/";

    private OAuthService service;

    public DeepinOAuthApiService(String apiKey, String apiSecret) {
        this(apiKey, apiSecret, null);
    }

    public DeepinOAuthApiService(String apiKey, String apiSecret, String callback) {
        super();
        ServiceBuilder builder = new ServiceBuilder().provider(DeepinOAuthApi.class).apiKey(apiKey).apiSecret(apiSecret);
        if (StringUtils.isNotBlank(callback)) {
            builder.callback(callback);
        }
        service = builder.build();
    }

    public Token createRquestToken() {
        return service.getRequestToken();
    }

    public String createAuthorizationCodeURL(Token requestToken) {
        return service.getAuthorizationUrl(requestToken);
    }

    public Token getTokenByAuthorizationCode(String code, Token requestToken) {
        Verifier v = new Verifier(code);
        return service.getAccessToken(requestToken, v);
    }

    public DeepinUser getUserByToken(Token accessToken) {
        OAuthRequest request = new OAuthRequest(Verb.GET, API_ENDPOINT + "user");
        service.signRequest(accessToken, request);
        Response response = request.send();
        String json = response.getBody();
        Gson gson = new Gson();
        DeepinUserResponce userResponce = gson.fromJson(json, DeepinUserResponce.class);
        if (userResponce != null) {
            return userResponce.user;
        } else {
            return null;
        }
    }

    public UserDetails getUserByUsername(String username) {
        InputStreamReader reader = null;
        DeepinUserResponce userResponce = null;
        try {
            URL url = new URL(API_ENDPOINT + "users/" + username);
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
            return userResponce.user;
        } else {
            return null;
        }
    }

}
