package de.novareto.keycloak.user;

import de.novareto.keycloak.external.VirtualStorageService;
import de.novareto.keycloak.external.VirtualUser;
import de.novareto.keycloak.external.VirtualUserCredential;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Niko Köbler, https://www.n-k.de, @dasniko
 */
public class VirtualUserStorageProvider implements UserStorageProvider,
        UserLookupProvider.Streams, UserQueryProvider.Streams,
        CredentialInputValidator, CredentialInputUpdater,
        UserRegistrationProvider {

    private final KeycloakSession session;
    private final ComponentModel model;
    private final VirtualStorageService service;

    // map of loaded users in this transaction
    protected Map<String, UserModel> loadedUsers = new HashMap<>();

    public VirtualUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        this.service = new VirtualStorageService(session, model);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return false;
        if (!supportsCredentialType(input.getType())) return false;
        UserCredentialModel cred = (UserCredentialModel) input;
        return service.updateCredentials(new VirtualUserCredential(StorageId.externalId(user.getId()), cred.getChallengeResponse()));
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Set.of();
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) {
            return false;
        }
        UserCredentialModel cred = (UserCredentialModel) input;
        return service.verifyCredentials(new VirtualUserCredential(StorageId.externalId(user.getId()), cred.getChallengeResponse()));
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        UserModel adapter = loadedUsers.get(id);
        if (adapter == null) {
            adapter = new VirtualUserAdapter(session, realm, model, service.findUserById(StorageId.externalId(id)));
            loadedUsers.put(id, adapter);
        }
        return adapter;
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return getUserByEmail(realm, username);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        UserModel adapter = loadedUsers.get(email);
        if (adapter == null) {
            VirtualUser user = service.findUserByEmail(email);
            if (user != null) {
                adapter = new VirtualUserAdapter(session, realm, model, user);
                loadedUsers.put(email, adapter);
            }
        }
        return adapter;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return service.getUsersCount();
    }

    @Override
    public Stream<UserModel> getUsersStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return toUserModelStream(service.getUsers(firstResult, maxResults), realm);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        return toUserModelStream(service.searchUsers(search, firstResult, maxResults), realm);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        return toUserModelStream(service.getUsers(firstResult, maxResults), realm);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty();
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        // not used, but fixed return value to prevent internal errors
        return null;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        // not used, but fixed return value to prevent internal errors
        return false;
    }

    private Stream<UserModel> toUserModelStream(List<VirtualUser> virtualUsers, RealmModel realm) {
        return virtualUsers.stream().map(user -> new VirtualUserAdapter(session, realm, model, user));
    }
}
