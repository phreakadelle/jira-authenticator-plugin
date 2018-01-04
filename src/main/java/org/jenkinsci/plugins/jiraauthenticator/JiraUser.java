package org.jenkinsci.plugins.jiraauthenticator;

import java.util.List;
import java.util.logging.Logger;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;

public class JiraUser implements UserDetails {

    private static final long serialVersionUID = 485415736315753530L;
    
    /** Used for logging purposes. */
    private static final Logger LOG = Logger.getLogger(JiraUser.class.getName());
    
    String user;
    String password;
    List<GrantedAuthority> grantedAuthorities;

    public JiraUser(String user, String password, List<GrantedAuthority> grantedAuthorities) {
        super();
        this.user = user;
        this.password = password;
        this.grantedAuthorities = grantedAuthorities;
    }

    @Override
    public GrantedAuthority[] getAuthorities() {
        GrantedAuthority[] retVal = grantedAuthorities.toArray(new GrantedAuthority[this.grantedAuthorities.size()]);
        LOG.info("getAuthorities: " + retVal.length);
        return retVal;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return user;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
