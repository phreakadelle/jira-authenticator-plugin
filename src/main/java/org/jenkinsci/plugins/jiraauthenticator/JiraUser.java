package org.jenkinsci.plugins.jiraauthenticator;

import java.util.List;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;

import hudson.util.Secret;

/**
 * Jira User.
 * 
 * @author stephan.watermeyer
 *
 */
public class JiraUser implements UserDetails {

    private static final long serialVersionUID = 485415736315753530L;

    String user;
    Secret password;
    List<GrantedAuthority> grantedAuthorities;

    public JiraUser(String user, Secret password, List<GrantedAuthority> grantedAuthorities) {
        super();
        this.user = user;
        this.password = password;
        this.grantedAuthorities = grantedAuthorities;
    }

    public JiraUser(String user, List<GrantedAuthority> grantedAuthorities) {
        this(user, null, grantedAuthorities);
    }

    @Override
    public GrantedAuthority[] getAuthorities() {
        return grantedAuthorities.toArray(new GrantedAuthority[this.grantedAuthorities.size()]);
    }

    @Override
    public String getPassword() {
        return (password == null ? null : password.getPlainText());
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
