package org.jenkinsci.plugins;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.providers.AbstractAuthenticationToken;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deepin.DeepinOAuthApiService;
import org.jenkinsci.plugins.deepin.DeepinOAuthApiService.DeepinToken;
import org.jenkinsci.plugins.deepin.DeepinUser;

public class DeepinAuthenticationToken extends AbstractAuthenticationToken {

	private static final long serialVersionUID = -4372773234796292520L;

	private DeepinToken accessToken;
    private DeepinUser deepinUser;

	@SuppressWarnings("deprecation")
	public DeepinAuthenticationToken(DeepinToken accessToken, String deepinidServer, String apiKey, String apiSecret) {
        this.accessToken = accessToken;
        this.deepinUser = new DeepinOAuthApiService(deepinidServer, apiKey, apiSecret).getUserByToken(accessToken);

        boolean authenticated = false;

        if (deepinUser != null) {
            authenticated = true;
        }

        setAuthenticated(authenticated);
    }

    @Override
    public GrantedAuthority[] getAuthorities() {
        return this.deepinUser != null ? this.deepinUser.getAuthorities() : new GrantedAuthority[0];
    }

    /**
     * @return the accessToken
     */
    public DeepinToken getAccessToken() {
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
        return (deepinUser != null ? deepinUser.getUsername() : null);
    }

}
