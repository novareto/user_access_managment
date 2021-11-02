package de.novareto.keycloak.forms;

import org.keycloak.Config;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.LoginFormsProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.FreeMarkerUtil;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class VirtualUserLoginFormsProviderFactory implements LoginFormsProviderFactory {

    public static String PROVIDER_ID = "virtualFreemarker";

    private FreeMarkerUtil freeMarker;

    @Override
    public LoginFormsProvider create(KeycloakSession session) {
        return new VirtualUserLoginFormsProvider(session, freeMarker);
    }

    @Override
    public void init(Config.Scope config) {
        freeMarker = new FreeMarkerUtil();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
        freeMarker = null;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
