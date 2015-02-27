package org.jenkinsci.plugins.deepin;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

public class DeepinOAuthApi extends DefaultApi10a {
    private static final String OAUTH_ENDPOINT = "http://api.linuxdeepin.com/oauth2/";

    @Override
    public String getAccessTokenEndpoint() {
        return OAUTH_ENDPOINT + "token";
    }

    @Override
    public String getAuthorizationUrl(Token oauthToken) {
    	//http://api.linuxdeepin.com/oauth2/authorize?
    	//client_id=eff9f27feaab7bd134c0b2f1cdf277b48fac7261
    	//&redirect_uri=http%253A%252F%252Fapi.linuxdeepin.com%252Fcrop%252Fqihoo%252Fbindredirect
    	//&response_type=code&scope=base
        return "http://api.linuxdeepin.com/oauth2/authorize?response_type=code";
    }

    @Override
    public String getRequestTokenEndpoint() {
        return OAUTH_ENDPOINT + "request_token";
    }
}
