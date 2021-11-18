package de.novareto.keycloak.user;

import de.novareto.keycloak.external.VirtualUser;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class VirtualUserAdapter extends AbstractUserAdapter.Streams {

    public static final String REQUIRES_PASSWORD_UPDATE = "requiresPasswordUpdate";

    private final VirtualUser user;

    public VirtualUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, VirtualUser user) {
        super(session, realm, model);
        this.storageId = new StorageId(storageProviderModel.getId(), user.getId());
        this.user = user;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public String getFirstAttribute(String name) {
        switch (name) {
            case UserModel.USERNAME:
                return getUsername();
            case UserModel.EMAIL:
                return getEmail();
            case UserModel.FIRST_NAME:
                return getFirstName();
            case UserModel.LAST_NAME:
                return getLastName();
            default:
                return user.getAttributes().getFirst(name);
        }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
        attributes.add(UserModel.USERNAME, getUsername());
        attributes.add(UserModel.EMAIL, getEmail());
        attributes.add(UserModel.FIRST_NAME, getFirstName());
        attributes.add(UserModel.LAST_NAME, getLastName());
        if (user.getAttributes() != null) {
            attributes.addAll(user.getAttributes());
        }
        return attributes;
    }

    @Override
    public List<String> getAttribute(String name) {
        if (name.equals(UserModel.USERNAME)) {
            return Collections.singletonList(getUsername());
        }
        return user.getAttributes() != null ? user.getAttributes().get(name) : List.of();
    }

    @Override
    protected Set<GroupModel> getGroupsInternal() {
        if (user.getGroups() != null) {
            return user.getGroups().stream().map(VirtualGroupModel::new).collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        if (user.getAttributes() != null && user.getAttributes().containsKey(REQUIRES_PASSWORD_UPDATE)) {
            return Stream.of(RequiredAction.UPDATE_PASSWORD.name());
        }
        return Stream.empty();
    }

    @Override
    public void removeRequiredAction(String action) {
        if (RequiredAction.UPDATE_PASSWORD.name().equals(action)) {
            removePasswordUpdateRequirement();
        } else {
            super.removeRequiredAction(action);
        }
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        if (RequiredAction.UPDATE_PASSWORD.equals(action)) {
            removePasswordUpdateRequirement();
        } else {
            super.removeRequiredAction(action);
        }
    }

    private void removePasswordUpdateRequirement() {
        if (user.getAttributes() != null) {
            user.getAttributes().remove(REQUIRES_PASSWORD_UPDATE);
        }
    }
}
