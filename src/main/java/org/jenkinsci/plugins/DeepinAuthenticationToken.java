package org.jenkinsci.plugins;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.providers.AbstractAuthenticationToken;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deepin.DeepinOAuthApiService;
import org.jenkinsci.plugins.deepin.DeepinUser;
import org.scribe.model.Token;

public class DeepinAuthenticationToken extends AbstractAuthenticationToken {

	private static final long serialVersionUID = -4372773234796292520L;

	private Token accessToken;
    private DeepinUser bitbucketUser;

	@SuppressWarnings("deprecation")
	public DeepinAuthenticationToken(Token accessToken, String apiKey, String apiSecret) {
        this.accessToken = accessToken;
        this.bitbucketUser = new DeepinOAuthApiService(apiKey, apiSecret).getUserByToken(accessToken);

        boolean authenticated = false;

        if (bitbucketUser != null) {
            authenticated = true;
        }

        setAuthenticated(authenticated);
    }

    @Override
    public GrantedAuthority[] getAuthorities() {
        return this.bitbucketUser != null ? this.bitbucketUser.getAuthorities() : new GrantedAuthority[0];
    }

    /**
     * @return the accessToken
     */
    public Token getAccessToken() {
        return accessToken;
    }

    @Override
    public Object getCredentials() {
        return StringUtils.EMPTY;
    }

    @Override
    public Object getPrincipal() {
        return getName();
    }

    @Override
    public String getName() {
        return (bitbucketUser != null ? bitbucketUser.getUsername() : null);
    }

}
