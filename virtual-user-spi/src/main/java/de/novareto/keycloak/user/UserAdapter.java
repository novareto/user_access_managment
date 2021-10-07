package de.novareto.keycloak.user;

import de.novareto.keycloak.external.VirtualUser;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class UserAdapter extends AbstractUserAdapterFederatedStorage {

    private final VirtualUser user;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, VirtualUser user) {
        super(session, realm, model);
        this.storageId = new StorageId(storageProviderModel.getId(), user.getId());
        this.user = user;
        setFirstName(user.getFirstName());
        setLastName(user.getLastName());
        setEmail(user.getEmail());
        setEnabled(true);

        // TODO attributes
        // TODO groups
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public void setUsername(String username) {
        user.setEmail(username);
    }

}
