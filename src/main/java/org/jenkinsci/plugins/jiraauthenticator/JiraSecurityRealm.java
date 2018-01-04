package org.jenkinsci.plugins.jiraauthenticator;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.AuthenticationException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.jenkinsci.plugins.jiraauthenticator.beans.Item;
import org.jenkinsci.plugins.jiraauthenticator.beans.JiraResponseGeneral;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.security.AbstractPasswordBasedSecurityRealm;
import hudson.security.GroupDetails;
import hudson.security.SecurityRealm;
import hudson.util.FormValidation;

public class JiraSecurityRealm extends AbstractPasswordBasedSecurityRealm {

    /** Used for logging purposes. */
    private static final Logger LOG = Logger.getLogger(JiraSecurityRealm.class.getName());

    private String url;

    @DataBoundConstructor
    public JiraSecurityRealm(String url) {
        this.url = url.trim();
    }

    @Override
    protected UserDetails authenticate(String pUsername, String pPassword) throws AuthenticationException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Authenticate user '" + pUsername + "' using password '" + (null != pPassword ? "<available>'" : "<not specified>'"));
        }

        // create the list of granted authorities
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        try {

            JiraAuthenticationService service = new JiraAuthenticationService(url);
            JiraResponseGeneral serviceResponse = service.authenticate(pUsername, pPassword);

            for (Item current : serviceResponse.getGroups().getItems()) {
                authorities.add(new GrantedAuthorityImpl(current.getName()));
            }

            // finally
            authorities.add(SecurityRealm.AUTHENTICATED_AUTHORITY);

            return new JiraUser(serviceResponse.getName(), pPassword, authorities);
        } catch (ClientHandlerException e) {
            if (e.getCause() != null && e.getCause() instanceof SocketTimeoutException) {
                throw new DataRetrievalFailureException("timeout: " + e.getMessage(), e);
            } else {
                LOG.log(Level.WARNING, "the answer from jira is unexpected: " + e.getMessage(), e);
                throw new DataRetrievalFailureException("format error: " + e.getMessage(), e);
            }
        } catch (UniformInterfaceException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            if (e.getMessage().contains("403")) {
                LOG.fine("Authenticate user '" + pUsername + "' failed with HTTP 403'");
                throw new UsernameNotFoundException(pUsername, e);
            }
            throw new DataRetrievalFailureException("response error" + e.getMessage(), e);
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            throw new DataRetrievalFailureException("general error" + e.getMessage(), e);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String pUsername) throws UsernameNotFoundException, DataAccessException {
        LOG.fine("loadUserByUsername '" + pUsername);
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        authorities.add(SecurityRealm.AUTHENTICATED_AUTHORITY);
        authorities.add(new GrantedAuthorityImpl("x"));
        return new JiraUser("abc", "123", authorities);
    }

    @Override
    public GroupDetails loadGroupByGroupname(String groupname) throws UsernameNotFoundException, DataAccessException {
        LOG.fine("loadGroupByGroupname '" + groupname);
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {
        /**
         * Default constructor.
         */
        public DescriptorImpl() {
            super(JiraSecurityRealm.class);
        }

        /**
         * Performs on-the-fly validation of the form field 'url'.
         * 
         * @param url
         *            The URL of the Crowd server.
         * 
         * @return Indicates the outcome of the validation. This is sent to the
         *         browser.
         */
        public FormValidation doCheckUrl(@QueryParameter final String url) {
            if (0 == url.length()) {
                return FormValidation.error("Bla");
            }

            return FormValidation.ok();
        }

        public FormValidation doTestConnection(@QueryParameter String url) {
            return FormValidation.ok();
        }

        /**
         * {@inheritDoc}
         * 
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return "JIRA Authenticator";
        }
    }

    public String getUrl() {
        return url;
    }

}
