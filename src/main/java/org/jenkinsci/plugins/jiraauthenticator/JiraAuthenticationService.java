package org.jenkinsci.plugins.jiraauthenticator;

import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationServiceException;
import org.acegisecurity.BadCredentialsException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.jiraauthenticator.beans.JiraResponseGeneral;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class JiraAuthenticationService {

    private static final Logger LOG = Logger.getLogger(JiraSecurityRealm.class.getName());
    private final String mUrl;
    private final String mTechnicalUserName;
    private final String mTechnicalUserPassword;
    private final Integer mTimeoutInMS;

    public JiraAuthenticationService(String url, String pTechnicalUserName, String pTechnicalUserPassword, Integer pTimeoutInMS) {
        super();
        this.mUrl = url;
        this.mTechnicalUserName = pTechnicalUserName;
        this.mTechnicalUserPassword = pTechnicalUserPassword;
        this.mTimeoutInMS = pTimeoutInMS;
        init();
    }

    public JiraResponseGeneral loadUserByUsername(final String pUsername) throws AuthenticationException {
        final MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<String, String>();
        requestParams.putSingle("username", pUsername);
        requestParams.putSingle("expand", "groups");
        return callService(mTechnicalUserName, mTechnicalUserPassword, requestParams);
    }

    public JiraResponseGeneral authenticate(final String pUsername, final String pPassword) throws AuthenticationException {
        final MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<String, String>();
        requestParams.putSingle("username", pUsername);
        
        // here we pass the given credentials. because we dont want to authorize the technical user
        return callService(pUsername, pPassword, requestParams);
    }

    JiraResponseGeneral callService(final String pUsername, final String pPassword, MultivaluedMap<String, String> pRequestParams) {
        try {
            ClientConfig config = new DefaultClientConfig();
            Client client = Client.create(config);

            if (StringUtils.isNotEmpty(pUsername) && StringUtils.isNotEmpty(pPassword)) {
                LOG.fine("setting username to: " + pUsername);
                client.addFilter(new HTTPBasicAuthFilter(pUsername, pPassword));
            } else {
                throw new AuthenticationServiceException("no username and password provided");
            }

            client.setReadTimeout(mTimeoutInMS);
            WebResource mInstance = client.resource(UriBuilder.fromUri(mUrl).build());
            mInstance = mInstance.path("rest/api/2/user/");

            final String serviceResponse = mInstance.queryParams(pRequestParams).accept(MediaType.APPLICATION_JSON).get(String.class);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(serviceResponse.toString());
            }

            JiraResponseGeneral parsedResponsed = new Gson().fromJson(serviceResponse, JiraResponseGeneral.class);

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(parsedResponsed.toString());
            }

            return parsedResponsed;
        } catch (ClientHandlerException e) {
            if (e.getCause() != null && e.getCause() instanceof SocketTimeoutException) {
                throw new AuthenticationServiceException("timeout: " + e.getMessage(), e);
            } else {
                LOG.log(Level.WARNING, "the answer from jira is unexpected: " + e.getMessage(), e);
                throw new AuthenticationServiceException("format error: " + e.getMessage(), e);
            }
        } catch (UniformInterfaceException e) {
            if (e.getMessage().contains("403")) {
                throw new BadCredentialsException("User does not exist (HTTP 403): " + pUsername, e);
            } else if (e.getMessage().contains("401")) {
                throw new BadCredentialsException("User is not allowed (HTTP 401): " + pUsername, e);
            } else {
                LOG.log(Level.WARNING, "response error: " + e.getMessage(), e);
                throw new AuthenticationServiceException("response error: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new AuthenticationServiceException("general error: " + e.getMessage(), e);
        }
    }

    void init() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            LOG.fine(e.getMessage());
        }
    }

}
