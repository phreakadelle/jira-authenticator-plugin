package org.jenkinsci.plugins.jiraauthenticator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationServiceException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.jiraauthenticator.beans.Item;
import org.jenkinsci.plugins.jiraauthenticator.beans.JiraResponseGeneral;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.springframework.dao.DataAccessException;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.security.AbstractPasswordBasedSecurityRealm;
import hudson.security.GroupDetails;
import hudson.security.Permission;
import hudson.security.SecurityRealm;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * This class contains functionality to display the configuration page inside "Configure Security". Also it contains the
 * functionality to "authorize" a user and "loadUserByUsername" which retrieves the users groups.
 * 
 * @author stephan.watermeyer
 *
 */
public class JiraSecurityRealm extends AbstractPasswordBasedSecurityRealm {

    /** Used for logging purposes. */
    private static final Logger LOG = Logger.getLogger(JiraSecurityRealm.class.getName());

    private String url;
    private String credentialsId;
    private Integer timeout;
    private boolean insecureConnection;

    @DataBoundConstructor
    public JiraSecurityRealm(String url, String credentialsId, Integer timeout, boolean insecureConnection) {
        this.url = url;
        this.credentialsId = credentialsId;
        this.timeout = timeout;
        this.insecureConnection = insecureConnection;
    }

    @Override
    protected UserDetails authenticate(String pUsername, String pPassword) throws AuthenticationException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Authenticate user '" + pUsername + "' using password '" + (!StringUtils.isEmpty(pPassword) ? "<available>'" : "<not specified>'"));
        }

        try {
            final UsernamePasswordCredentialsImpl c = getCredentials(getCredentialsId());
            JiraAuthenticationService service = new JiraAuthenticationService(url, c.getUsername(), c.getPassword(), timeout, insecureConnection);
            JiraResponseGeneral serviceResponse = service.authenticate(pUsername, pPassword);

            final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
            authorities.add(SecurityRealm.AUTHENTICATED_AUTHORITY);
            return new JiraUser(serviceResponse.getName(), authorities);
        } catch (AuthenticationException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, e.getMessage(), e);
            }
            throw e;
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            throw new AuthenticationServiceException("general error: " + e.getMessage(), e);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String pUsername) throws UsernameNotFoundException, DataAccessException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("loadUserByUsername '" + pUsername + "'");
        }

        // FIXME: Why does Jenkins call it like this?
        if ("MANAGE_DOMAINS".equalsIgnoreCase(pUsername)) {
            throw new UsernameNotFoundException("not supported");
        }

        try {
            final UsernamePasswordCredentialsImpl c = getCredentials(getCredentialsId());
            JiraAuthenticationService service = new JiraAuthenticationService(url, c.getUsername(), c.getPassword(), timeout, insecureConnection);
            JiraResponseGeneral serviceResponse = service.loadUserByUsername(pUsername);

            final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
            authorities.add(SecurityRealm.AUTHENTICATED_AUTHORITY);

            for (Item current : serviceResponse.getGroups().getItems()) {
                authorities.add(new GrantedAuthorityImpl(current.getName()));
            }

            return new JiraUser(serviceResponse.getName(), authorities);
        } catch (AuthenticationException e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, e.getMessage(), e);
            }
            throw e;
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            throw new AuthenticationServiceException("general error: " + e.getMessage(), e);
        }
    }

    @Override
    public GroupDetails loadGroupByGroupname(String groupname) throws UsernameNotFoundException, DataAccessException {
        LOG.fine("loadGroupByGroupname '" + groupname);
        return null;
    }

    /**
     * This is the dialogue that is displayed in the "Configure Security" page.
     * 
     * @author stephan.watermeyer
     *
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {

        /**
         * Default constructor.
         */
        public DescriptorImpl() {
            super(JiraSecurityRealm.class);
        }

        public FormValidation doCheckUrl(@QueryParameter final String url) {
            if (StringUtils.isEmpty(url)) {
                return FormValidation.error("The URL of Jira must not be null.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckUser(@QueryParameter final String user) {
            if (StringUtils.isEmpty(user)) {
                return FormValidation.error("The User of Jira must not be null.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter final String password) {
            if (StringUtils.isEmpty(password)) {
                return FormValidation.error("The Password of Jira must not be null.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckTimeout(@QueryParameter final String timeout) {
            if (StringUtils.isEmpty(timeout)) {
                return FormValidation.error("The Timeout of Jira must not be null.");
            }

            return FormValidation.ok();
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item instance) {
            Jenkins.getInstance().checkPermission(jenkins.model.Jenkins.ADMINISTER);
            return new StandardListBoxModel().includeEmptyValue().includeMatchingAs(ACL.SYSTEM, (hudson.model.Item) instance, StandardUsernamePasswordCredentials.class,
                    Collections.<DomainRequirement> emptyList(), CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
        }

        public FormValidation doTestConnection(@QueryParameter String url, @QueryParameter final String credentialsId, @QueryParameter final Integer timeout,
                @QueryParameter final boolean insecureConnection) {
            Jenkins.getInstance().checkPermission(jenkins.model.Jenkins.ADMINISTER);

            final UsernamePasswordCredentialsImpl c = getCredentials(credentialsId);
            JiraAuthenticationService service = new JiraAuthenticationService(url, c.getUsername(), c.getPassword(), timeout, insecureConnection);
            try {
                service.authenticate(c.getUsername(), c.getPassword().getPlainText());
                return FormValidation.ok("Connection successful");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "validating technical user for jira auth failed", e);
                return FormValidation.error("Failed to Authenticate your user: " + e.getMessage());
            }
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

    public static UsernamePasswordCredentialsImpl getCredentials(final String pCredentialId) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(UsernamePasswordCredentialsImpl.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement> emptyList()),
                CredentialsMatchers.allOf(CredentialsMatchers.withId(pCredentialId)));
    }

    public String getUrl() {
        return url;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public Integer getTimeout() {
        return timeout;
    }
    
    public boolean isInsecureConnection() {
        return insecureConnection;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }
   
    @DataBoundSetter
    public void setInsecureConnection(boolean insecureConnections) {
        this.insecureConnection = insecureConnections;
    }
    
    

}
