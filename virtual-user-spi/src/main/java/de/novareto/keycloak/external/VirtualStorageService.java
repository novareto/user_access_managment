package de.novareto.keycloak.external;

import de.novareto.keycloak.user.Constants;
import lombok.extern.jbosslog.JBossLog;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
@JBossLog
public class VirtualStorageService {

    private final VirtualStorageClient client;

    public VirtualStorageService(KeycloakSession session, ComponentModel model) {
        CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
        ResteasyClient resteasyClient = new ResteasyClientBuilder().httpEngine(engine).build();
        ResteasyWebTarget target = resteasyClient.target(model.get(Constants.BASE_URL));
        target.register(new BasicAuthentication(model.get(Constants.AUTH_USERNAME), model.get(Constants.AUTH_PASSWORD)));
        this.client = target.proxyBuilder(VirtualStorageClient.class).classloader(this.getClass().getClassLoader()).build();
    }

    public List<VirtualUser> getUsers(Integer firstResult, Integer maxResults) {
        return client.getUsers(firstResult, maxResults, null, null);
    }

    public Integer getUsersCount() {
        return client.getUsersCount().get("count");
    }

    public VirtualUser findUserById(String userId) {
        try {
            return client.getUserById(userId);
        } catch (ClientErrorException e) {
            log.warnf("User with userId %s could not be found, response from server: %s", userId, e.getResponse().getStatus());
            return null;
        }
    }

    public VirtualUser findUserByEmail(String email) {
        return client.getUsers(1, 1, email, null).get(0);
    }

    public List<VirtualUser> searchUsers(String search, Integer firstResult, Integer maxResults) {
        return client.getUsers(firstResult, maxResults, null, search);
    }

    public boolean verifyCredentials(VirtualUserCredential credential) {
        try {
            Response response = client.verifyCredentials(credential);
            return response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL);
        } catch (ClientErrorException e) {
            return false;
        }
    }

    public boolean updateCredentials(VirtualUserCredential credential) {
        try {
            Response response = client.updateCredentials(credential);
            return response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL);
        } catch (ClientErrorException e) {
            log.warnf("Credentials update for user %s failed with response %s", credential.getUserId(), e.getResponse().getStatus());
            return false;
        }
    }

}
