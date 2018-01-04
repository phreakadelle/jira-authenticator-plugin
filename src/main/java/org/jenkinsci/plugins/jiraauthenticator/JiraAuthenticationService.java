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
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.jiraauthenticator.beans.JiraResponseGeneral;
import org.springframework.dao.DataRetrievalFailureException;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class JiraAuthenticationService {

    private static final int TIMEOUT_IN_MS = 10000;
    private static final Logger LOG = Logger.getLogger(JiraSecurityRealm.class.getName());
    private String mUrl;

    public JiraAuthenticationService(String url) {
        super();
        this.mUrl = url;
        init();
    }

    public JiraResponseGeneral authenticate(String pUsername, String pPassword) throws AuthenticationException {
        try {

            ClientConfig config = new DefaultClientConfig();
            Client client = Client.create(config);

            if (StringUtils.isNotEmpty(pUsername) && StringUtils.isNotEmpty(pPassword)) {
                LOG.fine("setting username to: " + pUsername);
                client.addFilter(new HTTPBasicAuthFilter(pUsername, pPassword));
            } else {
                throw new DataRetrievalFailureException("no username and password provided");
            }

            client.setReadTimeout(TIMEOUT_IN_MS);
            WebResource mInstance = client.resource(UriBuilder.fromUri(mUrl).build());
            mInstance = mInstance.path("rest/api/2/user/");

            final MultivaluedMap<String, String> requestParams = new MultivaluedHashMap<String, String>();
            requestParams.putSingle("username", pUsername);
            requestParams.putSingle("expand", "groups");

            final String serviceResponse = mInstance.queryParams(requestParams).accept(MediaType.APPLICATION_JSON).get(String.class);
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
                throw new DataRetrievalFailureException("timeout: " + e.getMessage(), e);
            } else {
                LOG.log(Level.WARNING, "the answer from jira is unexpected: " + e.getMessage(), e);
                throw new DataRetrievalFailureException("format error: " + e.getMessage(), e);
            }
        } catch (UniformInterfaceException e) {
            if (e.getMessage().contains("403")) {
                LOG.fine("Authenticate user '" + pUsername + "' failed with HTTP 403'");
                throw new UsernameNotFoundException(pUsername, e);
            }
            throw new DataRetrievalFailureException("response error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DataRetrievalFailureException("general error: " + e.getMessage(), e);
        }

    }

    private void init() {
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
