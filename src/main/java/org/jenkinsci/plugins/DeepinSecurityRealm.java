package org.jenkinsci.plugins;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.security.GroupDetails;
import hudson.security.UserMayOrMayNotExistException;
import hudson.security.SecurityRealm;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UserDetailsService;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deepin.DeepinOAuthApiService;
import org.jenkinsci.plugins.deepin.DeepinOAuthApiService.DeepinToken;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Header;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.springframework.dao.DataAccessException;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class DeepinSecurityRealm extends SecurityRealm {

    private static final String REFERER_ATTRIBUTE = DeepinSecurityRealm.class.getName() + ".referer";
  //  private static final String ACCESS_TOKEN_ATTRIBUTE = DeepinSecurityRealm.class.getName() + ".access_token";
    private static final Logger LOGGER = Logger.getLogger(DeepinSecurityRealm.class.getName());

    private String deepinidServer;
    private String clientID;
    private String clientSecret;
    

    @DataBoundConstructor
    public DeepinSecurityRealm(String deepinidServer, String clientID, String clientSecret) {
        super();
        this.deepinidServer = Util.fixEmptyAndTrim(deepinidServer);
        this.clientID = Util.fixEmptyAndTrim(clientID);
        this.clientSecret = Util.fixEmptyAndTrim(clientSecret);
    }

    public DeepinSecurityRealm() {
        super();
        LOGGER.log(Level.FINE, "DeepinSecurityRealm()");
    }

    /**
     * @return the deepinidServer
     */
    public String getDeepinidServer() {
        return deepinidServer;
    }

    /**
     * @param deepinidServer the deepinidServer to set
     */
    public void setDeepinidServer(String deepinidServer) {
        this.deepinidServer = deepinidServer;
    }
    
    /**
     * @return the clientID
     */
    public String getClientID() {
        return clientID;
    }

    /**
     * @param clientID the clientID to set
     */
    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    /**
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @param clientSecret the clientSecret to set
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public HttpResponse doCommenceLogin(StaplerRequest request, @Header("Referer") final String referer) throws IOException {
        request.getSession().setAttribute(REFERER_ATTRIBUTE, referer);
        DeepinOAuthApiService DeepinOAuthApiService = new DeepinOAuthApiService(deepinidServer, clientID, clientSecret);
        return new HttpRedirect(DeepinOAuthApiService.createOAutuorizeURL());
    }

    public HttpResponse doFinishLogin(StaplerRequest request) throws IOException {
        String code = request.getParameter("code");
        
        if (StringUtils.isBlank(code)) {
            LOGGER.log(Level.SEVERE, "doFinishLogin() code = null");
            return HttpResponses.redirectToContextRoot();
        }

        DeepinToken token = new DeepinOAuthApiService(deepinidServer, clientID, clientSecret).getTokenByAuthorizationCode(code);

        if (!token.accessToken.isEmpty()) {
            DeepinAuthenticationToken auth = new DeepinAuthenticationToken(token, deepinidServer, clientID, clientSecret);
            SecurityContextHolder.getContext().setAuthentication(auth);
            User u = User.current();
            u.setFullName(auth.getName());
        } else {
            LOGGER.log(Level.SEVERE, "doFinishLogin() accessToken = null");
        }

        // redirect to referer
        String referer = (String) request.getSession().getAttribute(REFERER_ATTRIBUTE);
        if (referer != null) {
            return HttpResponses.redirectTo(referer);
        } else {
            return HttpResponses.redirectToContextRoot();
        }
    }

    @Override
    public void doLogout(StaplerRequest req, StaplerResponse rsp)    throws IOException, ServletException {
        String rootUrl = Hudson.getInstance().getRootUrl();
        if (StringUtils.endsWith(rootUrl, "/")) {
            rootUrl = StringUtils.left(rootUrl, StringUtils.length(rootUrl) - 1);
        }
        DeepinOAuthApiService DeepinOAuthApiService = new DeepinOAuthApiService(deepinidServer, clientID, clientSecret);
     	String logoutAPI = DeepinOAuthApiService.syncLogout(rootUrl);
    	rsp.sendRedirect2(logoutAPI);
    	super.doLogout(req, rsp);
    }
    
    @Override
    public SecurityComponents createSecurityComponents() {
        return new SecurityRealm.SecurityComponents(new AuthenticationManager() {
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                if (authentication instanceof DeepinAuthenticationToken) {
                    return authentication;
                }

                throw new BadCredentialsException("Unexpected authentication type: " + authentication);
            }
        }, new UserDetailsService() {
            public UserDetails loadUserByUsername(String username)  throws UserMayOrMayNotExistException, DataAccessException {
                throw new UserMayOrMayNotExistException("Cannot verify users in this context");
            }
        });
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserDetails result = null;
        DeepinAuthenticationToken authToken = (DeepinAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (authToken == null) {
            throw new UsernameNotFoundException("DeepinAuthenticationToken = null, no known user: " + username);
        }
        result = new DeepinOAuthApiService(deepinidServer, clientID, clientSecret).getUserByUsername(username);
        if (result == null) {
            throw new UsernameNotFoundException("User does not exist for login: " + username);
        }
        return result;
    }

    @Override
    public GroupDetails loadGroupByGroupname(String groupName) {
        throw new UsernameNotFoundException("groups not supported");
    }

    @Override
    public boolean allowsSignup() {
        return false;
    }

    @Override
    public String getLoginUrl() {
        return "securityRealm/commenceLogin";
    }

    public static final class ConverterImpl implements Converter {

        public boolean canConvert(Class type) {
            return type == DeepinSecurityRealm.class;
        }

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {

            DeepinSecurityRealm realm = (DeepinSecurityRealm) source;

            writer.startNode("deepinidServer");
            writer.setValue(realm.getDeepinidServer());
            writer.endNode();
            
            writer.startNode("clientID");
            writer.setValue(realm.getClientID());
            writer.endNode();

            writer.startNode("clientSecret");
            writer.setValue(realm.getClientSecret());
            writer.endNode();
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            String node = reader.getNodeName();

            reader.moveDown();

            DeepinSecurityRealm realm = new DeepinSecurityRealm();

            node = reader.getNodeName();

            String value = reader.getValue();

            setValue(realm, node, value);

            reader.moveUp();

            reader.moveDown();

            node = reader.getNodeName();

            value = reader.getValue();

            setValue(realm, node, value);

            reader.moveUp();

            if (reader.hasMoreChildren()) {
                reader.moveDown();

                node = reader.getNodeName();

                value = reader.getValue();

                setValue(realm, node, value);

                reader.moveUp();
            }
            return realm;
        }

        private void setValue(DeepinSecurityRealm realm, String node, String value) {

            if (node.equalsIgnoreCase("clientid")) {
                realm.setClientID(value);
            } else if (node.equalsIgnoreCase("clientsecret")) {
                realm.setClientSecret(value);
            } else if (node.equalsIgnoreCase("deepinidserver")) {
                realm.setDeepinidServer(value);
            } else {
                throw new ConversionException("invalid node value = " + node);
            }

        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {

        @Override
        public String getHelpFile() {
            return "/plugin/deepin-oauth/help/realm/help-security-realm.html";
        }

        @Override
        public String getDisplayName() {
            return "Deepin OAuth Plugin";
        }

        public DescriptorImpl() {
            super();
        }

        public DescriptorImpl(Class<? extends SecurityRealm> clazz) {
            super(clazz);
        }
    }

}
